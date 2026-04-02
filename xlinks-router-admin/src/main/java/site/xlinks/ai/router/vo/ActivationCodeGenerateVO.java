package site.xlinks.ai.router.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Activation code batch generate result.
 */
@Data
@Schema(description = "Activation code batch generate result")
public class ActivationCodeGenerateVO {

    private Long planId;

    private String planName;

    private Integer generatedCount;

    private List<String> codes;
}
