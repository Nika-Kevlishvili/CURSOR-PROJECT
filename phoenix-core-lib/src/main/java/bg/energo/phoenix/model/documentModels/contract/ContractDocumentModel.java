package bg.energo.phoenix.model.documentModels.contract;

import bg.energo.phoenix.model.documentModels.contract.response.ContractMainResponse;
import bg.energo.phoenix.service.billing.model.impl.CompanyDetailedInformationModelImpl;
import bg.energo.phoenix.util.epb.EPBListUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class ContractDocumentModel extends CompanyDetailedInformationModelImpl {
    public String CustomerNameComb;
    public String CustomerNameCombTrsl;
    public String CustomerIdentifer;
    public String CustomerNumber;
    public String CustomerType;
    public String HeadquarterAddressComb;
    public String HeadquarterPopulatedPlace;
    public String HeadquarterZip;
    public String HeadquarterDistrict;
    public String HeadquarterQuarterRaType;
    public String HeadquarterQuarterRaName;
    public String HeadquarterStrBlvdType;
    public String HeadquarterStrBlvdName;
    public String HeadquarterStrBlvdNumber;
    public String HeadquarterBlock;
    public String HeadquarterEntrance;
    public String HeadquarterFloor;
    public String HeadquarterApartment;
    public String HeadquarterAdditionalInfo;

    public String HeadquarterAddressCombTrsl;
    public String HeadquarterPopulatedPlaceTrsl;
    public String HeadquarterDistrictTrsl;
    public String HeadquarterQuarterRaTypeTrsl;
    public String HeadquarterQuarterRaNameTrsl;
    public String HeadquarterStrBlvdTypeTrsl;
    public String HeadquarterStrBlvdNameTrsl;
    public String HeadquarterStrBlvdNumberTrsl;
    public String HeadquarterBlockTrsl;
    public String HeadquarterEntranceTrsl;
    public String HeadquarterFloorTrsl;
    public String HeadquarterApartmentTrsl;
    public String HeadquarterAdditionalInfoTrsl;
    public String CommunicationAddressComb;
    public String CommunicationPopulatedPlace;
    public String CommunicationZip;
    public String CommunicationDistrict;
    public String CommunicationQuarterRaType;
    public String CommunicationQuarterRaName;
    public String CommunicationStrBlvdType;
    public String CommunicationStrBlvdName;
    public String CommunicationStrBlvdNumber;
    public String CommunicationBlock;
    public String CommunicationEntrance;
    public String CommunicationFloor;
    public String CommunicationApartment;
    public String CommunicationAdditionalInfo;
    public String CommunicationEmailComb;
    public String CommunicationMobileComb;
    public String CommunicationPhoneComb;
    public List<String> CommunicationEmail;
    public List<String> CommunicationMobile;
    public List<String> CommunicationPhone;
    public String CommunicationAddressCombTrsl;
    public String CommunicationPopulatedPlaceTrsl;
    public String CommunicationDistrictTrsl;
    public String CommunicationQuarterRaTypeTrsl;
    public String CommunicationQuarterRaNameTrsl;
    public String CommunicationStrBlvdTypeTrsl;
    public String CommunicationStrBlvdNameTrsl;
    public String CommunicationStrBlvdNumberTrsl;
    public String CommunicationBlockTrsl;
    public String CommunicationEntranceTrsl;
    public String CommunicationFloorTrsl;
    public String CommunicationApartmentTrsl;
    public String CommunicationAdditionalInfoTrsl;
    public String BillingAddressComb;
    public String BillingPopulatedPlace;
    public String BillingZip;
    public String BillingDistrict;
    public String BillingQuarterRaType;
    public String BillingQuarterRaName;
    public String BillingStrBlvdType;
    public String BillingStrBlvdName;
    public String BillingStrBlvdNumber;
    public String BillingBlock;
    public String BillingEntrance;
    public String BillingFloor;
    public String BillingApartment;
    public String BillingAdditionalInfo;
    public String BillingEmailComb;
    public String BillingMobileComb;
    public String BillingPhoneComb;
    public List<String> BillingEmail;
    public List<String> BillingMobile;
    public List<String> BillingPhone;
    public String BillingAddressCombTrsl;
    public String BillingPopulatedPlaceTrsl;
    public String BillingDistrictTrsl;
    public String BillingQuarterRaTypeTrsl;
    public String BillingQuarterRaNameTrsl;
    public String BillingStrBlvdTypeTrsl;
    public String BillingStrBlvdNameTrsl;
    public String BillingStrBlvdNumberTrsl;
    public String BillingBlockTrsl;
    public String BillingEntranceTrsl;
    public String BillingFloorTrsl;
    public String BillingApartmentTrsl;
    public String BillingAdditionalInfoTrsl;
    public List<String> CustomerSegments;
    public List<ManagerModel> Managers;
    public String ProductName;
    public String ProductNameTrsl;
    public String ProductPrintName;
    public String ProductPrintNameTrsl;
    public String TextInvoicesTemplates;
    public String TextInvoicesTemplatesTrsl;
    public String ContractType;
    public String PaymentGuaranteeType;
    public BigDecimal DepositAmount;
    public String DepositCurrencyPrintName;
    public String DepositCurrencyAbr;
    public String DepositCurrencyFullName;
    public BigDecimal BankGuaranteeAmount;
    public String BankGuaranteeCurrencyPrintName;
    public String BankGuaranteeCurrencyAbr;
    public String BankGuaranteeCurrencyFullName;
    public String RegistrationSchedules;
    public String Forecasting;
    public String TakingBalancingCosts;
    public String AdditionalField1;
    public String AdditionalField2;
    public String AdditionalField3;
    public String AdditionalField4;
    public String AdditionalField5;
    public String AdditionalField6;
    public String AdditionalField7;
    public String AdditionalField8;
    public String AdditionalField9;
    public String AdditionalField10;
    public String AdditionalParametersLabel1;
    public String AdditionalParametersValue1;
    public String AdditionalParametersLabel2;
    public String AdditionalParametersValue2;
    public String AdditionalParametersLabel3;
    public String AdditionalParametersValue3;
    public String AdditionalParametersLabel4;
    public String AdditionalParametersValue4;
    public String AdditionalParametersLabel5;
    public String AdditionalParametersValue5;
    public String AdditionalParametersLabel6;
    public String AdditionalParametersValue6;
    public String AdditionalParametersLabel7;
    public String AdditionalParametersValue7;
    public String AdditionalParametersLabel8;
    public String AdditionalParametersValue8;
    public String AdditionalParametersLabel9;
    public String AdditionalParametersValue9;
    public String AdditionalParametersLabel10;
    public String AdditionalParametersValue10;
    public String AdditionalParametersLabel11;
    public String AdditionalParametersValue11;
    public String AdditionalParametersLabel12;
    public String AdditionalParametersValue12;
    public String ContractDocumentType;
    public String ContractVersionType;
    public String ContractNumber;
    public String AdditionalSuffix;
    public LocalDate CreationDate;
    public LocalDate SigningDate;
    public LocalDate VersionStartDate;
    public LocalDate EntryForceDate;
    public LocalDate ContractTermStartDate;
    public String ApplicableInterestRate;
    public String Campaign;
    public String ContractBank;
    public String ContractBIC;
    public String ContractIBAN;
    public String CustomerBank;
    public String CustomerBIC;
    public String CustomerIBAN;
    public String EstimatedContractConsumption;
    public String Employee;
    public List<String> AssistingEmployee;
    public List<String> InternalIntermediary;
    public List<String> ExternalIntermediary;
    public String ContractTermType;
    public String ContractTermValue;
    public String ContractTermValueType;
    public String ContractTermValueTypeTrsl;
    public String ContractTermPerpetuity;
    public String ContractTermRenewal;
    public String ContractTermRenewalValue;
    public String ContractTermRenewalType;
    public String ContractTermRenewalTypeTrsl;
    public String PaymentTermType;
    public String PaymentTermTypeTrsl;
    public String PaymentTermValue;
    public String TermActivationDeliveryValue;
    public String TermActivationDeliveryType;
    public String TermActivationDeliveryTypeTrsl;
    public String DeadlineEarlyResigningValue;
    public String DeadlineEarlyResigningType;
    public String DeadlineEarlyResigningTypeTrsl;
    public String GeneralNoticePeriodValue;
    public String GeneralNoticePeriodType;
    public String GeneralNoticePeriodTypeTrsl;
    public String NoticeTermValue;
    public String NoticeTermType;
    public String NoticeTermTypeTrsl;
    public String NoticeTermDisconnectionValue;
    public String NoticeTermDisconnectionType;
    public String NoticeTermDisconnectionTypeTrsl;
    public String SupplyActivationType;
    public String SupplyActivationTypeTrsl;
    public String SupplyActivationValue;
    public String WaitOldContractToExpire;
    public String EntryIntoForceType;
    public String EntryIntoForceTypeTrsl;
    public String EntryIntoForceValue;
    public String StartInitialTermType;
    public String StartInitialTermTypeTrsl;
    public String StartInitialTermValue;
    public String InterimAdvancePaymentsYN;
    public List<String> InterimAdvancePaymentsList;
    public List<InterimAdvancePaymentDetailModel> InterimAdvancePaymentsDetails;
    public List<PriceComponentModel> X;
    public BigDecimal MarginalPrice;
    public String MarginalPriceValidity;
    public BigDecimal Pav;
    public BigDecimal ProcurementPrice;
    public BigDecimal ImbalancesPrice;
    public BigDecimal SetMargin;
    public Integer Quantity;
    public String ServicePODs;
    public String ServiceContracts;
    public List<PodModel> PODs;
    public List<PodModel> VersionAddedPODs;
    public List<PodModel> VersionRemovedPODs;

    public ContractDocumentModel from(ContractMainResponse response,
                                      List<ManagerModel> managers,
                                      List<InterimAdvancePaymentDetailModel> interims,
                                      List<PodModel> versionPods,
                                      List<PodModel> versionAddedPods,
                                      List<PodModel> versionRemovedPods,
                                      List<PriceComponentModel> priceComponentModels) {
        this.CustomerNameComb = response.getCustomerNameComb();
        this.CustomerNameCombTrsl = response.getCustomerNameCombTrsl();//todo
        this.CustomerIdentifer = response.getCustomerIdentifier();
        this.CustomerNumber = response.getCustomerNumber();
        this.CustomerType = response.getCustomerType();
        this.HeadquarterAddressComb = response.getHeadquarterAddressComb();
        this.HeadquarterPopulatedPlace = response.getHeadquarterPopulatedPlace();
        this.HeadquarterZip = response.getHeadquarterZip();
        this.HeadquarterDistrict = response.getHeadquarterDistrict();
        this.HeadquarterQuarterRaType = response.getHeadquarterQuarterRaType();
        this.HeadquarterQuarterRaTypeTrsl = response.getHeadquarterQuarterRaTypeTrsl();
        this.HeadquarterQuarterRaName = response.getHeadquarterQuarterRaName();
        this.HeadquarterStrBlvdType = response.getHeadquarterStrBlvdType();
        this.HeadquarterStrBlvdTypeTrsl = response.getHeadquarterStrBlvdTypeTrsl();
        this.HeadquarterStrBlvdName = response.getHeadquarterStrBlvdName();
        this.HeadquarterStrBlvdNumber = response.getHeadquarterStrBlvdNumber();
        this.HeadquarterBlock = response.getHeadquarterBlock();
        this.HeadquarterEntrance = response.getHeadquarterEntrance();
        this.HeadquarterFloor = response.getHeadquarterFloor();
        this.HeadquarterApartment = response.getHeadquarterApartment();
        this.HeadquarterAdditionalInfo = response.getHeadquarterAdditionalInfo();

        this.CommunicationAddressComb = response.getCommunicationAddressComb();
        this.CommunicationPopulatedPlace = response.getCommunicationPopulatedPlace();
        this.CommunicationZip = response.getCommunicationZip();
        this.CommunicationDistrict = response.getCommunicationDistrict();
        this.CommunicationQuarterRaType = response.getCommunicationQuarterRaType();
        this.CommunicationQuarterRaTypeTrsl = response.getCommunicationQuarterRaTypeTrsl();
        this.CommunicationQuarterRaName = response.getCommunicationQuarterRaName();
        this.CommunicationStrBlvdType = response.getCommunicationStrBlvdType();
        this.CommunicationStrBlvdTypeTrsl = response.getCommunicationStrBlvdTypeTrsl();
        this.CommunicationStrBlvdName = response.getCommunicationStrBlvdName();
        this.CommunicationStrBlvdNumber = response.getCommunicationStrBlvdNumber();
        this.CommunicationBlock = response.getCommunicationBlock();
        this.CommunicationEntrance = response.getCommunicationEntrance();
        this.CommunicationFloor = response.getCommunicationFloor();
        this.CommunicationApartment = response.getCommunicationApartment();
        this.CommunicationAdditionalInfo = response.getCommunicationAdditionalInfo();
        this.CommunicationEmailComb = response.getCommunicationEmailComb();
        this.CommunicationMobileComb = response.getCommunicationMobileComb();
        this.CommunicationPhoneComb = response.getCommunicationPhoneComb();
        this.CommunicationEmail = EPBListUtils.convertDBStringArrayIntoListString(response.getCommunicationEmail());
        this.CommunicationMobile = EPBListUtils.convertDBStringArrayIntoListString(response.getCommunicationMobile());
        this.CommunicationPhone = EPBListUtils.convertDBStringArrayIntoListString(response.getCommunicationPhone());

        this.BillingAddressComb = response.getBillingAddressComb();
        this.BillingPopulatedPlace = response.getBillingPopulatedPlace();
        this.BillingZip = response.getBillingZip();
        this.BillingDistrict = response.getBillingDistrict();
        this.BillingQuarterRaType = response.getBillingQuarterRaType();
        this.BillingQuarterRaTypeTrsl = response.getBillingQuarterRaTypeTrsl();
        this.BillingQuarterRaName = response.getBillingQuarterRaName();
        this.BillingStrBlvdType = response.getBillingStrBlvdType();
        this.BillingStrBlvdTypeTrsl = response.getBillingStrBlvdTypeTrsl();
        this.BillingStrBlvdName = response.getBillingStrBlvdName();
        this.BillingStrBlvdNumber = response.getBillingStrBlvdNumber();
        this.BillingBlock = response.getBillingBlock();
        this.BillingEntrance = response.getBillingEntrance();
        this.BillingFloor = response.getBillingFloor();
        this.BillingApartment = response.getBillingApartment();
        this.BillingAdditionalInfo = response.getBillingAdditionalInfo();
        this.BillingEmailComb = response.getBillingEmailComb();
        this.BillingMobileComb = response.getBillingMobileComb();
        this.BillingPhoneComb = response.getBillingPhoneComb();
        this.BillingEmail = EPBListUtils.convertDBStringArrayIntoListString(response.getBillingEmail());
        this.BillingMobile = EPBListUtils.convertDBStringArrayIntoListString(response.getBillingMobile());
        this.BillingPhone = EPBListUtils.convertDBStringArrayIntoListString(response.getBillingPhone());

        this.CustomerSegments = EPBListUtils.convertDBStringArrayIntoListString(response.getCustomerSegments());

        this.ProductName = response.getProductName();
        this.ProductNameTrsl = response.getProductNameTrsl();
        this.ProductPrintName = response.getProductPrintName();
        this.ProductPrintNameTrsl = response.getProductPrintNameTrsl();
        this.TextInvoicesTemplates = response.getTextInvoicesTemplates();
        this.TextInvoicesTemplatesTrsl = response.getTextInvoicesTemplatesTrsl();
        this.ContractType = response.getContractType();
        this.PaymentGuaranteeType = response.getPaymentGuaranteeType();
        this.DepositAmount = response.getDepositAmount();
        this.DepositCurrencyPrintName = response.getDepositCurrencyPrintName();
        this.DepositCurrencyAbr = response.getDepositCurrencyAbr();
        this.DepositCurrencyFullName = response.getDepositCurrencyFullName();
        this.BankGuaranteeAmount = response.getBankGuaranteeAmount();
        this.BankGuaranteeCurrencyPrintName = response.getBankGuaranteeCurrencyPrintName();
        this.BankGuaranteeCurrencyAbr = response.getBankGuaranteeCurrencyAbr();
        this.BankGuaranteeCurrencyFullName = response.getBankGuaranteeCurrencyFullName();
        this.RegistrationSchedules = response.getRegistrationSchedules() != null
                ? response.getRegistrationSchedules().replaceAll("[{}]", "")
                : null;
        this.Forecasting = response.getForecasting() != null
                ? response.getForecasting().replaceAll("[{}]", "")
                : null;
        this.TakingBalancingCosts = response.getTakingBalancingCosts() != null
                ? response.getTakingBalancingCosts().replaceAll("[{}]", "")
                : null;
        this.AdditionalField1 = response.getAdditionalField1();
        this.AdditionalField2 = response.getAdditionalField2();
        this.AdditionalField3 = response.getAdditionalField3();
        this.AdditionalField4 = response.getAdditionalField4();
        this.AdditionalField5 = response.getAdditionalField5();
        this.AdditionalField6 = response.getAdditionalField6();
        this.AdditionalField7 = response.getAdditionalField7();
        this.AdditionalField8 = response.getAdditionalField8();
        this.AdditionalField9 = response.getAdditionalField9();
        this.AdditionalField10 = response.getAdditionalField10();

        this.AdditionalParametersLabel1 = response.getAdditionalParametersLabel1();
        this.AdditionalParametersValue1 = response.getAdditionalParametersValue1();

        this.AdditionalParametersLabel2 = response.getAdditionalParametersLabel2();
        this.AdditionalParametersValue2 = response.getAdditionalParametersValue2();

        this.AdditionalParametersLabel3 = response.getAdditionalParametersLabel3();
        this.AdditionalParametersValue3 = response.getAdditionalParametersValue3();

        this.AdditionalParametersLabel4 = response.getAdditionalParametersLabel4();
        this.AdditionalParametersValue4 = response.getAdditionalParametersValue4();

        this.AdditionalParametersLabel5 = response.getAdditionalParametersLabel5();
        this.AdditionalParametersValue5 = response.getAdditionalParametersValue5();

        this.AdditionalParametersLabel6 = response.getAdditionalParametersLabel6();
        this.AdditionalParametersValue6 = response.getAdditionalParametersValue6();

        this.AdditionalParametersLabel7 = response.getAdditionalParametersLabel7();
        this.AdditionalParametersValue7 = response.getAdditionalParametersValue7();

        this.AdditionalParametersLabel8 = response.getAdditionalParametersLabel8();
        this.AdditionalParametersValue8 = response.getAdditionalParametersValue8();

        this.AdditionalParametersLabel9 = response.getAdditionalParametersLabel9();
        this.AdditionalParametersValue9 = response.getAdditionalParametersValue9();

        this.AdditionalParametersLabel10 = response.getAdditionalParametersLabel10();
        this.AdditionalParametersValue10 = response.getAdditionalParametersValue10();

        this.AdditionalParametersLabel11 = response.getAdditionalParametersLabel11();
        this.AdditionalParametersValue11 = response.getAdditionalParametersValue11();

        this.AdditionalParametersLabel12 = response.getAdditionalParametersLabel12();
        this.AdditionalParametersValue12 = response.getAdditionalParametersValue12();

        this.ContractDocumentType = response.getContractDocumentType();
        this.ContractVersionType = response.getContractVersionType();
        this.ContractNumber = response.getContractNumber();
        this.AdditionalSuffix = response.getAdditionalSuffix();
        this.CreationDate = response.getCreationDate();
        this.SigningDate = response.getSigningDate();
        this.VersionStartDate = response.getVersionStartDate();
        this.EntryForceDate = response.getEntryForceDate();
        this.ContractTermStartDate = response.getContractTermStartDate();
        this.ApplicableInterestRate = response.getApplicableInterestRate();
        this.Campaign = response.getCampaign();
        this.ContractBank = response.getContractBank();
        this.ContractBIC = response.getContractBIC();
        this.ContractIBAN = response.getContractIBAN();
        this.CustomerBank = response.getCustomerBank();
        this.CustomerBIC = response.getCustomerBIC();
        this.CustomerIBAN = response.getCustomerIBAN();
        this.EstimatedContractConsumption = response.getEstimatedContractConsumption();
        this.Employee = response.getEmployee();
        this.AssistingEmployee = convertEmployeeArrayIntoListString(response.getAssistingEmployee());
        this.InternalIntermediary = convertEmployeeArrayIntoListString(response.getInternalIntermediary());
        this.ExternalIntermediary = EPBListUtils.convertDBStringArrayIntoListString(response.getExternalIntermediary());
        this.ContractTermType = response.getContractTermType();
        this.ContractTermValue = response.getContractTermValue();
        this.ContractTermValueType = response.getContractTermValueType();
        this.ContractTermValueTypeTrsl = response.getContractTermValueType();//todo
        this.ContractTermPerpetuity = response.getContractTermPerpetuity();
        this.ContractTermRenewal = response.getContractTermRenewal();
        this.ContractTermRenewalValue = response.getContractTermRenewalValue();
        this.ContractTermRenewalType = response.getContractTermRenewalType();
        this.ContractTermRenewalTypeTrsl = response.getContractTermRenewalType();//todo
        this.PaymentTermType = response.getPaymentTermType();
        this.PaymentTermTypeTrsl = response.getPaymentTermType();//todo
        this.PaymentTermValue = response.getPaymentTermValue();
        this.TermActivationDeliveryValue = response.getTermActivationDeliveryValue();
        this.TermActivationDeliveryType = response.getTermActivationDeliveryType();
        this.TermActivationDeliveryTypeTrsl = response.getTermActivationDeliveryType();//todo
        this.DeadlineEarlyResigningType = response.getDeadlineEarlyResigningType();
        this.DeadlineEarlyResigningTypeTrsl = response.getDeadlineEarlyResigningType();//todo
        this.DeadlineEarlyResigningValue = response.getDeadlineEarlyResigningValue();
        this.GeneralNoticePeriodValue = response.getGeneralNoticePeriodValue();
        this.GeneralNoticePeriodType = response.getGeneralNoticePeriodType();
        this.GeneralNoticePeriodTypeTrsl = response.getGeneralNoticePeriodType();//todo
        this.NoticeTermValue = response.getNoticeTermValue();
        this.NoticeTermType = response.getNoticeTermType();
        this.NoticeTermTypeTrsl = response.getNoticeTermType();//todo
        this.NoticeTermDisconnectionType = response.getNoticeTermDisconnectionType();
        this.NoticeTermDisconnectionTypeTrsl = response.getNoticeTermDisconnectionType();//todo
        this.NoticeTermDisconnectionValue = response.getNoticeTermDisconnectionValue();
        this.SupplyActivationType = response.getSupplyActivationType();
        this.SupplyActivationTypeTrsl = response.getSupplyActivationType();//todo
        this.SupplyActivationValue = response.getSupplyActivationValue();
        this.WaitOldContractToExpire = response.getWaitOldContractToExpire();
        this.EntryIntoForceType = response.getEntryIntoForceType();
        this.EntryIntoForceTypeTrsl = response.getEntryIntoForceType();//todo
        this.EntryIntoForceValue = response.getEntryIntoForceValue();
        this.StartInitialTermType = response.getStartInitialTermType();
        this.StartInitialTermTypeTrsl = response.getStartInitialTermType();//todo
        this.StartInitialTermValue = response.getStartInitialTermValue();
        this.InterimAdvancePaymentsYN = response.getInterimAdvancePaymentsYN();
        this.InterimAdvancePaymentsList = EPBListUtils.convertDBStringArrayIntoListString(response.getInterimAdvancePaymentsList());
        this.MarginalPrice = response.getMarginalPrice();
        this.MarginalPriceValidity = response.getMarginalPriceValidity();
        this.Pav = response.getPav();
        this.ProcurementPrice = response.getProcurementPrice();
        this.ImbalancesPrice = response.getImbalancesPrice();
        this.SetMargin = response.getSetMargin();
        this.Quantity = response.getQuantity();
        this.ServicePODs = response.getServicePODs();
        this.ServiceContracts = response.getServiceContracts();

        this.Managers = managers;

        this.InterimAdvancePaymentsDetails = interims;

        this.PODs = versionPods;
        this.VersionAddedPODs = versionAddedPods;
        this.VersionRemovedPODs = versionRemovedPods;

        this.X = priceComponentModels;

        return this;
    }

    private List<String> convertEmployeeArrayIntoListString(String stringAsArray) {
        if (StringUtils.isEmpty(stringAsArray)) {
            return null;
        }

        return Arrays.stream(stringAsArray.split(";")).toList();
    }
}
