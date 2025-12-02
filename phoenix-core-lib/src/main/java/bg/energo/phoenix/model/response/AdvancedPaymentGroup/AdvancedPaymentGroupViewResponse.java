package bg.energo.phoenix.model.response.AdvancedPaymentGroup;

import bg.energo.phoenix.model.enums.product.iap.advancedPaymentGroup.AdvancedPaymentGroupStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * <h1>AdvancedPaymentGroupViewResponse</h1>
 * {@link #id} InterimAdvancedPaymentGroup id
 * {@link #status} InterimAdvancedPaymentGroup status
 * {@link #name} InterimAdvancedPaymentGroup name
 * {@link #advancedPaymentGroupId} InterimAdvancedPaymentGroup id
 * {@link #versionId} InterimAdvancedPaymentGroup version id
 * {@link #advancedPayments} attached InterimAdvancedPayment info
 * {@link #versions} InterimAdvancedPaymentGroup version list
 */
@Data
@Builder
public class AdvancedPaymentGroupViewResponse {

    List<AdvancedPaymentSimpleInfoResponse> advancedPayments;
    private Long id;
    private AdvancedPaymentGroupStatus status;
    private String name;
    private LocalDate startDate;
    private Long advancedPaymentGroupId;
    private Long versionId;
    private List<AdvancedPaymentGroupVersionResponse> versions;

    private Boolean isLocked;

}
