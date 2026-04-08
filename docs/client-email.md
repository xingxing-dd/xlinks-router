# Client Mail Service (Mailtrap)

## Overview

The client module uses **Mailtrap Send API** to deliver email verification codes.

Current endpoint:

- `POST https://send.api.mailtrap.io/api/send`

Success response example:

```json
{
  "success": true,
  "message_ids": [
    "b82532f0-3289-11f1-0040-f1c49622d009"
  ]
}
```

## Related Code

- `xlinks-router-client/src/main/java/site/xlinks/ai/router/client/config/MailtrapEmailProperties.java`
- `xlinks-router-client/src/main/java/site/xlinks/ai/router/client/service/MailtrapEmailClient.java`
- `xlinks-router-client/src/main/java/site/xlinks/ai/router/client/service/verifycode/EmailVerifyCodeSender.java`

## Configuration

```yaml
xlinks:
  email:
    mailtrap:
      enabled: true
      access-token: ${XLS_EMAIL_MAILTRAP_ACCESS_TOKEN:}
      sender-email: ${XLS_EMAIL_MAILTRAP_SENDER_EMAIL:tech@xlinks.site}
      sender-name: ${XLS_EMAIL_MAILTRAP_SENDER_NAME:xlinks-tech}
      sender-name-template: ${XLS_EMAIL_MAILTRAP_SENDER_NAME_TEMPLATE:{appName}}
      verify-code-subject-template: "${XLS_EMAIL_MAILTRAP_VERIFY_CODE_SUBJECT_TEMPLATE:[{appName}] {sceneLabel} verification code}"
      app-name: ${XLS_EMAIL_MAILTRAP_APP_NAME:xlinks}
      api-url: ${XLS_EMAIL_MAILTRAP_API_URL:https://send.api.mailtrap.io/api/send}
      category: ${XLS_EMAIL_MAILTRAP_CATEGORY:Integration Test}
```

## Template Variables

The following placeholders are supported in `sender-name-template` and `verify-code-subject-template`:

- `{appName}`: application name
- `{scene}`: raw scene code, such as `register` / `resetpwd`
- `{sceneLabel}`: readable scene label, such as `register` / `reset password`

Default scene mapping:

- `register` -> `register`
- `resetpwd` -> `reset password`
- other values -> `verification`

## Verify Code Email Behavior

When `POST /api/v1/auth/verify-code` is called with `codeType=email`:

1. the system generates the verification code in Redis
2. resolves the email subject from `verify-code-subject-template`
3. resolves the sender display name from `sender-name-template`
4. sends both `text` and `html` bodies through Mailtrap

Example rendered defaults:

- register subject: `[xlinks] register verification code`
- reset password subject: `[xlinks] reset password verification code`