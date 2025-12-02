package bg.energo.phoenix.model.response.billing.billingRun;

import bg.energo.phoenix.model.enums.billing.billings.*;
import bg.energo.phoenix.model.enums.billing.processPeriodicity.ProcessPeriodicityBillingProcessStart;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.DeductionFrom;
import bg.energo.phoenix.model.enums.product.iap.interimAdvancePayment.IssuingForTheMonthToCurrent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;


public interface OneTimeCreationModel {

    Long getPeriodicityId();

    ProcessPeriodicityBillingProcessStart getProcessExecutionType();

    LocalDateTime getProcessExecutionDate();

    Long getId();

    String getBillingNumber();

    BillingRunPeriodicity getRunPeriodicity();

    String getAdditionalInfo();

    BillingType getType();

    BillingStatus getStatus();

    LocalDate getTaxEventDate();

    LocalDate getInvoiceDate();

    Long getAccountingPeriodId();

    InvoiceDueDateType getInvoiceDueDateType();

    LocalDate getInvoiceDueDate();

    /*List<ApplicationModelType>*/String getApplicationModelType();

    SendingAnInvoice getSendingAnInvoice();

    ExecutionType getExecutionType();

    LocalDateTime getExecutionDate();

    BillingCriteria getBillingCriteria();

    BillingApplicationLevel getApplicationLevel();

    String getCustomerContractOrPodConditions();

    String getCustomerContractOrPodList();

   /* List<RunStage>*/String getRunStages();

    Long getVatRateId();

    Boolean getGlobalVatRate();

    Long getInterestRateId();

    Long getBankId();

    String getIban();

    BigDecimal getAmountExcludingVat();

    IssuingForTheMonthToCurrent getIssuingForTheMonthToCurrent();

    /*List<IssuedSeparateInvoice>*/ String getIssuedSeparateInvoices();

    Long getCurrencyId();

    DeductionFrom getDeductionFrom();

    String getNumberOfIncomeAccount();

    String getCostCenterControllingOrder();

    Long getCustomerDetailId();

    Long getProductContractId();

    Long getServiceContractId();

    DocumentType getDocumentType();

    Long getGoodsOrderId();

    Long getServiceOrderId();

    String getBasisForIssuing();

    ManualInvoiceType getManualInvoiceType();

    Boolean getDirectDebit();

    Long getCustomerCommunicationId();

    String getListOfInvoices();

    Boolean getPriceChange();

    Long getPeriodicityCreatedFromId();

    Long getEmployeeId();

    LocalDateTime getPeriodicityAddDate();

    Integer getMaxEndDateValue();

    BillingEndDate getMaxEndDate();

    OffsetTime getStartTime();
}
