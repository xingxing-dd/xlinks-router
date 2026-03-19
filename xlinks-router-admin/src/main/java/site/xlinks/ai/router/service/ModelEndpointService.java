package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.entity.ModelEndpoint;
import site.xlinks.ai.router.mapper.ModelEndpointMapper;

/**
 * Model Endpoint Service
 */
@Service
@RequiredArgsConstructor
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
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Model Endpoint 不存在");
        }
        return modelEndpoint;
    }

    public boolean save(ModelEndpoint modelEndpoint) {
        return super.save(modelEndpoint);
    }

    public boolean update(ModelEndpoint modelEndpoint) {
        getById(modelEndpoint.getId());
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
}
