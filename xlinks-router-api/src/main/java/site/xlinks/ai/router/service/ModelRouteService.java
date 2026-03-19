package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.context.RouteTarget;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.Provider;

import java.util.List;

/**
 * 模型路由服务
 * 根据模型编码，路由到目标 Provider 和模型
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelRouteService {

    private final site.xlinks.ai.router.mapper.ModelMapper modelMapper;
    private final site.xlinks.ai.router.mapper.ProviderMapper providerMapper;

    /**
     * 根据模型编码获取路由目标
     *
     * @param modelCode 模型编码
     * @return 路由目标
     */
    public RouteTarget route(String modelCode) {
        log.debug("Routing for model: {}", modelCode);

        // 1. 查询模型
        LambdaQueryWrapper<Model> modelWrapper = new LambdaQueryWrapper<>();
        modelWrapper.eq(Model::getModelCode, modelCode)
                    .eq(Model::getStatus, 1);
        Model model = modelMapper.selectOne(modelWrapper);
        
        if (model == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模型不存在: " + modelCode);
        }

        // 2. 查询 Provider 信息
        Provider provider = providerMapper.selectById(model.getProviderId());
        if (provider == null || provider.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ROUTE_ERROR, "Provider 不可用");
        }

        // 3. 构建路由目标
        return RouteTarget.builder()
                .providerId(provider.getId())
                .providerCode(provider.getProviderCode())
                .providerType(provider.getProviderType())
                .baseUrl(provider.getBaseUrl())
                .providerModel(model.getModelName())
                .modelId(model.getId())
                .build();
    }
}
