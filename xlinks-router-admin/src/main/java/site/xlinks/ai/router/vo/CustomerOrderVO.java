package site.xlinks.ai.router.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CustomerOrderVO {

    private Long id;

    private String orderNo;

    private String refNo;

    private Long accountId;

    private String accountName;

    private String accountPhone;

    private String accountEmail;

    private String orderType;

    private String orderTitle;

    private String orderInfo;

    private String paymentChannel;

    private BigDecimal totalAmount;

    private Integer status;

    private LocalDateTime completeAt;

    private LocalDateTime expiredAt;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
