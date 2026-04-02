package site.xlinks.ai.router.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Third-party pay link view object")
public class PayLinkVO {

    private Long id;

    private Long targetId;

    private String targetType;

    private String planName;

    private Integer planStatus;

    private Integer planVisible;

    private String payUrl;

    private Integer status;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
