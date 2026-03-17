package site.xlinks.ai.router.client.dto.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContactSubmitRequest {
    @NotBlank(message = "name不能为空")
    private String name;

    @Email(message = "邮箱格式不正确")
    @NotBlank(message = "email不能为空")
    private String email;

    @NotBlank(message = "subject不能为空")
    private String subject;

    @NotBlank(message = "message不能为空")
    private String message;
}
