package bg.energo.phoenix.model.documentModels;

import bg.energo.phoenix.model.entity.receivable.rescheduling.Rescheduling;
import bg.energo.phoenix.model.response.receivable.rescheduling.ReschedulingAddressResponse;
import bg.energo.phoenix.service.billing.model.impl.CompanyDetailedInformationModelImpl;
import bg.energo.phoenix.util.epb.EPBDecimalUtils;
import bg.energo.phoenix.util.transliteration.BulgarianTransliterationUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
@Slf4j
public class ReschedulingDocumentModel extends CompanyDetailedInformationModelImpl {
    public String CustomerNameComb;
    public String CustomerNameCombTrsl;
    public String CustomerIdentifier;
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
    public String CustomerEmail;
    public List<String> CustomerSegments;
    public List<ReschedulingManagerModel> Managers;
    public String Number;
    public LocalDateTime CreationDate;
    public BigDecimal TotalInstallmentsAmount;
    public BigDecimal TotalInstallmentsPrinciple;
    public BigDecimal TotalInstallmentsInterests;
    public String TotalInstallmentsAmountWithWords;
    public String TotalInstallmentsPrincipleWithWords;
    public String TotalInstallmentsInterestsWithWords;
    public String CurrencyPrintName;
    public String CurrencyAbr;
    public String CurrencyFullName;
    public String Contracts;
    public String LiabilitiesPeriodFrom;
    public String LiabilitiesPeriodTo;
    public List<ReschedulingInstallmentModel> Installments;
    public List<ReschedulingLiabilityModel> Liabilities;

    public void from(
            ReschedulingAddressResponse response,
            List<ReschedulingManagerModel> managers,
            Rescheduling rescheduling,
            List<ReschedulingInstallmentModel> installments,
            List<ReschedulingLiabilityModel> liabilities,
            String customerEmail,
            String contracts
    ) {
        this.CustomerNameComb = response.getCustomerNameComb();
        this.CustomerNameCombTrsl = response.getCustomerNameCombTrsl();
        this.CustomerIdentifier = response.getCustomerIdentifier();
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
        this.CustomerEmail = customerEmail;
        this.CustomerSegments = response.getCustomerSegments() != null ?
                Arrays.stream(response.getCustomerSegments().split(","))
                        .map(String::trim)
                        .toList() :
                Collections.emptyList();
        this.Number = rescheduling.getReschedulingNumber();
        this.CreationDate = rescheduling.getCreateDate();
        this.TotalInstallmentsAmount = response.getTotalInstallmentsAmount();
        this.TotalInstallmentsPrinciple = response.getTotalInstallmentsPrinciple();
        this.TotalInstallmentsInterests = response.getTotalInstallmentsInterests();
        if(response.getTotalInstallmentsAmount()!=null) {
            try {
                BigDecimal totalWithScale2 = EPBDecimalUtils.roundToTwoDecimalPlaces(response.getTotalInstallmentsAmount());
                BigDecimal integerAmount = totalWithScale2.setScale(0, RoundingMode.FLOOR);
                BigDecimal fractionAmount = totalWithScale2.subtract(totalWithScale2.setScale(0, RoundingMode.FLOOR)).movePointRight(totalWithScale2.scale());

                this.TotalInstallmentsAmountWithWords = BulgarianTransliterationUtil.convertAmountToWords(integerAmount.toBigInteger().intValue(), fractionAmount.intValue());
            } catch (Exception e) {
                log.error("Cannot transliterate amount in words", e);
            }
        }
        if(response.getTotalInstallmentsPrinciple()!=null) {
            try {
                BigDecimal principleScale2 = EPBDecimalUtils.roundToTwoDecimalPlaces(response.getTotalInstallmentsPrinciple());
                BigDecimal integerAmount = principleScale2.setScale(0, RoundingMode.FLOOR);
                BigDecimal fractionAmount = principleScale2.subtract(principleScale2.setScale(0, RoundingMode.FLOOR)).movePointRight(principleScale2.scale());
                this.TotalInstallmentsPrincipleWithWords = BulgarianTransliterationUtil.convertAmountToWords(integerAmount.toBigInteger().intValue(), fractionAmount.intValue());
            }
                catch(Exception e) {
                    log.error("Cannot transliterate amount in words", e);
                }
        }
        if (response.getTotalInstallmentsInterests() != null) {
            try {
                BigDecimal interestScale2 = EPBDecimalUtils.roundToTwoDecimalPlaces(response.getTotalInstallmentsInterests());
                BigDecimal integerAmount = interestScale2.setScale(0, RoundingMode.FLOOR);
                BigDecimal fractionAmount = interestScale2.subtract(interestScale2.setScale(0, RoundingMode.FLOOR)).movePointRight(interestScale2.scale());
                this.TotalInstallmentsInterestsWithWords = BulgarianTransliterationUtil.convertAmountToWords(integerAmount.toBigInteger().intValue(), fractionAmount.intValue());
            } catch (Exception e) {
                log.error("Cannot transliterate amount in words", e);
            }
        }
        this.CurrencyPrintName = response.getCurrencyPrintName();
        this.CurrencyAbr = response.getCurrencyAbr();
        this.CurrencyFullName = response.getCurrencyFullName();
        this.Contracts = contracts;
        this.LiabilitiesPeriodFrom = response.getLiabilitiesPeriodFrom() != null ? response.getLiabilitiesPeriodFrom().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "";
        this.LiabilitiesPeriodTo = response.getLiabilitiesPeriodTo() != null ? response.getLiabilitiesPeriodTo().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) : "";
        this.Managers = managers;
        this.Installments = installments;
        this.Liabilities = liabilities;
    }
}
