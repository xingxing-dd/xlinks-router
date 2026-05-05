# xlinks-router-api-distributed 建设大纲

## 项目定位

`xlinks-router-api-distributed` 是对 `xlinks-router-api` 的分布式化重构项目，目标是基于 `Java 17 + Spring Boot` 构建一套可横向扩容的大模型 API 转发平台。

该项目的核心职责不是训练模型，而是作为统一网关层，对外提供标准化的大模型调用接口，对内完成不同上游服务商之间的协议兼容、请求转发、路由决策和稳定性控制。

## 核心目标

1. 统一协议接入  
   对外兼容 OpenAI、Anthropic 等主流大模型 API 协议，并保留后续扩展能力。

2. 协议适配与转发  
   支持请求转换、响应映射、流式输出转发、错误码兼容、超时控制。

3. 智能路由  
   根据模型、商户、账户、套餐、协议类型、provider 优先级、provider token 状态等维度动态选择最优上游链路。

4. 稳定性保障  
   内置限流、并发控制、失败重试、超时处理、降级、熔断和故障 provider 临时摘除机制。

5. 分布式高可用  
   支持多节点部署，通过共享状态与分布式协调机制提升整体吞吐、可用性和稳定性。

## 基础设施

- `MySQL`：存储模型、供应商、token、商户、套餐、调用记录等核心业务数据
- `Redis`：存储缓存、分布式锁、并发许可、临时状态、节点协同数据

## 核心能力分层

- 协议兼容层：统一入口、协议识别、请求与响应适配
- 路由决策层：模型匹配、商户路由、provider 与 token 选择
- 稳定性控制层：限流、并发许可、超时、重试、降级与熔断
- 缓存与状态层：路由缓存、token 缓存、provider 状态与失效控制
- 观测与审计层：请求日志、链路追踪、错误记录、用量统计
- 分布式支撑层：节点协同、共享状态、缓存刷新广播、分布式控制能力

## 当前代码分层约定

为避免协议处理、业务决策、基础设施能力相互耦合，`xlinks-router-api-distributed` 当前按以下结构组织：

- `boot`：启动入口
- `protocol/controller`：OpenAI、Anthropic 等协议入口控制器
- `protocol/interceptor`：协议级请求拦截，例如客户 token 识别
- `protocol/service`：协议层轻量处理，只做关键字段解析和协议上下文提取
- `protocol/model`：协议层最小模型、协议错误响应、上下文常量
- `protocol/handler`：协议原生异常响应处理
- `infrastructure/config`：Spring MVC 和基础设施装配
- `infrastructure/cache`：基于 Redis 的分布式缓存，承接原单节点内存缓存迁移
- `infrastructure/http`：后续上游 provider 网络转发能力，负责真实 HTTP 调用、流式转发、超时和上游响应接收
- `app/forwarding`：后续转发流程编排，负责把协议解析、客户确认、路由决策、HTTP 转发、后处理串起来
- `support/web`：通用 Web 支撑能力，例如 traceId 过滤器
- `support/handler`：非协议接口使用的通用异常处理
- `support/controller`：健康检查等非协议支撑接口

当前分层原则：

- 协议层只负责协议兼容，不直接承担业务路由决策
- customer token 识别属于协议入口能力，是后续业务路由的前置条件
- 路由、provider 选择、支付模式、额度与结算应在后续应用层/领域层继续拆分，不继续堆在控制器中
- 网络转发能力固定放在 `infrastructure/http`，不能放回 `protocol/controller` 或领域决策层
- 原 `xlinks-router-api` 中的 JVM 内存缓存不再沿用，分布式场景统一迁移到 Redis，key 前缀规范为 `api:cache:*`
- MySQL、Redis、HTTP 调用等外部交互能力后续统一放入基础设施层，不反向污染协议层
- routing 领域层只负责“选谁”，不负责“怎么发”；HTTP 转发编排必须留给 forwarding + infrastructure/http

## 当前核心决策链

当前分布式 API 模块已经形成一条相对明确的决策主链，核心目标是先完成“识别客户、准备读模型、做路由决策”，再进入后续 HTTP 转发。

当前主链执行顺序如下：

`protocol/controller -> app/forwarding/ForwardingApplicationService -> app/forwarding/ForwardingReadModelLoader -> domain/routing/DefaultRoutingDomainService -> domain/provider/DefaultProviderTokenSelectionService`

各层职责如下：

- `protocol/controller`：接收 OpenAI、Anthropic 协议请求，不做业务决策，只负责调用 forwarding 编排入口
- `ForwardingApplicationService`：编排入口，负责把 `customerToken`、账户、套餐、模型准备好，再触发 routing
- `ForwardingReadModelLoader`：负责 Redis 读缓存；缓存未命中时回源 MySQL，并把结果写回 Redis
- `DefaultRoutingDomainService`：负责真正的“路由决策”，包括模型权限校验、候选 providerModel 选择、商户优选 provider 重排、provider 故障过滤
- `DefaultProviderTokenSelectionService`：负责 providerToken 过滤与选择，包括故障态过滤、状态过滤、过期过滤、配额过滤和优先级排序

当前已经落地的决策步骤如下：

1. 从请求头中解析 `customer token`
2. 根据 `customer token` 找到 `CustomerToken`
3. 根据 `CustomerToken.accountId` 找到客户账户
4. 查找当前可用的 `CustomerPlan`
5. 根据请求体关键字段解析出的 `model` 找到标准模型
6. 校验 `customerToken.allowedModels` 与 `plan.allowedModels`
7. 根据 `modelId + protocol` 取候选 `ProviderModel`
8. 根据商户路由配置确认是否存在优选 `provider`
9. 过滤临时故障 `provider`
10. 为每个候选 `provider` 选择可用 `providerToken`
11. 生成最终 `RoutingDecision`

当前阶段的边界也已经明确：

- routing 只负责决定“调用哪个 provider、哪个 providerModel、哪个 providerToken”
- forwarding 只负责流程编排，不在控制器里分散业务决策
- Redis 缓存仓储只负责存取，不负责数据库回源逻辑
- 真实上游 HTTP 请求仍待下一阶段在 `infrastructure/http` 中接入

## 实施计划

### 第一阶段：项目骨架与基础设施搭建

目标是完成项目最小可运行骨架，统一基础技术栈与工程规范。

- 基础框架采用 `Spring Boot 3`、`Maven`、`MySQL`、`Redis`
- 公共工具优先使用 Apache 开源工具类，避免重复造轮子
- 建立统一的配置结构、环境配置加载方式和模块目录规范
- 完成日志规范化输出、日志归档配置、全局异常处理、统一返回结构
- 预留 Redis、数据库、HTTP 客户端、线程池、链路追踪等基础配置入口

### 第二阶段：定义转发 API 协议

目标是先把对外 API 定义清楚，再进入具体转发实现。

- 主要参考 `docs/ai-model/` 中的协议文档
- 当前优先实现补全、聊天、模型列表三类转发能力
- `docs/ai-model/` 中暂未完整覆盖 Anthropic 协议，这部分参考现有 `xlinks-router-api` 的接口定义与实现方式
- 请求和响应报文在转发链路中优先使用原始 `String` 传递，避免因自定义对象字段不完整导致转发报文缺失
- 对请求体的对象化解析只用于提取关键字段，例如 `model`、`stream`、协议类型、必要透传头，不作为最终转发报文来源
- 第四阶段实现真实转发时，调用方 `requestBody` 原样下发给底层 provider，provider 的 `responseBody` 原样回传给调用方
- 明确定义统一异常码、错误响应模型和流式响应转发边界
- 这一阶段重点是“定义清楚协议边界”，而不是立即做复杂路由逻辑

### 第三阶段：实现智能路由与稳定性策略

目标是完成请求从客户入口到上游 provider 选择的核心决策逻辑。

- 请求头中的 `customer token` 是整个链路的第一输入，必须先通过 `Authorization Bearer` 或 `x-api-key` 完成客户识别
- 核心链路为：`customerToken + model -> 商户 -> 支付模式 -> provider -> providerModel -> providerToken`
- 完成模型匹配、商户映射、支付模式确认、provider 路由、provider token 选择
- 引入限流、降级、失败重试、故障 provider 临时摘除等稳定性策略
- 并发控制粒度以 `provider + token` 为核心，保障上游 token 资源可控使用
- 结合 Redis 实现分布式状态共享，避免多节点下限流和故障状态失真
- 在实现真实 routing 之前，forwarding 编排层应先完成基础读模型准备：`customerToken`、客户账户、可用套餐、模型信息

### 当前缓存迁移结论

现有 `xlinks-router-api` 的内存缓存可分为两类：

- 路由读模型缓存：模型、provider、providerModel、providerToken、customerToken、套餐模型权限、商户路由、协议路由索引
- 运行态状态缓存：provider 故障状态、provider token 故障状态

`xlinks-router-api-distributed` 中这两类缓存均不再使用 JVM 本地 `volatile Map` 持有，而是统一迁移到 Redis：

- Redis key 统一前缀：`api:cache:*`
- 路由读模型缓存放 `infrastructure/cache`
- 运行态故障状态也放 `infrastructure/cache`
- 后续 forwarding 和 routing 只依赖缓存仓储接口，不直接依赖 Redis 命令细节

### 第四阶段：实现网络请求与协议转发

目标是完成与上游模型服务的真实网络交互能力。

- 构建统一的 HTTP 调用与协议适配层，目录归属 `infrastructure/http`
- 转发链路编排放在 `app/forwarding`
- 支持同步响应与流式响应转发
- 处理超时、连接异常、上游错误码映射、重试与中断
- 完成不同协议之间的请求转换和响应转换，形成完整转发闭环

### 第五阶段：转发后处理与业务落账

目标是补齐请求结束后的业务处理链路。

- 完成调用记录、错误记录、用量记录等数据落库
- 完成余额、额度、套餐扣减或结算阶段处理逻辑
- 补齐缓存刷新、状态回写、异步审计等后置流程
- 为后续报表、运营分析、审计追踪提供可用数据基础

## 当前实施原则

- 先定义边界，再实现逻辑，避免协议和领域模型频繁返工
- 先骨架、后路由、再转发、最后后处理，保持阶段目标单一
- 单节点实现可以复用，但分布式状态必须明确抽象边界
- 每一阶段结束后都应形成可验证结果，而不是只停留在代码草稿状态

## 关键结论归档

以下结论已明确，后续设计与实现默认遵循，不再反复摇摆：

1. 协议报文处理原则  
   转发链路中的 `requestBody` 和后续 `responseBody` 以原始 `String` 为主，不使用自定义完整协议对象承载请求细节。对象化解析只用于提取关键字段，例如 `model`、`stream`、协议类型和必要透传头。

2. 客户识别入口  
   请求头中的 `customer token` 是整个链路的第一输入，必须优先处理。  
   OpenAI 兼容协议使用 `Authorization: Bearer <token>`。  
   Anthropic 协议优先使用 `x-api-key`，必要时再兼容 `Authorization`。

3. 协议层边界  
   `/v1/**` 协议接口不能返回通用 `Result` 包装，必须返回协议原生成功结构和协议原生错误结构，否则会破坏兼容性。

4. 分层约束  
   协议层只负责协议兼容和关键字段提取，不直接承担商户识别、provider 路由、余额扣减、数据库持久化等业务决策。  
   网络转发能力固定放在 `infrastructure/http`。  
   转发流程编排固定放在 `app/forwarding`。

5. 缓存演进方向  
   分布式 API 模块不再沿用 JVM 本地内存缓存。现有 `xlinks-router-api` 的 `volatile Map` / `ConcurrentMap` 缓存模型需要迁移到 Redis。

6. Redis key 规范  
   Redis 缓存统一使用前缀：`api:cache:*`。  
   所有路由读模型缓存、customer token 缓存、provider 故障状态缓存、provider token 故障状态缓存均按该规范组织。

7. 缓存分类结论  
   当前缓存明确分成两类：  
   路由读模型缓存：模型、provider、providerModel、providerToken、customerToken、套餐权限、商户路由、协议路由索引。  
   运行态状态缓存：provider 故障状态、provider token 故障状态。

8. forwarding 前置要求  
   在真正实现 forwarding 编排前，必须先完成 Redis 缓存仓储抽象，保证 forwarding 和 routing 依赖的是统一缓存接口，而不是直接依赖本地内存结构或散乱的 Redis 操作。

9. routing 读模型装载边界  
   `DistributedRouteCacheRepository` 只负责 Redis 存取，不直接承担缓存未命中时的数据库回源。  
   缓存未命中后的 MySQL 装载与 Redis 回填统一由 `app/forwarding/ForwardingReadModelLoader` 负责。  
   `domain/routing` 只消费已准备好的读模型或通过 loader 获取读模型，不直接编排数据库查询。

10. 当前 routing 已落地能力  
    当前 forwarding 主链已完成：`协议请求解析 -> customer token -> customerAccount -> customerPlan -> model -> allowedModels 校验 -> merchant preferred provider -> provider/providerToken 选择`。  
    当前阶段已经接入 `infrastructure/http` 的最小可用 forwarding 骨架：路由完成后会进入统一 HTTP 执行器，由协议适配器完成真实上游调用。  
    当前实现仍以“原始字符串请求体透传 + 必要字段改写 + 原始响应/原始 SSE 转发”为主，后续再逐步细化协议适配与异常映射。

11. provider 故障态过滤原则  
    provider 与 providerToken 的运行态故障信息继续放在 Redis 中。  
    routing 在 provider 维度先过滤临时失败节点，providerToken 选择阶段再过滤 token 维度的失败状态与配额耗尽状态。  
    这样可以保证“路由决策”和“运行态稳定性状态”解耦，但仍保持单次决策链路清晰。

12. 第四阶段当前落地边界  
    `app/forwarding` 只负责准备上下文并调用 `ProviderHttpForwardingExecutor`。  
    `infrastructure/http` 负责：
    `ProviderHttpForwardingExecutor` 统一选择协议适配器；  
    `OpenAiProviderHttpAdapter` 与 `AnthropicProviderHttpAdapter` 负责构建上游请求；  
    `AbstractOkHttpProviderHttpAdapter` 负责公共 OkHttp 执行、超时控制、请求体最小改写、同步响应透传、SSE 字节流透传。  
    当前版本优先保证 forwarding 闭环跑通，不在这一阶段过早引入复杂响应重写逻辑。

## ���䣺����ת������

����ת���߼����������Ѿ����䵽�ֿ⼶�ĵ���

- [api-provider-token-routing-flow.md](D:/project/xlinks-router/docs/api-provider-token-routing-flow.md)

���ĵ������ˡ�����ת���������ơ��½ڣ���ȷ����������·��

- �ͻ� token ����������Ϸ���У��
- �û� token ��Ч��У��
- ���ѷ�ʽ���ߣ��ײ�ģʽ / ���ģʽ
- ������·�ɾ��������ȼ�ѡ��
- ������ token ��ѯ�벢�����ƻ�ȡ
- ʵ������ת����ʧ�����ԡ��������л�
- ���������ͷ���ɹ�����첽����

���� `xlinks-router-api-distributed` �� routing��forwarding��retry��post-processing ��ƣ�ӦĬ����������������ΪͳһԼ����
