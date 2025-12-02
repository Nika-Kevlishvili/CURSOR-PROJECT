package bg.energo.phoenix.model.response.document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SignatusDocumentResponse {
    private Long id;
    private String file;
    private String fileName;
    private String content;
    private String portal;
    private String kid;
}
