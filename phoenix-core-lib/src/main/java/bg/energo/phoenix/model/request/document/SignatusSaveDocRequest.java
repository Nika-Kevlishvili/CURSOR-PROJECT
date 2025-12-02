package bg.energo.phoenix.model.request.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignatusSaveDocRequest {
    private Long id;
    private String file;
    private String fileName;
    private String kid;
    private String key;
}
