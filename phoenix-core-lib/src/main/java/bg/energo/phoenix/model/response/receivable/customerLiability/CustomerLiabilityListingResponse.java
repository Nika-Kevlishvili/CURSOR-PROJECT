package bg.energo.phoenix.model.response.receivable.customerLiability;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.billing.accountingsPeriods.AccountingPeriodStatus;
import bg.energo.phoenix.model.enums.receivable.CreationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerLiabilityListingResponse {

    private Long customerId;
    private String liabilityNumber;
    private String customer;
    private String billingGroup;
    private LocalDate dueDate;
    private List<CustomerLiabilityPodResponse> pods;
    private String alternativeRecipientOfAnInvoice;
    private BigDecimal initialAmount;
    private BigDecimal currentAmount;
    private Long currencyId;
    private String currencyName;
    private Long id;
    private EntityStatus status;
    private CreationType creationType;
    private AccountingPeriodStatus accountingPeriodStatus;
    private LocalDate occurrenceDate;

}
