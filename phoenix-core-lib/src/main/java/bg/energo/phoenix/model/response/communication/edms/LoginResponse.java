package bg.energo.phoenix.model.response.communication.edms;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private LoginResponseContent token;
}
