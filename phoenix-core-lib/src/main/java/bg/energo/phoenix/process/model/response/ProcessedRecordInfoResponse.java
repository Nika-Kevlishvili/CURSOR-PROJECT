package bg.energo.phoenix.process.model.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProcessedRecordInfoResponse {

    private Long id;

    private Long recordId;

    private String recordIdentifier;

    private String recordIdentifierVersion;

    private boolean success;

    private String errorMessage;

}
