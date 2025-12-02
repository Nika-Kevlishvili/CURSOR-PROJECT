package bg.energo.phoenix.model.documentModels.latePaymentFine;

import bg.energo.phoenix.model.enums.receivable.latePaymentFine.LatePaymentFineType;
import bg.energo.phoenix.service.billing.model.impl.CompanyDetailedInformationModelImpl;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class LatePaymentFineDocumentModel extends CompanyDetailedInformationModelImpl {

    @JsonProperty("BillingGroup")
    public String BillingGroup;

    @JsonProperty("CurrencyAbr")
    public String CurrencyAbr;

    @JsonProperty("CurrencyFullName")
    public String CurrencyFullName;

    @JsonProperty("CurrencyPrintName")
    public String CurrencyPrintName;

    @JsonProperty("CustomerAdditionalInfo")
    public String CustomerAdditionalInfo;

    @JsonProperty("CustomerAddressComb")
    public String CustomerAddressComb;

    @JsonProperty("CustomerApartment")
    public String CustomerApartment;

    @JsonProperty("CustomerBlock")
    public String CustomerBlock;

    @JsonProperty("CustomerDistrict")
    public String CustomerDistrict;

    @JsonProperty("CustomerEntrance")
    public String CustomerEntrance;

    @JsonProperty("CustomerFloor")
    public String CustomerFloor;

    @JsonProperty("CustomerIdentifier")
    public String CustomerIdentifier;

    @JsonProperty("CustomerNameComb")
    public String CustomerNameComb;

    @JsonProperty("CustomerNameCombTrsl")
    public String CustomerNameCombTrsl;

    @JsonProperty("CustomerNumber")
    public String CustomerNumber;

    @JsonProperty("CustomerPopulatedPlace")
    public String CustomerPopulatedPlace;

    @JsonProperty("CustomerQuarterRaName")
    public String CustomerQuarterRaName;

    @JsonProperty("CustomerQuarterRaType")
    public String CustomerQuarterRaType;

    @JsonProperty("CustomerSegments")
    public List<String> CustomerSegments;

    @JsonProperty("CustomerStrBlvdName")
    public String CustomerStrBlvdName;

    @JsonProperty("CustomerStrBlvdNumber")
    public String CustomerStrBlvdNumber;

    @JsonProperty("CustomerStrBlvdType")
    public String CustomerStrBlvdType;

    @JsonProperty("CustomerVat")
    public String CustomerVat;

    @JsonProperty("CustomerZip")
    public String CustomerZip;

    @JsonProperty("DocumentDate")
    public LocalDateTime DocumentDate;

    @JsonProperty("DocumentNumber")
    public String DocumentNumber;

    @JsonProperty("DocumentPrefix")
    public String DocumentPrefix;

    @JsonProperty("DocumentType")
    public LatePaymentFineType DocumentType;

    @JsonProperty("DueDate")
    public LocalDate DueDate;

    @JsonProperty("FullPaymentDate")
    public LocalDate FullPaymentDate;

    @JsonProperty("LiabilityInitialAmount")
    public String LiabilityInitialAmount;

    @JsonProperty("OtherCurrencyAbr")
    public String OtherCurrencyAbr;

    @JsonProperty("OtherCurrencyFullName")
    public String OtherCurrencyFullName;

    @JsonProperty("OtherCurrencyPrintName")
    public String OtherCurrencyPrintName;

    @JsonProperty("OverdueDocumentDate")
    public LocalDate OverdueDocumentDate;

    @JsonProperty("OverdueDocumentNumber")
    public String OverdueDocumentNumber;

    @JsonProperty("OverdueDocumentPrefix")
    public String OverdueDocumentPrefix;

    @JsonProperty("OverdueDocumentType")
    public String OverdueDocumentType;

    @JsonProperty("ReversedDocumentDate")
    public LocalDateTime ReversedDocumentDate;

    @JsonProperty("ReversedDocumentNumber")
    public String ReversedDocumentNumber;

    @JsonProperty("ReversedDocumentPrefix")
    public String ReversedDocumentPrefix;

    @JsonProperty("ReversedDocumentType")
    public LatePaymentFineType ReversedDocumentType;

    @JsonProperty("TotalAmount")
    public BigDecimal TotalAmount;

    @JsonProperty("TotalAmountOtherCurrency")
    public BigDecimal TotalAmountOtherCurrency;

    @JsonProperty("Interests")
    public List<LatePaymentFineInterestsResponse> Interests;

}
