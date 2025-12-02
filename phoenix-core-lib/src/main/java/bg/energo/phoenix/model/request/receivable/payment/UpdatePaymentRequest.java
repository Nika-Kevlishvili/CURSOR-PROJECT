package bg.energo.phoenix.model.request.receivable.payment;

import bg.energo.phoenix.model.customAnotations.DateRangeValidator;
import bg.energo.phoenix.model.enums.receivable.payment.OutgoingDocumentType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePaymentRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "paymentDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate paymentDate;

    @DecimalMax(value = "999999999999.99", message = "initialAmount-InitialAmount must me less or equal to 999999999999.99;")
    @DecimalMin(value = "-999999999999.99", message = "initialAmount-InitialAmount be greater or equal to -999999999999.99;")
    @Digits(integer = 12, fraction = 150, message = "initialAmount-InitialAmount should have {integer} number before decimal point and {fraction} after decimal point;")
    private BigDecimal initialAmount;

    private Long currencyId;

    private Long collectionChannelId;

    private Long paymentPackageId;

    private Long accountPeriodId;

    @Length(min = 1,max = 2048,message = "paymentPurpose-PaymentPurpose length should be between {min}:{max} characters;")
    private String paymentPurpose;

    @Length(min = 1,max = 2048,message = "paymentInfo-PaymentInfo length should be between {min}:{max} characters;")
    private String paymentInfo;

    private boolean blockedForOffsetting;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "blockedForOffsettingFromDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate blockedForOffsettingFromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @DateRangeValidator(fieldPath = "blockedForOffsettingToDate", fromDate = "1990-01-01", toDate = "2090-12-31")
    private LocalDate blockedForOffsettingToDate;

    private Long blockedForOffsettingBlockingReasonId;

    @Length(min = 1,max = 2048,message = "blockedForOffsettingAdditionalInfo-BlockedForOffsettingAdditionalInfo length should be between {min}:{max} characters;")
    private String blockedForOffsettingAdditionalInfo;

    private Long customerId;

    private Long contractBillingGroupId;

    private OutgoingDocumentType outgoingDocumentType;

    private Long invoiceId;

    private Long latePaymentFineId;

    private Long customerDepositId;

    private Long penaltyId;
}
