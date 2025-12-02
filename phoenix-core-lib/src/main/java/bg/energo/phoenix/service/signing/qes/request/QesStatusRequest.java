package bg.energo.phoenix.service.signing.qes.request;

import lombok.Data;

@Data
public class QesStatusRequest {
    private String path;
    private Long fileId;
}
