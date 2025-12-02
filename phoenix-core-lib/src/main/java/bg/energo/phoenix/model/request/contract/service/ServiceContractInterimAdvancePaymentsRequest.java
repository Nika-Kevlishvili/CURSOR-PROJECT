package bg.energo.phoenix.model.request.contract.service;

import bg.energo.phoenix.model.request.contract.product.PriceComponentContractFormula;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceContractInterimAdvancePaymentsRequest {

    private Integer issueDate;
    @DecimalMin(value = "0.01",message = "productParameters.interimAdvancePayments.value-value can not be less than 0.01;")
    @DecimalMax(value = "999999999.99",message = "productParameters.interimAdvancePayments.value-value can not be greater than 999999999.99;")
    private BigDecimal value;
    @NotNull(message = "serviceParameters.interimAdvancePaymentId-interimAdvancePaymentId can not be null;")
    private Long interimAdvancePaymentId;
    private Integer termValue;

    private List<@Valid PriceComponentContractFormula> contractFormulas = new ArrayList<>();
}
