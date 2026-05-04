package site.xlinks.ai.router.distributed.infrastructure.cache.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderFailureState {

    private int failureCount;
    private Instant firstFailureAt;
}
