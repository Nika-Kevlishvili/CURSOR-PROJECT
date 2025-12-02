package bg.energo.phoenix.model.request.pod.pod;

import lombok.Data;

import java.time.LocalDate;
@Data
public class BlockedBillingRequest {
    private LocalDate from;
    private LocalDate to;
    private String reason;
    private String additionalInfo;
}
