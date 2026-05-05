package site.xlinks.ai.router.domain.provider;

import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderToken;

import java.util.List;

public interface ProviderTokenSelectionService {

    ProviderToken select(Provider provider, List<ProviderToken> candidates);
}
