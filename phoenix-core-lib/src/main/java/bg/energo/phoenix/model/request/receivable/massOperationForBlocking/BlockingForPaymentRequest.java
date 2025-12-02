package bg.energo.phoenix.model.request.receivable.massOperationForBlocking;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableBlockingReasonType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlockingForPaymentRequest {

    @NotNull(message = "blockingForPayment.fromDate-fromDate must be provided;")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "blockingForPayment.fromDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "blockingForPayment.toDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate toDate;

    @NotNull(message = "blockingForPayment.reasonId-reasonId should not be null;")
    private Long reasonId;

    @Size(min = 1, max = 2048, message = "blockingForPayment.additionalInformation-additional Information size should be between {min} and {max} characters;")
    private String additionalInformation;

    @NotNull(message = "blockingForPayment.reasonType-blocking reason type should not be null;")
    private ReceivableBlockingReasonType reasonType;

}
