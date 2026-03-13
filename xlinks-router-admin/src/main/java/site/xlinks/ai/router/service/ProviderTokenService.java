package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.mapper.ProviderTokenMapper;

/**
 * Provider Token Service
 */
@Service
@RequiredArgsConstructor
public class ProviderTokenService extends ServiceImpl<ProviderTokenMapper, ProviderToken> {

    public IPage<ProviderToken> pageQuery(Integer page, Integer pageSize, Long providerId, Integer tokenStatus) {
        LambdaQueryWrapper<ProviderToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(providerId != null, ProviderToken::getProviderId, providerId)
               .eq(tokenStatus != null, ProviderToken::getTokenStatus, tokenStatus)
               .orderByDesc(ProviderToken::getCreatedAt);
        
        return this.page(new Page<>(page, pageSize), wrapper);
    }

    public ProviderToken getById(Long id) {
        ProviderToken token = super.getById(id);
        if (token == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Provider Token 不存在");
        }
        return token;
    }

    public boolean save(ProviderToken token) {
        return super.save(token);
    }

    public boolean update(ProviderToken token) {
        getById(token.getId());
        return super.updateById(token);
    }

    public boolean updateStatus(Long id, Integer status) {
        getById(id);
        ProviderToken token = new ProviderToken();
        token.setId(id);
        token.setTokenStatus(status);
        return super.updateById(token);
    }
}
