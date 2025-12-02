package bg.energo.phoenix.model.documentModels.contract.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ContractMainResponse {

    String getCustomerNameComb();

    String getCustomerNameCombTrsl();

    String getCustomerIdentifier();

    String getCustomerNumber();

    String getHeadquarterAddressComb();

    String getHeadquarterPopulatedPlace();

    String getHeadquarterZip();

    String getHeadquarterDistrict();

    String getHeadquarterQuarterRaType();

    String getHeadquarterQuarterRaTypeTrsl();

    String getHeadquarterStrBlvdTypeTrsl();

    String getCommunicationQuarterRaTypeTrsl();

    String getCommunicationStrBlvdTypeTrsl();

    String getBillingQuarterRaTypeTrsl();

    String getBillingStrBlvdTypeTrsl();

    String getHeadquarterQuarterRaName();

    String getHeadquarterStrBlvdType();

    String getHeadquarterStrBlvdName();

    String getHeadquarterStrBlvdNumber();

    String getHeadquarterBlock();

    String getHeadquarterEntrance();

    String getHeadquarterFloor();

    String getHeadquarterApartment();

    String getHeadquarterAdditionalInfo();

    String getCommunicationAddressComb();

    String getCommunicationPopulatedPlace();

    String getCommunicationZip();

    String getCommunicationDistrict();

    String getCommunicationQuarterRaType();

    String getCommunicationQuarterRaName();

    String getCommunicationStrBlvdType();

    String getCommunicationStrBlvdName();

    String getCommunicationStrBlvdNumber();

    String getCommunicationBlock();

    String getCommunicationEntrance();

    String getCommunicationFloor();

    String getCommunicationApartment();

    String getCommunicationAdditionalInfo();

    String getCommunicationEmailComb();

    String getCommunicationMobileComb();

    String getCommunicationPhoneComb();

    String getCommunicationEmail();

    String getCommunicationMobile();

    String getCommunicationPhone();

    String getBillingAddressComb();

    String getBillingPopulatedPlace();

    String getBillingZip();

    String getBillingDistrict();

    String getBillingQuarterRaType();

    String getBillingQuarterRaName();

    String getBillingStrBlvdType();

    String getBillingStrBlvdName();

    String getBillingStrBlvdNumber();

    String getBillingBlock();

    String getBillingEntrance();

    String getBillingFloor();

    String getBillingApartment();

    String getBillingAdditionalInfo();

    String getBillingEmailComb();

    String getBillingMobileComb();

    String getBillingPhoneComb();

    String getBillingEmail();

    String getBillingMobile();

    String getBillingPhone();

    String getCustomerSegments();

    String getProductName();

    String getProductNameTrsl();

    String getProductPrintName();

    String getProductPrintNameTrsl();

    String getTextInvoicesTemplates();

    String getTextInvoicesTemplatesTrsl();

    String getContractType();

    String getPaymentGuaranteeType();

    BigDecimal getDepositAmount();

    String getDepositCurrencyPrintName();

    String getDepositCurrencyAbr();

    String getDepositCurrencyFullName();

    BigDecimal getBankGuaranteeAmount();

    String getBankGuaranteeCurrencyPrintName();

    String getBankGuaranteeCurrencyAbr();

    String getBankGuaranteeCurrencyFullName();

    String getRegistrationSchedules();

    String getForecasting();

    String getTakingBalancingCosts();

    String getAdditionalField1();

    String getAdditionalField2();

    String getAdditionalField3();

    String getAdditionalField4();

    String getAdditionalField5();

    String getAdditionalField6();

    String getAdditionalField7();

    String getAdditionalField8();

    String getAdditionalField9();

    String getAdditionalField10();

    String getAdditionalParametersLabel1();

    String getAdditionalParametersValue1();

    String getAdditionalParametersLabel2();

    String getAdditionalParametersValue2();

    String getAdditionalParametersLabel3();

    String getAdditionalParametersValue3();

    String getAdditionalParametersLabel4();

    String getAdditionalParametersValue4();

    String getAdditionalParametersLabel5();

    String getAdditionalParametersValue5();

    String getAdditionalParametersLabel6();

    String getAdditionalParametersValue6();

    String getAdditionalParametersLabel7();

    String getAdditionalParametersValue7();

    String getAdditionalParametersLabel8();

    String getAdditionalParametersValue8();

    String getAdditionalParametersLabel9();

    String getAdditionalParametersValue9();

    String getAdditionalParametersLabel10();

    String getAdditionalParametersValue10();

    String getAdditionalParametersLabel11();

    String getAdditionalParametersValue11();

    String getAdditionalParametersLabel12();

    String getAdditionalParametersValue12();

    String getContractDocumentType();

    String getContractVersionType();

    String getContractNumber();

    String getAdditionalSuffix();

    LocalDate getCreationDate();

    LocalDate getSigningDate();

    String getApplicableInterestRate();

    String getCampaign();

    String getContractBank();

    String getContractBIC();

    String getContractIBAN();

    String getCustomerBank();

    String getCustomerBIC();

    String getCustomerIBAN();

    String getEstimatedContractConsumption();

    String getEmployee();

    String getAssistingEmployee();

    String getInternalIntermediary();

    String getExternalIntermediary();

    String getContractTermType();

    String getContractTermValue();

    String getContractTermValueType();

    String getContractTermPerpetuity();

    String getContractTermRenewal();

    String getContractTermRenewalValue();

    String getContractTermRenewalType();

    String getPaymentTermType();

    String getPaymentTermValue();

    String getTermActivationDeliveryValue();

    String getTermActivationDeliveryType();

    String getDeadlineEarlyResigningValue();

    String getDeadlineEarlyResigningType();

    String getGeneralNoticePeriodValue();

    String getGeneralNoticePeriodType();

    String getNoticeTermValue();

    String getNoticeTermType();

    String getNoticeTermDisconnectionValue();

    String getNoticeTermDisconnectionType();

    String getSupplyActivationType();

    String getSupplyActivationValue();

    String getWaitOldContractToExpire();

    String getEntryIntoForceType();

    String getEntryIntoForceValue();

    String getStartInitialTermType();

    String getStartInitialTermValue();

    String getInterimAdvancePaymentsYN();

    String getInterimAdvancePaymentsList();

    BigDecimal getMarginalPrice();

    String getMarginalPriceValidity();

    BigDecimal getPav();

    BigDecimal getProcurementPrice();

    BigDecimal getImbalancesPrice();

    BigDecimal getSetMargin();

    String getServicePODs();

    String getServiceContracts();

    Integer getQuantity();

    String getCustomerType();

    LocalDate getVersionStartDate();

    LocalDate getEntryForceDate();

    LocalDate getContractTermStartDate();
}
