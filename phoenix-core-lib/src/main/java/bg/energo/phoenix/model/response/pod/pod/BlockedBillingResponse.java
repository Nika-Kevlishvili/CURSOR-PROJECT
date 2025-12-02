package bg.energo.phoenix.model.response.pod.pod;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BlockedBillingResponse {
    private LocalDate from;
    private LocalDate to;
    private String reason;
    private String additionalInfo;
}
