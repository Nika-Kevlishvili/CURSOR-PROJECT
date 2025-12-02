package bg.energo.phoenix.model.request.customer.list;

import bg.energo.phoenix.model.enums.customer.list.CustomerRelatedLiabilityAndReceivableSearchField;
import bg.energo.phoenix.model.request.receivable.customerLiability.CustomerLiabilityAndReceivableListColumns;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRelatedLiabilitiesAndReceivablesListRequest {

    @NotNull(message = "page-Page must not be null;")
    private Integer page;

    @NotNull(message = "size-Size must not be null;")
    private Integer size;

    private String prompt;

    private CustomerRelatedLiabilityAndReceivableSearchField searchFields;

    private Boolean blockedForPayments;

    private Boolean blockedForReminders;

    private Boolean blockedForInterest;

    private Boolean blockedForOffsetting;

    private Boolean blockedForDisconnection;

    private Boolean showDeposits = false;

    private Boolean showLiabilitiesAndReceivables;

    private CustomerLiabilityAndReceivableListColumns columns;

    private Sort.Direction direction;


}
