package bg.energo.phoenix.model.documentModels.mlo;

import bg.energo.phoenix.model.response.receivable.manualLiabilityOffsetting.MloCustomerMiddleResponse;
import bg.energo.phoenix.service.billing.model.impl.CompanyDetailedInformationModelImpl;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
public class MloDocumentModel extends CompanyDetailedInformationModelImpl {
    public String CustomerNameComb;
    public String CustomerNameCombTrsl;
    public String CustomerIdentifer;
    public String CustomerNumber;
    public String CustomerAddressComb;
    public String CustomerPopulatedPlace;
    public String CustomerZip;
    public String CustomerDistrict;
    public String CustomerQuarterRaType;
    public String CustomerQuarterRaName;
    public String CustomerStrBlvdType;
    public String CustomerStrBlvdName;
    public String CustomerStrBlvdNumber;
    public String CustomerBlock;
    public String CustomerEntrance;
    public String CustomerFloor;
    public String CustomerApartment;
    public String CustomerAdditionalInfo;
    public List<String> CustomerSegments;
    public List<Manager> Managers;
    public LocalDate OffsettingDate;
    public BigDecimal LiabilitiesAmountBefore;
    public BigDecimal LiabilitiesAmountAfter;
    public BigDecimal ReceivablesAmountBefore;
    public BigDecimal ReceivablesAmountAfter;
    public String CurrencyPrintName;
    public String CurrencyAbr;
    public String CurrencyFullName;
    public List<ReceivableOrLiability> Liabilities;
    public List<ReceivableOrLiability> Receivables;

    public void from(MloCustomerMiddleResponse response,List<Manager> managers,List<ReceivableOrLiability> liabilities, List<ReceivableOrLiability> receivables) {
        this.CustomerNameComb = response.getCustomerNameComb();
        this.CustomerNameCombTrsl = response.getCustomerNameCombTrsl();
        this.CustomerIdentifer = response.getCustomerIdentifier();
        this.CustomerNumber = response.getCustomerNumber();
        this.CustomerAddressComb = response.getCustomerAddressComb();
        this.CustomerPopulatedPlace = response.getCustomerPopulatedPlace();
        this.CustomerZip = response.getCustomerZip();
        this.CustomerDistrict = response.getCustomerDistrict();
        this.CustomerQuarterRaType = response.getCustomerQuarterRaType();
        this.CustomerQuarterRaName = response.getCustomerQuarterRaName();
        this.CustomerStrBlvdType = response.getCustomerStrBlvdType();
        this.CustomerStrBlvdName = response.getCustomerStrBlvdName();
        this.CustomerStrBlvdNumber = response.getCustomerStrBlvdNumber();
        this.CustomerBlock = response.getCustomerBlock();
        this.CustomerEntrance = response.getCustomerEntrance();
        this.CustomerFloor = response.getCustomerFloor();
        this.CustomerApartment = response.getCustomerApartment();
        this.CustomerAdditionalInfo = response.getCustomerAdditionalInfo();
        this.CustomerSegments = response.getCustomerSegments() != null ?
                Arrays.stream(response.getCustomerSegments().split(","))
                        .map(String::trim)
                        .toList() :
                Collections.emptyList();
        this.OffsettingDate = response.getOffsettingDate();
        this.LiabilitiesAmountBefore = response.getLiabilitiesAmountBefore();
        this.LiabilitiesAmountAfter = response.getLiabilitiesAmountAfter();
        this.ReceivablesAmountBefore = response.getReceivablesAmountBefore();
        this.ReceivablesAmountAfter = response.getReceivablesAmountAfter();
        this.CurrencyPrintName= response.getCurrencyPrintName();
        this.CurrencyAbr = response.getCurrencyAbr();
        this.CurrencyFullName = response.getCurrencyFullName();
        this.Managers = managers;
        this.Liabilities = liabilities;
        this.Receivables = receivables;
    }
}
