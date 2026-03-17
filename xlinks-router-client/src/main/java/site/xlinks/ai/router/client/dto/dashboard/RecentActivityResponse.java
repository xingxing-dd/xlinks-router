package site.xlinks.ai.router.client.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityResponse {
    private String time;
    private String event;
    private String tokens;
    private String status;
}
