package bg.energo.phoenix.model.customAnotations.contract.service;

import bg.energo.phoenix.model.enums.contract.service.ServiceContractContractType;
import bg.energo.phoenix.model.enums.product.product.PaymentGuarantee;
import bg.energo.phoenix.model.enums.product.term.terms.WaitForOldContractTermToExpire;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.ContractEntryIntoForceForContractFields;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.DepositResponse;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.InstallmentForContractFields;
import bg.energo.phoenix.model.response.contract.productContract.priceComponentFormula.StartOfContractInitialTermsForContractFields;
import bg.energo.phoenix.model.response.contract.serviceContract.ServiceContractAdditionalParamsResponse;
import bg.energo.phoenix.model.response.service.ServiceContractTermShortResponse;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ServiceContractThirdPageFields {
    private List<ServiceContractContractType> contractTypes = new ArrayList<>();

    private List<ServiceContractTermShortResponse> serviceContractTerms = new ArrayList<>();

    private List<ServiceContractInvoicePaymentTermsResponse> invoicePaymentTerms = new ArrayList<>();

    private List<PaymentGuarantee> paymentGuarantees;

    private List<ServiceContractPriceComponentFormula> formulaVariables = new ArrayList<>();

    private List<ServiceContractInterimAdvancePaymentResponse> interimAdvancePayments = new ArrayList<>();

    private List<ServiceContractAdditionalParamsResponse> serviceContractAdditionalParamsResponses = new ArrayList<>();

    private ContractEntryIntoForceForContractFields contractEntryIntoForces;

    private StartOfContractInitialTermsForContractFields startOfContractInitialTermsForContractFields;

    //private SupplyActivationsForContractFields supplyActivationsForContractFields;

    private InstallmentForContractFields installmentForContractFields;

    private List<WaitForOldContractTermToExpire> waitForOldContractTermToExpires;

    private DepositResponse depositResponse;

    private boolean termFromGroup;

    private boolean isQuantityVisible;

}
