package site.xlinks.ai.router.client.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.client.dto.contact.ContactSubmitRequest;
import site.xlinks.ai.router.common.result.Result;

@RestController
@RequestMapping("/api/v1")
public class ContactController {

    @PostMapping("/contact")
    public Result<Void> submitContact(@Valid @RequestBody ContactSubmitRequest request) {
        return Result.success();
    }
}
