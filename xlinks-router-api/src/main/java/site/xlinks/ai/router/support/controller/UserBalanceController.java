package site.xlinks.ai.router.support.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.support.service.UserBalanceQueryService;

import java.util.Map;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class UserBalanceController {

    private final UserBalanceQueryService userBalanceQueryService;

    @GetMapping("/user/balance")
    public Map<String, Object> balance(HttpServletRequest request) {
        return userBalanceQueryService.queryBalance(request);
    }
}
