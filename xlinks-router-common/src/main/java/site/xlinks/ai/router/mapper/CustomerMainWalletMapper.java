package site.xlinks.ai.router.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import site.xlinks.ai.router.entity.CustomerMainWallet;

/**
 * Main wallet mapper.
 */
@Mapper
public interface CustomerMainWalletMapper extends BaseMapper<CustomerMainWallet> {

    @Select("""
            SELECT id, account_id, wallet_no, total_balance, available_balance, allow_in, allow_out, status, deleted, remark,
                   created_at, updated_at, create_by, update_by
            FROM customer_main_wallets
            WHERE account_id = #{accountId}
              AND deleted = 0
            LIMIT 1
            FOR UPDATE
            """)
    CustomerMainWallet selectByAccountIdForUpdate(@Param("accountId") Long accountId);
}
