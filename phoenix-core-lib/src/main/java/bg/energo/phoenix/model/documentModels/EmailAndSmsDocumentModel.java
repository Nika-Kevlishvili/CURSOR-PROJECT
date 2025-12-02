package bg.energo.phoenix.model.documentModels;

import bg.energo.phoenix.service.billing.model.impl.CompanyDetailedInformationModelImpl;
import bg.energo.phoenix.util.epb.EPBJsonUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class EmailAndSmsDocumentModel extends CompanyDetailedInformationModelImpl {
    @JsonProperty("companyLogoList")
    public List<String> CompanyLogoList;

    @JsonProperty("creatorUserName")
    public String CreatorUsername;

    @JsonProperty("senderUserName")
    public String SenderUsername;

    @JsonProperty("archivingNumber")
    public String ArchivingNumber;

    @JsonProperty("systemDate")
    public LocalDate SystemDate;

    @JsonProperty("customerIdentifier")
    public String CustomerIdentifier;

    @JsonProperty("customerNameComb")
    public String CustomerNameComb;

    @JsonProperty("customerNumber")
    public String CustomerNumber;

    @JsonProperty("addressHeadquarterComb")
    public String AddressHeadquarterComb;

    @JsonProperty("addressHeadquarterPopulatedPlace")
    public String AddressHeadquarterPopulatedPlace;

    @JsonProperty("addressHeadquarterZIP")
    public String AddressHeadquarterZIP;

    @JsonProperty("addressComb")
    public String AddressComb;

    @JsonProperty("addressPopulatedPlace")
    public String AddressPopulatedPlace;

    @JsonProperty("addressZIP")
    public String AddressZIP;

    @JsonProperty("managers")
    public List<Manager> Managers;

    @JsonProperty("liabilities")
    public List<Liability> Liabilities;

    @JsonProperty("sumLiabilities")
    public BigDecimal SumLiabilities;

    @JsonProperty("sumOverdueLiabilities")
    public BigDecimal SumOverdueLiabilities;

    @JsonProperty("contractNumber")
    public String ContractNumber;

    @JsonProperty("contractDate")
    public LocalDate ContractDate;

    @JsonProperty("contractProductName")
    public String ContractProductName;

    @JsonProperty("contractServiceName")
    public String ContractServiceName;

    @JsonProperty("contractStatus")
    public String ContractStatus;

    @JsonProperty("contractActivationDate")
    public LocalDate ContractActivationDate;

    @JsonProperty("contractTerminationDate")
    public LocalDate ContractTerminationDate;

    @JsonProperty("contractTermEndDate")
    public LocalDate ContractTermEndDate;

    @JsonProperty("contractTermEndDatePlus1")
    public LocalDate ContractTermEndDatePlus1;

    @JsonProperty("contractPaymentGuaranteeType")
    public String ContractPaymentGuaranteeType;

    @JsonProperty("contractPaymentGuaranteeBankAmount")
    public BigDecimal ContractPaymentGuaranteeBankAmount;

    @JsonProperty("contractPaymentGuaranteeBankCurrency")
    public String ContractPaymentGuaranteeBankCurrency;

    @JsonProperty("contractPaymentGuaranteeCashDepositAmount")
    public BigDecimal ContractPaymentGuaranteeCashDepositAmount;

    @JsonProperty("contractPaymentGuaranteeCashDepositCurrency")
    public String ContractPaymentGuaranteeCashDepositCurrency;

    @JsonProperty("contractTermForActivation")
    public String ContractTermForActivation;

    @JsonProperty("contractVersionActivationDatePriceChange")
    public LocalDate ContractVersionActivationDatePriceChange;

    @JsonProperty("contractVersionActivationDatePriceChangeMinus30")
    public LocalDate ContractVersionActivationDatePriceChangeMinus30;

    @JsonProperty("contractVersionActivationDatePriceChangeMinus45")
    public LocalDate ContractVersionActivationDatePriceChangeMinus45;

    @JsonProperty("contractPODs")
    public List<POD> ContractPODs;

    @JsonProperty("x")
    public List<XField> X;

    @JsonProperty("contractLatestActionExecutionDate")
    public LocalDate ContractLatestActionExecutionDate;

    @JsonProperty("contractLatestActionTerminationClauseNumber")
    public String ContractLatestActionTerminationClauseNumber;

    @JsonProperty("contractLatestActionPenaltyClauseNumber")
    public String ContractLatestActionPenaltyClauseNumber;

    @JsonProperty("contractLatestActionPenaltyClaimAmount")
    public BigDecimal ContractLatestActionPenaltyClaimAmount;

    @JsonProperty("contractLatestActionPenaltyClaimCurrency")
    public String ContractLatestActionPenaltyClaimCurrency;

    @JsonProperty("contractLatestActionPenaltyPaymentDueDate")
    public LocalDate ContractLatestActionPenaltyPaymentDueDate;

    @JsonProperty("contractLatestActionTerminationNoticePeriod")
    public String ContractLatestActionTerminationNoticePeriod;

    @JsonProperty("contractLatestActionTerminationNoticePeriodType")
    public String ContractLatestActionTerminationNoticePeriodType;

    @JsonProperty("actionTerminationGOPODs")
    public List<ActionTerminationGO> ActionTerminationGOPODs;

    @JsonProperty("actionTerminationGOListPOD")
    public List<ActionTerminationGOListPOD> ActionTerminationGOListPOD;

    // Manager class with annotations
    public static class Manager {

        @JsonProperty("title")
        public String Title;

        @JsonProperty("name")
        public String Name;

        @JsonProperty("surname")
        public String Surname;

        @JsonProperty("jobPosition")
        public String JobPosition;
    }

    // Liability class with annotations
    public static class Liability {

        @JsonProperty("invoiceNumber")
        public String InvoiceNumber;

        @JsonProperty("invoiceDate")
        public String InvoiceDate;

        @JsonProperty("dueDate")
        public String DueDate;

        @JsonProperty("initialAmount")
        public String InitialAmount;

        @JsonProperty("currentAmount")
        public String CurrentAmount;

        @JsonProperty("invoicePeriodFrom")
        public String InvoicePeriodFrom;

        @JsonProperty("invoicePeriodTo")
        public String InvoicePeriodTo;
    }

    // POD class with annotations
    public static class POD {

        @JsonProperty("id")
        public String ID;

        @JsonProperty("additionalID")
        public String AdditionalID;

        @JsonProperty("name")
        public String Name;

        @JsonProperty("addressComb")
        public String AddressComb;

        @JsonProperty("place")
        public String Place;

        @JsonProperty("zip")
        public String ZIP;

        @JsonProperty("type")
        public String Type;

        @JsonProperty("providedPower")
        public String ProvidedPower;

        @JsonProperty("measurementType")
        public String MeasurementType;
    }

    // XField class with annotations
    public static class XField {

        @JsonProperty("tag")
        public String TAG;

        @JsonProperty("x1Desc")
        public String X1Desc;

        @JsonProperty("x1Value")
        public String X1Value;

        @JsonProperty("x2Desc")
        public String X2Desc;

        @JsonProperty("x2Value")
        public String X2Value;

        @JsonProperty("x3Desc")
        public String X3Desc;

        @JsonProperty("x3Value")
        public String X3Value;

        @JsonProperty("x4Desc")
        public String X4Desc;

        @JsonProperty("x4Value")
        public String X4Value;

        @JsonProperty("x5Desc")
        public String X5Desc;

        @JsonProperty("x5Value")
        public String X5Value;

        @JsonProperty("x6Desc")
        public String X6Desc;

        @JsonProperty("x6Value")
        public String X6Value;

        @JsonProperty("x7Desc")
        public String X7Desc;

        @JsonProperty("x7Value")
        public String X7Value;

        @JsonProperty("x8Desc")
        public String X8Desc;

        @JsonProperty("x8Value")
        public String X8Value;
    }

    // ActionTerminationGO class with annotations
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ActionTerminationGO {
        @JsonProperty("go")
        public String GO;

        @JsonProperty("id")
        public String ID;

        @JsonProperty("additionalId")
        public String AdditionalID;

        @JsonProperty("executionDate")
        public String ExecutionDate;
    }

    // ActionTerminationGOListPOD class with annotations
    public static class ActionTerminationGOListPOD {

        public ActionTerminationGOListPOD(ActionTerminationGOListPODProjection projection) {
            this.GO = projection.getGridName();
            this.ListPOD = projection.getPodNames();
            this.ExecutionDate = projection.getExecutionDate();
        }

        @JsonProperty("go")
        public String GO;

        @JsonProperty("listPOD")
        public String ListPOD;

        @JsonProperty("executionDate")
        public LocalDate ExecutionDate;
    }

    public interface CustomerAdditionalInfoProjection {
        String getCreatorUserName();

        String getSenderUserName();

        String getArchivingNumber();

        LocalDate getSystemDate();

        String getCustomerIdentifier();

        String getCustomerNameComb();

        String getCustomerNumber();

        String getAddressHeadquarterComb();

        String getAddressHeadquarterPopulatedPlace();

        String getAddressHeadquarterZIP();

        String getAddressComb();

        String getAddressPopulatedPlace();

        String getAddressZIP();

        Long getCustomerId();

        Long getCustomerDetailId();
    }

    public interface ContractsInfoProjection {
        String getContractNumber();

        LocalDate getContractDate();

        String getContractProductName();

        String getContractServiceName();

        String getContractStatus();

        LocalDate getContractActivationDate();

        LocalDate getContractTerminationDate();

        LocalDate getContractTerminationEndDate();

        LocalDate getContractTerminationEndDate1();

        String getContractPaymentGuaranteeType();

        BigDecimal getContractPaymentGuaranteeBankAmount();

        String getContractPaymentGuaranteeBankCurrency();

        BigDecimal getContractPaymentGuaranteeCashDepositAmount();

        String getContractPaymentGuaranteeCashDepositCurrency();

        String getContractTermForActivation();

        LocalDate getContractVersionActivationDatePriceChange();

        LocalDate getContractVersionActivationDatePriceChange30();

        LocalDate getContractVersionActivationDatePriceChange45();
    }

    public interface ContractsActionsProjection {
        Long getActionId();

        LocalDate getLastExecutionDate();

        String getTerminationContractClauseNumber();

        String getPenaltyContractClauseNumber();

        BigDecimal getPenaltyClaimAmount();

        String getContractTerminationDate();

        String getPenaltyClaimCurrency();

        LocalDate getPenaltyPaymentDueDate();

        Integer getTerminationNoticePeriodMin();

        Integer getTerminationNoticePeriodMax();

        String getTerminationPeriodType();

    }

    public interface ActionTerminationGOListPODProjection {
        Long getGridId();

        String getPodNames();

        String getGridName();

        LocalDate getExecutionDate();
    }

    public void fillWithContractInfo(ContractsInfoProjection projection) {
        this.ContractNumber = projection.getContractNumber();
        this.ContractDate = projection.getContractDate();
        this.ContractProductName = projection.getContractProductName();
        this.ContractServiceName = projection.getContractServiceName();
        this.ContractStatus = projection.getContractStatus();
        this.ContractActivationDate = projection.getContractActivationDate();
        this.ContractTerminationDate = projection.getContractTerminationDate();
        this.ContractTermEndDate = projection.getContractTerminationEndDate();
        this.ContractTermEndDatePlus1 = projection.getContractTerminationEndDate1();
        this.ContractPaymentGuaranteeType = projection.getContractPaymentGuaranteeType();
        this.ContractPaymentGuaranteeBankAmount = projection.getContractPaymentGuaranteeBankAmount();
        this.ContractPaymentGuaranteeBankCurrency = projection.getContractPaymentGuaranteeBankCurrency();
        this.ContractPaymentGuaranteeCashDepositAmount = projection.getContractPaymentGuaranteeCashDepositAmount();
        this.ContractPaymentGuaranteeCashDepositCurrency = projection.getContractPaymentGuaranteeCashDepositCurrency();
        this.ContractTermForActivation = projection.getContractTermForActivation();
        this.ContractVersionActivationDatePriceChange = projection.getContractVersionActivationDatePriceChange();
        this.ContractVersionActivationDatePriceChangeMinus30 = projection.getContractVersionActivationDatePriceChange30();
        this.ContractVersionActivationDatePriceChangeMinus45 = projection.getContractVersionActivationDatePriceChange45();
    }

    public void fillWithCustomerAdditionalInfo(CustomerAdditionalInfoProjection projection) {
        this.SystemDate = projection.getSystemDate();
        this.AddressHeadquarterPopulatedPlace = projection.getAddressHeadquarterPopulatedPlace();
        this.AddressHeadquarterComb = projection.getAddressHeadquarterComb();
        this.AddressHeadquarterZIP = projection.getAddressHeadquarterZIP();
        this.AddressPopulatedPlace = projection.getAddressPopulatedPlace();
        this.CustomerIdentifier = projection.getCustomerIdentifier();
        this.CustomerNameComb = projection.getCustomerNameComb();
        this.ArchivingNumber = projection.getArchivingNumber();
        this.CreatorUsername = projection.getCreatorUserName();
        this.SenderUsername = projection.getSenderUserName();
        this.CustomerNumber = projection.getCustomerNumber();
        this.AddressComb = projection.getAddressComb();
        this.AddressZIP = projection.getAddressZIP();
    }

    public void fillWithContractActionsInfo(ContractsActionsProjection projection) {
        this.ContractLatestActionExecutionDate = projection.getLastExecutionDate();
        this.ContractLatestActionTerminationClauseNumber = projection.getTerminationContractClauseNumber();
        this.ContractLatestActionPenaltyClauseNumber = projection.getPenaltyContractClauseNumber();
        this.ContractLatestActionPenaltyClaimAmount = projection.getPenaltyClaimAmount();
        this.ContractLatestActionPenaltyClaimCurrency = projection.getPenaltyClaimCurrency();
        this.ContractLatestActionPenaltyPaymentDueDate = projection.getPenaltyPaymentDueDate();
        Integer terminationNoticePeriodMin = projection.getTerminationNoticePeriodMin();
        Integer terminationNoticePeriodMax = projection.getTerminationNoticePeriodMax();
        if (Objects.nonNull(terminationNoticePeriodMin)) {
            if (Objects.nonNull(terminationNoticePeriodMax)) {
                this.ContractLatestActionTerminationNoticePeriod = String.format("%s-%s", terminationNoticePeriodMin, terminationNoticePeriodMax);
            } else {
                this.ContractLatestActionTerminationNoticePeriod = String.format("%s-%s", terminationNoticePeriodMin, "");
            }
        }
        this.ContractLatestActionTerminationNoticePeriodType = projection.getTerminationPeriodType();
    }

    public void fillWithLiabilityData(Map<String, Object> liabilityDataMap) {
        BigDecimal sumOfOverdueLiabilities = (BigDecimal) liabilityDataMap.get("SumOverdueLiabilities");
        BigDecimal sumOfLiabilities = (BigDecimal) liabilityDataMap.get("SumLiabilities");
        String liabilitiesJsonString = (String) liabilityDataMap.get("Liabilities");

        if (StringUtils.isNotBlank(liabilitiesJsonString)) {
            this.Liabilities = EPBJsonUtils.readList(liabilitiesJsonString, EmailAndSmsDocumentModel.Liability.class);
        }

        if (Objects.nonNull(sumOfLiabilities)) {
            this.SumLiabilities = sumOfLiabilities;
        }

        if (Objects.nonNull(sumOfOverdueLiabilities)) {
            this.SumOverdueLiabilities = sumOfOverdueLiabilities;
        }
    }

    // A simple class for deserializing JSON
    public static class XFieldVariable {
        public String tag;
        public String variableName;
        public String description;
        public String value;
    }

}