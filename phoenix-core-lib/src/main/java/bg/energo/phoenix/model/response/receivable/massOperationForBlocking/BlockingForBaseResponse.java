package bg.energo.phoenix.model.response.receivable.massOperationForBlocking;

import java.time.LocalDate;

public record BlockingForBaseResponse(
        LocalDate fromDate,
        LocalDate toDate,
        BlockingReasonShortResponse blockingReason,
        String additionalInformation
) {
}
