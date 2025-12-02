package bg.energo.phoenix.model.response.contract.productContract;

import bg.energo.phoenix.model.enums.product.product.ContractType;
import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.enums.product.term.terms.WaitForOldContractTermToExpire;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.*;
import bg.energo.phoenix.model.response.interimAdvancePayment.InterimAdvancePaymentResponse;
import bg.energo.phoenix.model.response.product.ProductContractTermsResponse;
import bg.energo.phoenix.model.response.terms.InvoicePaymentTermsResponse;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProductContractThirdPageFields {
    private List<ContractType> contractTypes = new ArrayList<>();

    private List<ProductContractTermsResponse>  productContractTerms = new ArrayList<>();

    private List<InvoicePaymentTermsResponse> invoicePaymentTerms = new ArrayList<>();

    private List<PaymentGuarantee> paymentGuarantees = new ArrayList<>();

    private List<PriceComponentFormula> formulaVariables = new ArrayList<>();

    private List<InterimAdvancePaymentResponse> interimAdvancePayments = new ArrayList<>();

    private ContractEntryIntoForceForContractFields contractEntryIntoForces ;

    private StartOfContractInitialTermsForContractFields startOfContractInitialTermsForContractFields;

    private SupplyActivationsForContractFields supplyActivationsForContractFields;

    private InstallmentForContractFields installmentForContractFields;

    private List<WaitForOldContractTermToExpire> waitForOldContractTermToExpires;

    private DepositResponse depositResponse;

    private List<ProductContractAdditionalParamsResponse> productAdditionalParams;

    private boolean termFromGroup;
}
