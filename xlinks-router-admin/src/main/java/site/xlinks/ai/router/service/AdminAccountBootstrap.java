package site.xlinks.ai.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Bootstrap default admin account on startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountBootstrap {

    private final AdminAccountService adminAccountService;

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrap() {
        adminAccountService.bootstrapDefaultAdminIfNecessary();
        log.info("Admin account bootstrap finished");
    }
}
