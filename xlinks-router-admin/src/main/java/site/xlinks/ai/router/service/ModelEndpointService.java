package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.ModelEndpoint;
import site.xlinks.ai.router.mapper.ModelEndpointMapper;

/**
 * Model endpoint service.
 */
@Service
public class ModelEndpointService extends ServiceImpl<ModelEndpointMapper, ModelEndpoint> {

    public IPage<ModelEndpoint> pageQuery(Integer page, Integer pageSize, String endpointName, Integer status) {
        LambdaQueryWrapper<ModelEndpoint> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(endpointName), ModelEndpoint::getEndpointName, endpointName)
                .eq(status != null, ModelEndpoint::getStatus, status)
                .orderByDesc(ModelEndpoint::getCreatedAt);
        return this.page(new Page<>(page, pageSize), wrapper);
    }

    public ModelEndpoint getById(Long id) {
        ModelEndpoint modelEndpoint = super.getById(id);
        if (modelEndpoint == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Model endpoint not found");
        }
        return modelEndpoint;
    }

    public boolean save(ModelEndpoint modelEndpoint) {
        validateUnique(modelEndpoint.getEndpointCode(), modelEndpoint.getEndpointUrl(), null);
        return super.save(modelEndpoint);
    }

    public boolean update(ModelEndpoint modelEndpoint) {
        ModelEndpoint existing = getById(modelEndpoint.getId());
        String endpointCode = StringUtils.hasText(modelEndpoint.getEndpointCode())
                ? modelEndpoint.getEndpointCode()
                : existing.getEndpointCode();
        String endpointUrl = StringUtils.hasText(modelEndpoint.getEndpointUrl())
                ? modelEndpoint.getEndpointUrl()
                : existing.getEndpointUrl();
        validateUnique(endpointCode, endpointUrl, modelEndpoint.getId());
        return super.updateById(modelEndpoint);
    }

    public boolean updateStatus(Long id, Integer status) {
        getById(id);
        ModelEndpoint modelEndpoint = new ModelEndpoint();
        modelEndpoint.setId(id);
        modelEndpoint.setStatus(status);
        return super.updateById(modelEndpoint);
    }

    public boolean deleteById(Long id) {
        getById(id);
        return super.removeById(id);
    }

    private void validateUnique(String endpointCode, String endpointUrl, Long excludeId) {
        if (StringUtils.hasText(endpointCode)) {
            LambdaQueryWrapper<ModelEndpoint> codeWrapper = new LambdaQueryWrapper<ModelEndpoint>()
                    .eq(ModelEndpoint::getEndpointCode, endpointCode);
            if (excludeId != null) {
                codeWrapper.ne(ModelEndpoint::getId, excludeId);
            }
            if (this.count(codeWrapper) > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "Endpoint code already exists");
            }
        }

        if (StringUtils.hasText(endpointUrl)) {
            LambdaQueryWrapper<ModelEndpoint> urlWrapper = new LambdaQueryWrapper<ModelEndpoint>()
                    .eq(ModelEndpoint::getEndpointUrl, endpointUrl);
            if (excludeId != null) {
                urlWrapper.ne(ModelEndpoint::getId, excludeId);
            }
            if (this.count(urlWrapper) > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "Endpoint URL already exists");
            }
        }
    }
}
