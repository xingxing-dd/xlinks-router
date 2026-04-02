package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.dto.MerchantUpdateDTO;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.mapper.CustomerAccountMapper;
import site.xlinks.ai.router.vo.MerchantVO;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final CustomerAccountMapper customerAccountMapper;

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
        return toVO(getEntity(id));
    }

    public MerchantVO update(Long id, MerchantUpdateDTO dto) {
        CustomerAccount existing = getEntity(id);
        CustomerAccount account = new CustomerAccount();
        account.setId(existing.getId());
        if (dto.getRemark() != null) {
            account.setRemark(dto.getRemark());
        }
        customerAccountMapper.updateById(account);
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
