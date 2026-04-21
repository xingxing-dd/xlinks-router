package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.dto.MerchantProviderRouteConfigDTO;
import site.xlinks.ai.router.dto.MerchantUpdateDTO;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.MerchantProviderRoute;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.mapper.CustomerAccountMapper;
import site.xlinks.ai.router.mapper.MerchantProviderRouteMapper;
import site.xlinks.ai.router.mapper.ModelMapper;
import site.xlinks.ai.router.mapper.ProviderMapper;
import site.xlinks.ai.router.vo.MerchantProviderRouteVO;
import site.xlinks.ai.router.vo.MerchantVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final CustomerAccountMapper customerAccountMapper;
    private final MerchantProviderRouteMapper merchantProviderRouteMapper;
    private final ModelMapper modelMapper;
    private final ProviderMapper providerMapper;
    private final ApiCacheRefreshNotifier apiCacheRefreshNotifier;

    public IPage<MerchantVO> pageQuery(Integer page, Integer pageSize, String keyword, Integer status) {
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        LambdaQueryWrapper<CustomerAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, CustomerAccount::getStatus, status)
                .and(StringUtils.hasText(trimmedKeyword), query -> query.like(CustomerAccount::getUsername, trimmedKeyword)
                        .or()
                        .like(CustomerAccount::getPhone, trimmedKeyword)
                        .or()
                        .like(CustomerAccount::getEmail, trimmedKeyword))
                .orderByDesc(CustomerAccount::getCreatedAt);

        Page<CustomerAccount> entityPage = customerAccountMapper.selectPage(new Page<>(page, pageSize), wrapper);
        Page<MerchantVO> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(entityPage.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    public MerchantVO getDetail(Long id) {
        CustomerAccount account = getEntity(id);
        MerchantVO vo = toVO(account);
        vo.setProviderRoutes(loadProviderRoutes(account.getId()));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public MerchantVO update(Long id, MerchantUpdateDTO dto) {
        CustomerAccount existing = getEntity(id);
        CustomerAccount account = new CustomerAccount();
        account.setId(existing.getId());
        if (dto.getRemark() != null) {
            account.setRemark(dto.getRemark());
        }
        customerAccountMapper.updateById(account);

        if (dto.getProviderRoutes() != null) {
            replaceProviderRoutes(existing.getId(), dto.getProviderRoutes());
            apiCacheRefreshNotifier.notifyAdminCacheChanged("merchantRoute", "updated", existing.getId());
        }
        return getDetail(id);
    }

    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Merchant status only supports 0 or 1");
        }
        getEntity(id);
        CustomerAccount account = new CustomerAccount();
        account.setId(id);
        account.setStatus(status);
        customerAccountMapper.updateById(account);
    }

    private void replaceProviderRoutes(Long accountId, List<MerchantProviderRouteConfigDTO> routeConfigs) {
        List<MerchantProviderRouteConfigDTO> normalized = routeConfigs == null
                ? Collections.emptyList()
                : routeConfigs.stream().filter(item -> item != null).toList();

        validateRouteConfigs(normalized);
        merchantProviderRouteMapper.delete(new LambdaQueryWrapper<MerchantProviderRoute>()
                .eq(MerchantProviderRoute::getAccountId, accountId));

        for (MerchantProviderRouteConfigDTO config : normalized) {
            MerchantProviderRoute route = new MerchantProviderRoute();
            route.setAccountId(accountId);
            route.setModelId(config.getModelId());
            route.setProviderId(config.getProviderId());
            route.setCreateBy(String.valueOf(accountId));
            route.setUpdateBy(String.valueOf(accountId));
            merchantProviderRouteMapper.insert(route);
        }
    }

    private void validateRouteConfigs(List<MerchantProviderRouteConfigDTO> routeConfigs) {
        if (routeConfigs == null || routeConfigs.isEmpty()) {
            return;
        }

        Map<Long, MerchantProviderRouteConfigDTO> deduplicated = new LinkedHashMap<>();
        for (MerchantProviderRouteConfigDTO config : routeConfigs) {
            if (config.getModelId() == null || config.getProviderId() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "Model and provider cannot be null");
            }
            if (deduplicated.putIfAbsent(config.getModelId(), config) != null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "Duplicate model route configuration is not allowed");
            }
        }

        Set<Long> modelIds = deduplicated.values().stream()
                .map(MerchantProviderRouteConfigDTO::getModelId)
                .collect(Collectors.toSet());
        Set<Long> providerIds = deduplicated.values().stream()
                .map(MerchantProviderRouteConfigDTO::getProviderId)
                .collect(Collectors.toSet());

        Map<Long, Model> models = modelMapper.selectBatchIds(modelIds).stream()
                .collect(Collectors.toMap(Model::getId, item -> item));
        Map<Long, Provider> providers = providerMapper.selectBatchIds(providerIds).stream()
                .collect(Collectors.toMap(Provider::getId, item -> item));

        for (MerchantProviderRouteConfigDTO config : deduplicated.values()) {
            if (!models.containsKey(config.getModelId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "Model not found: " + config.getModelId());
            }
            if (!providers.containsKey(config.getProviderId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "Provider not found: " + config.getProviderId());
            }
        }
    }

    private List<MerchantProviderRouteVO> loadProviderRoutes(Long accountId) {
        if (accountId == null) {
            return Collections.emptyList();
        }
        List<MerchantProviderRoute> routes = merchantProviderRouteMapper.selectList(new LambdaQueryWrapper<MerchantProviderRoute>()
                .eq(MerchantProviderRoute::getAccountId, accountId)
                .orderByAsc(MerchantProviderRoute::getModelId)
                .orderByAsc(MerchantProviderRoute::getId));
        if (routes == null || routes.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> modelIds = routes.stream().map(MerchantProviderRoute::getModelId).collect(Collectors.toSet());
        Set<Long> providerIds = routes.stream().map(MerchantProviderRoute::getProviderId).collect(Collectors.toSet());

        Map<Long, Model> modelMap = modelIds.isEmpty()
                ? Collections.emptyMap()
                : modelMapper.selectBatchIds(modelIds).stream().collect(Collectors.toMap(Model::getId, item -> item));
        Map<Long, Provider> providerMap = providerIds.isEmpty()
                ? Collections.emptyMap()
                : providerMapper.selectBatchIds(providerIds).stream().collect(Collectors.toMap(Provider::getId, item -> item));

        List<MerchantProviderRouteVO> result = new ArrayList<>();
        for (MerchantProviderRoute route : routes) {
            MerchantProviderRouteVO vo = new MerchantProviderRouteVO();
            vo.setId(route.getId());
            vo.setModelId(route.getModelId());
            vo.setProviderId(route.getProviderId());
            Model model = modelMap.get(route.getModelId());
            if (model != null) {
                vo.setModelCode(model.getModelCode());
                vo.setModelName(model.getModelName());
            }
            Provider provider = providerMap.get(route.getProviderId());
            if (provider != null) {
                vo.setProviderCode(provider.getProviderCode());
                vo.setProviderName(provider.getProviderName());
            }
            result.add(vo);
        }
        return result;
    }

    private CustomerAccount getEntity(Long id) {
        CustomerAccount account = customerAccountMapper.selectById(id);
        if (account == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Merchant not found");
        }
        return account;
    }

    private MerchantVO toVO(CustomerAccount account) {
        MerchantVO vo = new MerchantVO();
        BeanUtils.copyProperties(account, vo);
        return vo;
    }
}
