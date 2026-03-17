package site.xlinks.ai.router.client.dto.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerModelItemResponse {
    private Long id;
    private String logicModelCode;
    private String logicModelName;
    private String modelType;
    private Integer status;
    private Integer isDefault;
    private String remark;
    private String createdAt;
}
