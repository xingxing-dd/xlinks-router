package site.xlinks.ai.router.model.wallet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import site.xlinks.ai.router.entity.CustomerMainWallet;
import site.xlinks.ai.router.entity.CustomerSubWallet;

import java.util.List;

/**
 * Wallet aggregate snapshot.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletBundle {

    private CustomerMainWallet mainWallet;

    private List<CustomerSubWallet> subWallets;
}
