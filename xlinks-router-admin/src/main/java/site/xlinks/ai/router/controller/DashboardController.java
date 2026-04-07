package site.xlinks.ai.router.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.mapper.CustomerAccountMapper;
import site.xlinks.ai.router.service.CustomerTokenService;
import site.xlinks.ai.router.service.ModelService;
import site.xlinks.ai.router.service.ProviderModelService;
import site.xlinks.ai.router.service.ProviderService;
import site.xlinks.ai.router.service.ProviderTokenService;
import site.xlinks.ai.router.vo.DashboardOverviewVO;

import java.time.LocalDateTime;

/**
 * Admin dashboard API.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Admin dashboard APIs")
public class DashboardController {

    private final CustomerAccountMapper customerAccountMapper;
    private final ProviderService providerService;
    private final ModelService modelService;
    private final ProviderModelService providerModelService;
    private final ProviderTokenService providerTokenService;
    private final CustomerTokenService customerTokenService;

    @GetMapping("/overview")
    @Operation(summary = "Dashboard overview")
    public Result<DashboardOverviewVO> overview() {
        DashboardOverviewVO overview = new DashboardOverviewVO();
        overview.setMerchantCount(customerAccountMapper.selectCount(new LambdaQueryWrapper<>()));
        overview.setActiveMerchantCount(customerAccountMapper.selectCount(
                new LambdaQueryWrapper<CustomerAccount>().eq(CustomerAccount::getStatus, 1)
        ));
        overview.setProviderCount(providerService.count());
        overview.setActiveProviderCount(providerService.count(
                new LambdaQueryWrapper<Provider>().eq(Provider::getStatus, 1)
        ));
        overview.setModelCount(modelService.count());
        overview.setProviderModelCount(providerModelService.count());
        overview.setProviderTokenCount(providerTokenService.count());
        overview.setCustomerTokenCount(customerTokenService.count());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusDays(7);
        long expiringProviderTokenCount = providerTokenService.count(
                new LambdaQueryWrapper<ProviderToken>()
                        .isNotNull(ProviderToken::getExpireTime)
                        .between(ProviderToken::getExpireTime, now, deadline)
        );
        long expiringCustomerTokenCount = customerTokenService.count(
                new LambdaQueryWrapper<CustomerToken>()
                        .isNotNull(CustomerToken::getExpireTime)
                        .between(CustomerToken::getExpireTime, now, deadline)
        );
        overview.setExpiringTokenCount(expiringProviderTokenCount + expiringCustomerTokenCount);
        return Result.success(overview);
    }
}
