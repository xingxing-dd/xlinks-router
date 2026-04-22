package site.xlinks.ai.router.vo;

import lombok.Data;

@Data
public class WalletBatchOpenResultVO {

    private Integer totalAccountCount;

    private Integer existingWalletCount;

    private Integer createdWalletCount;
}
