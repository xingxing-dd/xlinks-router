# Provider Token 并发限流与超时治理方案

## 1. 背景

当前 `xlinks-router-api` 的上游请求链路已经具备：

- 标准模型解析
- provider / provider_model 路由
- provider token 选择
- 上游直连与流式转发
- usage 记录与 provider 故障标记

但还缺少以下关键能力：

- `providerToken` 维度的分布式并发限流
- 会话开始占用、结束释放的统一资源管理
- 流式与非流式的首包/空闲超时治理
- 进程异常、网络中断、客户端断开时的 permit 自动回收

这会导致：

- 同一个上游 token 被瞬时打爆
- 多实例部署下并发控制不一致
- 上游长时间无响应时请求悬挂过久
- 流式请求在首包无响应、流中断流时缺少统一失败策略

## 2. 目标

本次方案目标：

1. 限流配置放在 `provider` 上。
2. 实际限流维度是 `providerToken`。
3. 每个会话开始时占用一个并发名额，结束或异常时释放。
4. 多实例部署下并发控制必须一致。
5. 非流式请求 20 秒内未完成则直接失败。
6. 流式请求：
   - 20 秒内未收到首个有效事件则失败
   - 流过程中任意 20 秒没有新事件则失败
7. 超时、异常、客户端断开时必须释放 permit，避免资源泄漏。
8. 失败原因应可观测、可落 usage 记录、可支持后台查询。

## 3. 为什么不用纯令牌桶做主方案

需求语义是：

- 会话开始消耗一个令牌
- 会话结束恢复一个令牌

这更接近“并发占坑”，不是典型的速率限制。

典型令牌桶适合：

- 每秒允许多少次请求
- 每分钟允许多少次请求

而本需求更适合：

- 每个 `providerToken` 同时允许多少个活跃会话

因此主方案采用：

- 分布式 expirable semaphore 作为并发控制

后续如果要增加真正的 QPS 限制，可以再叠加：

- `provider` 维度 QPS
- `providerToken` 维度 QPS
- 单账户 / 单 customer token 维度 QPS

## 4. 技术选型

### 4.1 主方案

推荐引入 Redisson，使用：

- `RPermitExpirableSemaphore`

原因：

- 基于 Redis，天然支持多实例共享状态
- 支持 permit lease time，进程崩溃后可自动回收
- 每个 permit 有独立 permitId，便于精确释放
- 适合“会话开始 acquire，结束 release”的语义

### 4.2 为什么优先选 Redisson 而不是 Bucket4j

Bucket4j 本身是成熟方案，适合做分布式令牌桶和速率限制。

但当前核心需求是：

- 活跃会话并发数限制
- 异常自动回收
- 会话级 permit 生命周期管理

这类场景用 Redisson 的 expirable semaphore 更直接。

结论：

- 并发控制主用 Redisson
- 如果后续要做 QPS 限流，再叠加 `RRateLimiter` 或 Bucket4j

## 5. 配置模型

限流配置放在 `providers` 表，建议新增字段：

- `concurrency_limit_enabled` `tinyint` 默认 `0`
- `max_concurrent_per_token` `int` 默认 `0`
- `acquire_timeout_ms` `int` 默认 `0`
- `request_timeout_ms` `int` 默认 `20000`
- `first_response_timeout_ms` `int` 默认 `20000`
- `stream_idle_timeout_ms` `int` 默认 `20000`
- `session_lease_ms` `int` 默认 `30000`
- `session_renew_interval_ms` `int` 默认 `10000`

语义说明：

- `concurrency_limit_enabled`
  - 是否启用 provider 级并发控制
- `max_concurrent_per_token`
  - provider 下每个 token 的最大并发会话数
- `acquire_timeout_ms`
  - 申请 permit 时的等待上限，建议第一版先用 `0`，不等待，立即尝试下一个 token / provider
- `request_timeout_ms`
  - 非流式请求完整响应超时阈值
- `first_response_timeout_ms`
  - 首包超时阈值
- `stream_idle_timeout_ms`
  - 流式空闲超时阈值
- `session_lease_ms`
  - permit 初始租约
- `session_renew_interval_ms`
  - permit 续租周期

## 6. 上下文与记录字段

建议补充以下字段：

### 6.1 ProviderInvokeContext

新增：

- `providerTokenId`
- `providerTokenName`
- `providerPermitId`

用途：

- 内部逻辑统一用 `providerTokenId` 做限流与审计
- `providerPermitId` 用于精确释放 permit

### 6.2 usage_records

建议新增：

- `provider_token_id`
- `finish_reason`

`finish_reason` 建议枚举：

- `success`
- `provider_error`
- `upstream_timeout`
- `stream_first_event_timeout`
- `stream_idle_timeout`
- `client_abort`
- `internal_error`
- `rate_limited`

说明：

- `response_status` 继续保留，方便后台成功/失败筛选
- `finish_reason` 用于更细粒度分析

## 7. Redis Key 设计

每个 `providerToken` 一个 semaphore key：

```text
xlinks:router:provider:{providerId}:token:{providerTokenId}:permits
```

可选的心跳或调试 key：

```text
xlinks:router:session:{requestId}
```

第一版不是必须写独立 session key，只要 permit lease 与续租健全即可。

## 8. 核心流程设计

### 8.1 路由与 acquire

现有逻辑是：

1. 找 candidate provider
2. 选 token
3. 直接调用上游

新逻辑改为：

1. 找 candidate provider
2. 遍历 candidate provider 下的可用 token
3. 对每个 token 尝试 acquire permit
4. acquire 成功才算路由成功
5. 当前 token 满了就尝试下一个 token
6. 当前 provider 所有 token 都拿不到 permit，则尝试下一个 provider
7. 全部 provider 都失败，返回“无可用 provider token / 并发已满”

注意：

- “permit 已满”不应记为 provider 故障
- “上游真实超时 / 连接失败 / 5xx”才进入 provider failure 统计

### 8.2 会话生命周期

会话开始：

- acquire permit
- 记录 `providerTokenId`
- 启动 permit 续租任务

会话成功结束：

- 停止续租
- release permit
- 落成功 usage record

会话失败结束：

- 停止续租
- cancel 上游调用
- release permit
- 落失败 usage record

进程异常退出：

- permit lease 到期后自动回收

## 9. 超时治理

### 9.1 非流式请求

策略：

- `callTimeout = 20s`
- `readTimeout = 20s`

判定：

- 20 秒内未拿到完整响应，视为失败

行为：

- 中断上游调用
- 释放 permit
- 记录 `finish_reason = upstream_timeout`

### 9.2 流式请求

流式需要两个超时：

#### 首包超时

判定：

- 建立上游连接后，20 秒内未收到首个有效事件

有效事件定义：

- 产生业务数据的 SSE event
- 不建议把单纯 heartbeat comment 当作成功首包

行为：

- cancel 上游调用
- 向客户端返回错误事件
- 释放 permit
- 记录 `finish_reason = stream_first_event_timeout`

#### 空闲超时

判定：

- 流开始后，任意连续 20 秒未收到新事件

行为：

- cancel 上游调用
- 向客户端发送错误事件
- 释放 permit
- 记录 `finish_reason = stream_idle_timeout`

### 9.3 客户端断开

若下游客户端主动断开：

- 立即 cancel upstream call
- 释放 permit
- 记录 `finish_reason = client_abort`

客户端断开不应计入 provider failure。

## 10. 错误码与响应语义

建议新增内部错误语义：

- `PROVIDER_TOKEN_RATE_LIMITED`
- `UPSTREAM_TIMEOUT`
- `STREAM_FIRST_EVENT_TIMEOUT`
- `STREAM_IDLE_TIMEOUT`
- `CLIENT_ABORT`

对外响应建议：

- permit 满 / 并发已满：`429`
- 上游超时：`504`
- provider 返回 4xx/5xx：按当前错误模型映射
- 内部异常：`500`

后台统计仍可保持：

- `response_status = 200` 为成功
- `response_status != 200` 为失败

## 11. 组件拆分建议

建议新增以下组件：

### 11.1 ProviderConcurrencyGuard

职责：

- 按 `providerTokenId` 获取 semaphore
- acquire / renew / release permit
- 返回 `ProviderPermitLease`

### 11.2 ProviderPermitLease

字段建议：

- `providerId`
- `providerTokenId`
- `providerTokenValue`
- `permitId`
- `leaseMs`

方法建议：

- `startRenew()`
- `releaseQuietly()`
- `markReleased()`

### 11.3 ProxyTimeoutPolicy

职责：

- 从 provider 配置解析 timeout / lease 参数
- 统一提供默认值

### 11.4 StreamTimeoutWatcher

职责：

- 跟踪首包超时
- 跟踪流空闲超时
- 超时后触发 upstream cancel

## 12. 对现有代码的主要改造点

### 12.1 ProviderTokenSelectService

当前仅负责挑选 token。

需要升级为：

- 遍历 token
- 尝试 acquire permit
- 返回“已占用”的 token 与 permit 信息

### 12.2 ProviderRouteResolver

当前逻辑：

- providerModel -> provider -> token

新逻辑：

- providerModel -> provider -> token + permit

### 12.3 ProtocolProxyService

这里是整个生命周期最适合做兜底的位置。

需要保证：

- 成功、业务异常、运行时异常、超时、下游断开
- 所有路径都能进入统一 release

建议使用：

- `try/finally` 做 permit 释放
- `usageRecordService` 记录 finish reason

### 12.4 Adapter 层

需要支持：

- 对单次请求传入专属 timeout policy
- 在超时时主动 cancel `Call`
- 流式读取时刷新“最近事件时间”

### 12.5 Controller 层

SSE controller 要补：

- emitter completion / timeout / error 事件与 upstream cancel 的联动

## 13. 实施计划

### Phase 1：文档与基础设施

1. 补设计文档
2. 引入 Redisson
3. 新增配置类
4. 新增 provider 配置字段

### Phase 2：并发限流主链路

1. 给 `ProviderInvokeContext` 增加 `providerTokenId / permitId`
2. 新增 `ProviderConcurrencyGuard`
3. 改造 `ProviderTokenSelectService`
4. 改造 `ProviderRouteResolver`
5. 改造 `ProtocolProxyService`，统一 acquire / release

### Phase 3：超时治理

1. 非流式 20s 超时
2. 流式首包 20s 超时
3. 流式空闲 20s 超时
4. 客户端断开联动 cancel upstream

### Phase 4：记录与观测

1. usage_records 增加 `provider_token_id`
2. usage_records 增加 `finish_reason`
3. 增加关键日志与指标

### Phase 5：后台配置与展示

1. admin provider 页面支持配置限流参数
2. usage 记录页面支持 `finish_reason` 与 `providerTokenId`

## 14. 第一版实施范围

为了尽快上线，第一版建议先做：

- Redisson 接入
- `providerToken` 分布式并发 permit
- `ProviderInvokeContext` 补 `providerTokenId`
- 非流式 20s 超时
- 流式首包 20s + 空闲 20s
- `usage_records` 增加 `provider_token_id`
- `usage_records` 增加 `finish_reason`

第一版暂不做：

- provider 页面完整配置 UI
- 复杂的 QPS 令牌桶
- 过于细分的多级限流策略

## 15. 风险与注意事项

### 15.1 不能只靠本地内存

如果只在 JVM 内存里做 semaphore：

- 多实例下并发会失真
- 实例崩溃后会话状态丢失

因此必须走 Redis。

### 15.2 permit 必须有 lease

否则进程异常退出会导致永久泄漏。

### 15.3 permit 续租不能忘

长流会话如果只有初始 lease，没有续租，会被 Redis 自动回收，导致并发统计失真。

### 15.4 客户端断开必须 cancel 上游

否则只是释放下游连接，但上游仍在跑，会浪费 token 并可能继续计费。

### 15.5 provider failure 与 rate limit 要分开

并发已满不是 provider 不健康，不应参与 provider 熔断计数。

## 16. 验收标准

满足以下条件视为通过：

1. 同一 `providerToken` 超过最大并发时，请求不会继续打到上游。
2. 多实例部署下，并发限制一致。
3. 非流式请求 20 秒超时会失败并释放 permit。
4. 流式请求首包超时、空闲超时都会失败并释放 permit。
5. 客户端断开后 permit 会释放。
6. 进程异常退出后 permit 能在 lease 过期后自动回收。
7. usage 记录可区分成功、超时、客户端断开、上游异常等原因。
