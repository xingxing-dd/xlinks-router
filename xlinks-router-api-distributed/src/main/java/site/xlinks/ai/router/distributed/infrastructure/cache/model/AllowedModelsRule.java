package site.xlinks.ai.router.distributed.infrastructure.cache.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllowedModelsRule {

    private boolean allowAll;
    private Set<String> allowedModels;
    private List<String> allowedModelList;

    public static AllowedModelsRule allowAll() {
        return new AllowedModelsRule(true, Set.of(), List.of());
    }
}
