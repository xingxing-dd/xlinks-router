package site.xlinks.ai.router.client.dto.token;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerTokenSummaryResponse {
    private Integer totalTokens;
    private Integer activeTokens;
    private Integer disabledTokens;
    private Integer expiredTokens;
    private Long totalRequests;
}
