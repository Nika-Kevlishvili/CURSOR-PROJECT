package bg.energo.phoenix.model.request.communication.edms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublishDocumentRequest {
    private Boolean asNewRevision;
}
