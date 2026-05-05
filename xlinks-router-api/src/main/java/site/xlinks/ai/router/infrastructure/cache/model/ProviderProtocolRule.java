package site.xlinks.ai.router.infrastructure.cache.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderProtocolRule {

    private boolean allowAll;
    private Set<String> normalizedProtocols;
}
