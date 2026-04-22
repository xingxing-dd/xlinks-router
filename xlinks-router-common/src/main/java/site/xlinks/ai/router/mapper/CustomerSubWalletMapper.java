package site.xlinks.ai.router.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import site.xlinks.ai.router.entity.CustomerSubWallet;

import java.util.List;

/**
 * Sub wallet mapper.
 */
@Mapper
public interface CustomerSubWalletMapper extends BaseMapper<CustomerSubWallet> {

    @Select("""
            SELECT id, main_wallet_id, wallet_no, wallet_type, balance, status, deleted, remark,
                   created_at, updated_at, create_by, update_by
            FROM customer_sub_wallets
            WHERE main_wallet_id = #{mainWalletId}
              AND deleted = 0
            ORDER BY id ASC
            FOR UPDATE
            """)
    List<CustomerSubWallet> selectByMainWalletIdForUpdate(@Param("mainWalletId") Long mainWalletId);
}
