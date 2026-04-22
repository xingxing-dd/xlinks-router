package site.xlinks.ai.router.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import site.xlinks.ai.router.entity.CustomerMainWalletFlow;

/**
 * Main wallet flow mapper.
 */
@Mapper
public interface CustomerMainWalletFlowMapper extends BaseMapper<CustomerMainWalletFlow> {

    @Select("""
            SELECT id, main_wallet_id, account_id, order_no, biz_type, direction, change_amount,
                   total_balance_before, total_balance_after, available_balance_before, available_balance_after,
                   remark, created_at, updated_at, create_by, update_by
            FROM customer_main_wallet_flows
            WHERE main_wallet_id = #{mainWalletId}
              AND order_no = #{orderNo}
              AND biz_type = #{bizType}
            LIMIT 1
            """)
    CustomerMainWalletFlow selectByOrderNoAndBizType(@Param("mainWalletId") Long mainWalletId,
                                                     @Param("orderNo") String orderNo,
                                                     @Param("bizType") String bizType);
}
