package bg.energo.phoenix.model.response.contract.order.service;

import bg.energo.phoenix.model.enums.product.service.ServiceExecutionLevel;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.PriceComponentFormula;
import bg.energo.phoenix.model.response.service.ServiceContractTermShortResponse;
import bg.energo.phoenix.model.response.terms.InvoicePaymentTermsResponse;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ServiceOrderServiceParametersFields {
    private ServiceExecutionLevel executionLevel;
    private List<InvoicePaymentTermsResponse> invoicePaymentTerms;
    private List<PriceComponentFormula> formulaVariables;
    private List<ServiceContractTermShortResponse> serviceContractTerms = new ArrayList<>();
    private boolean isQuantityVisible;
}
