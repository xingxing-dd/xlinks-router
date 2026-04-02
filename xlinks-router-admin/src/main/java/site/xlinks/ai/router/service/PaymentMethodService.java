package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.dto.PaymentMethodCreateDTO;
import site.xlinks.ai.router.dto.PaymentMethodUpdateDTO;
import site.xlinks.ai.router.entity.PaymentMethod;
import site.xlinks.ai.router.mapper.PaymentMethodMapper;
import site.xlinks.ai.router.vo.PaymentMethodVO;

@Service
@RequiredArgsConstructor
public class PaymentMethodService extends ServiceImpl<PaymentMethodMapper, PaymentMethod> {

    private final ObjectMapper objectMapper;

    public IPage<PaymentMethodVO> pageQuery(Integer page, Integer pageSize, String methodType, String keyword, Integer status) {
        String trimmedMethodType = StringUtils.hasText(methodType) ? methodType.trim() : null;
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        LambdaQueryWrapper<PaymentMethod> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(trimmedMethodType), PaymentMethod::getMethodType, trimmedMethodType)
                .eq(status != null, PaymentMethod::getStatus, status)
                .and(StringUtils.hasText(trimmedKeyword), query -> query.like(PaymentMethod::getMethodName, trimmedKeyword)
                        .or()
                        .like(PaymentMethod::getMethodCode, trimmedKeyword))
                .orderByAsc(PaymentMethod::getSort)
                .orderByDesc(PaymentMethod::getCreatedAt);

        Page<PaymentMethod> entityPage = this.page(new Page<>(page, pageSize), wrapper);
        Page<PaymentMethodVO> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(entityPage.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    public PaymentMethodVO getDetail(Long id) {
        return toVO(getEntity(id));
    }

    public PaymentMethodVO create(PaymentMethodCreateDTO dto) {
        ensureUniqueCode(dto.getMethodCode(), null);
        PaymentMethod method = new PaymentMethod();
        method.setMethodCode(dto.getMethodCode().trim());
        method.setMethodName(dto.getMethodName().trim());
        method.setMethodType(dto.getMethodType().trim());
        method.setIconUrl(normalizeOptional(dto.getIconUrl()));
        method.setSort(dto.getSort() == null ? 0 : dto.getSort());
        method.setStatus(normalizeBinaryStatus(dto.getStatus(), "Payment method status"));
        method.setConfigJson(normalizeJson(dto.getConfigJson()));
        method.setRemark(dto.getRemark());
        this.save(method);
        return getDetail(method.getId());
    }

    public PaymentMethodVO update(Long id, PaymentMethodUpdateDTO dto) {
        PaymentMethod existing = getEntity(id);
        PaymentMethod method = new PaymentMethod();
        method.setId(id);
        if (StringUtils.hasText(dto.getMethodName())) {
            method.setMethodName(dto.getMethodName().trim());
        }
        if (StringUtils.hasText(dto.getMethodType())) {
            method.setMethodType(dto.getMethodType().trim());
        }
        if (dto.getIconUrl() != null) {
            method.setIconUrl(normalizeOptional(dto.getIconUrl()));
        }
        if (dto.getSort() != null) {
            method.setSort(dto.getSort());
        }
        if (dto.getStatus() != null) {
            method.setStatus(normalizeBinaryStatus(dto.getStatus(), "Payment method status"));
        }
        if (dto.getConfigJson() != null) {
            method.setConfigJson(normalizeJson(dto.getConfigJson()));
        }
        if (dto.getRemark() != null) {
            method.setRemark(dto.getRemark());
        }
        this.updateById(method);
        return getDetail(existing.getId());
    }

    public void updateStatus(Long id, Integer status) {
        getEntity(id);
        PaymentMethod method = new PaymentMethod();
        method.setId(id);
        method.setStatus(normalizeBinaryStatus(status, "Payment method status"));
        this.updateById(method);
    }

    public void deleteById(Long id) {
        getEntity(id);
        super.removeById(id);
    }

    private PaymentMethod getEntity(Long id) {
        PaymentMethod method = super.getById(id);
        if (method == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Payment method not found");
        }
        return method;
    }

    private PaymentMethodVO toVO(PaymentMethod method) {
        PaymentMethodVO vo = new PaymentMethodVO();
        BeanUtils.copyProperties(method, vo);
        return vo;
    }

    private void ensureUniqueCode(String methodCode, Long currentId) {
        if (!StringUtils.hasText(methodCode)) {
            return;
        }
        LambdaQueryWrapper<PaymentMethod> wrapper = new LambdaQueryWrapper<PaymentMethod>()
                .eq(PaymentMethod::getMethodCode, methodCode.trim());
        if (currentId != null) {
            wrapper.ne(PaymentMethod::getId, currentId);
        }
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Payment method code already exists");
        }
    }

    private String normalizeJson(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Payment config must not be blank");
        }
        try {
            return objectMapper.writeValueAsString(objectMapper.readTree(value));
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Payment config must be valid JSON");
        }
    }

    private Integer normalizeBinaryStatus(Integer value, String fieldName) {
        if (value == null) {
            return 1;
        }
        if (value != 0 && value != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + " only supports 0 or 1");
        }
        return value;
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
