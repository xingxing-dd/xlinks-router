# Provider Token 连续失败熔断规则

本文说明 `xlinks-router-api` 中 provider token 连续失败熔断的默认行为、配置项以及实际生效语义。

## 默认规则

- 单个 `providerToken` 连续失败达到 `5` 次后，进入临时拉黑状态
- 临时拉黑默认持续 `5` 分钟
- 只要中间出现一次成功转发，就会立即清空该 token 的连续失败计数
- 拉黑窗口结束后，如果再次失败，会从第 `1` 次重新累计

## 配置项

配置位于 [application.yml](/D:/project/xlinks-router/xlinks-router-api/src/main/resources/application.yml)：

```yaml
xlinks:
  router:
    forward:
      failure:
        token-consecutive-failure-threshold: 5
        token-block-duration-ms: 300000
        token-failure-state-ttl-ms: 1800000
```

## 字段说明

- `token-consecutive-failure-threshold`
  - token 连续失败多少次后进入临时拉黑
- `token-block-duration-ms`
  - token 被拉黑的持续时长，单位毫秒
- `token-failure-state-ttl-ms`
  - token 失败状态在 Redis 中的保留时长，单位毫秒
  - 建议不小于 `token-block-duration-ms`
  - 系统会自动兜底，保证最终 TTL 不会小于拉黑时长

## 实现说明

- 连续失败状态保存在 Redis 中
- 只有真正的转发成功才会清除失败状态
- 单次抖动不会直接拉黑 token，必须达到阈值才会熔断
- 当前逻辑优先隔离 `providerToken`，不会因为一个 token 的短暂异常直接把整个 provider 永久绕开
