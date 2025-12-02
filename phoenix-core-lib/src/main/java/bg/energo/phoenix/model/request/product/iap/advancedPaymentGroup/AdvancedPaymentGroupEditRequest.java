package bg.energo.phoenix.model.request.product.iap.advancedPaymentGroup;

import bg.energo.phoenix.model.customAnotations.product.interimAdvancePaymentGroup.ValidEditInterimAdvancedPaymentGroupRequest;
import bg.energo.phoenix.model.entity.product.iap.interimAdvancePayment.InterimAdvancePayment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * <h1>AdvancedPaymentGroupEditRequest</h1>
 * {@link #name} AdvancedPaymentGroup name
 * {@link #advancedPayments} List of {@link InterimAdvancePayment} ids
 * {@link #versionId} AdvancedPaymentGroup version
 * {@link #updateExistingVersion} parameter to update existing version or create new one
 * {@link #startDate} start date
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ValidEditInterimAdvancedPaymentGroupRequest
public class AdvancedPaymentGroupEditRequest {

    @Size(min = 1, max = 1024, message = "name-length should be {min} to {max};")
    private String name;

    private List<Long> advancedPayments;

    @NotNull(message = "detailsVersion-detailsVersion version is required;")
    private Long versionId;

    @NotNull(message = "updateExistingVersion-updateExistingVersion is required;")
    private Boolean updateExistingVersion;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

}
