package site.xlinks.ai.router.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import site.xlinks.ai.router.entity.PaymentMethod;

@Mapper
public interface PaymentMethodMapper extends BaseMapper<PaymentMethod> {
}
