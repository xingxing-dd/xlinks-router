package site.xlinks.ai.router.client.dto.auth;

import lombok.Data;

@Data
public class RsaPublicKeyResponse {

    private String algorithm;

    private String publicKey;
}
