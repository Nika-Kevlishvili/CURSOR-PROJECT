package bg.energo.phoenix.model.response.communication.edms;

import lombok.Data;

@Data
public class LoginResponseContent {
    private String accessToken;
    private Long expiresIn;
    private String tokenType;
}
