package bg.energo.phoenix.service.signing.qes.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QesSigningStatusUpdateResponse {

    private Integer transferredCount;
    private Integer totalTransferCount;
    private Integer signedCount;
    private Integer totalToSign;
}
