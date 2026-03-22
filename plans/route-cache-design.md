# 路由数据内存缓存设计方案

## 1. 需求背景

当前 `ChatService.chatCompletions()` 每次请求都会查询数据库：

- `model_endpoints` 表（根据 `endpoint_code` 查询）
- `models` 表（根据 `model_code` + `endpoint_id` 查询）
- `providers` 表（根据 `provider_id` 查询）
- `provider_tokens` 表（根据 `provider_id` 选择可用 token）

高频查询导致性能瓶颈，且增加数据库负载。

## 2. 缓存范围

| 实体 | 缓存键 | 缓存内容 | 备注 |
|------|--------|----------|------|
| ModelEndpoint | `endpoint_code` | 完整 ModelEndpoint 对象 | 支持 enabled 状态过滤 |
| Model | `model_code` + `endpoint_id` | 完整 Model 对象（含 provider_id） | |
| Provider | `provider_id` | 完整 Provider 对象 | 仅缓存 status=1 的 |
| ProviderToken | `provider_id` | 可用的 Token 对象列表 | 需要支持选举逻辑 |

## 3. 缓存结构设计

```java
// 1. Endpoint 缓存
private final Map<String, ModelEndpoint> endpointCache = new ConcurrentHashMap<>();

// 2. Model 缓存: endpointId -> (modelCode -> Model)
private final Map<Long, Map<String, Model>> modelCache = new ConcurrentHashMap<>();

// 3. Provider 缓存
private final Map<Long, Provider> providerCache = new ConcurrentHashMap<>();

// 4. ProviderToken 缓存（选举后的当前可用 token）
private final Map<Long, ProviderToken> providerTokenCache = new ConcurrentHashMap<>();
```

## 4. 刷新机制

### 4.1 启动时加载

```java
@PostConstruct
public void init() {
    refreshAll();
}
```

### 4.2 定时自动刷新

```java
@Scheduled(fixedRate = 5 * 60 * 1000) // 5 分钟
public void scheduledRefresh() {
    refreshAll();
}
```

## 5. 与现有代码的集成

### 5.1 新增 CacheService

```java
@Service
public class RouteCacheService {

    @Autowired private ModelEndpointMapper modelEndpointMapper;
    @Autowired private ModelMapper modelMapper;
    @Autowired private ProviderMapper providerMapper;
    @Autowired private ProviderTokenMapper providerTokenMapper;

    // 缓存...
    
    public void refreshAll() {
        refreshProviders();
        refreshEndpoints();
        refreshModels();
        refreshTokens();
    }
    
    // ... 各刷新方法
}
```

### 5.2 修改 ChatService

```java
@Autowired private RouteCacheService cacheService;

public ChatCompletionResponse chatCompletions(...) {
    // 替换 DB 查询为缓存查询
    ModelEndpoint endpoint = cacheService.getEndpoint(endpointCode);
    Model model = cacheService.getModel(endpoint.getId(), request.getModel());
    Provider provider = cacheService.getProvider(model.getProviderId());
    ProviderToken token = cacheService.getProviderToken(provider.getId());
    // ...
}
```

## 6. 数据一致性考虑

1. **初始加载**：启动时一次性加载所有数据到内存
2. **手动刷新**：管理员可通过 API 触发全量/增量刷新
3. **失效策略**：
   - 缓存 key 不存在时回源 DB 查询并回填缓存
   - 刷新时直接覆盖旧数据
4. **异常降级**：缓存查询异常时回退到 DB 查询

## 7. 实现决定

- **刷新方式**：定时自动刷新（@Scheduled），默认 5 分钟间隔
- **选举逻辑**：保持现有 ProviderTokenSelectService 实现
- **容量限制**：暂不需要，按全量加载处理

## 8. 实现步骤

1. 新增 `RouteCacheService` 负责缓存加载与刷新
2. 使用 `@PostConstruct` 启动时加载
3. 使用 `@Scheduled` 定时刷新
4. 修改 `ChatService` 调用缓存而非直接查库
