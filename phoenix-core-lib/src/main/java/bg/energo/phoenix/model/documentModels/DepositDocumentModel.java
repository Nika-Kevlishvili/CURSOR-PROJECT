package bg.energo.phoenix.model.documentModels;

import bg.energo.phoenix.service.billing.model.impl.CompanyDetailedInformationModelImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DepositDocumentModel extends CompanyDetailedInformationModelImpl {
    public LocalDateTime SystemDate;
    public LocalDateTime LiabilityCreationDate;
    public LocalDate LiabilityDueDate;
    public BigDecimal LiabilityInitialAmount;
    public String LiabilityInitialAmountWithWords;
    public BigDecimal LiabilityCurrentAmount;
    public BigDecimal LiabilityInitialAmountOtherCurrency;
    public BigDecimal LiabilityCurrentAmountOtherCurrency;
    public String LiabilityCurrency;
    public BigDecimal InitialAmount;
    public String InitialAmountWithWords;
    public BigDecimal InitialAmountOtherCurrency;
    public String Currency;
    public String CustomerIdentifier;
    public String CustomerNameComb;
    public String CustomerNameCombTrsl;
    public String CustomerNumber;
    public String AddressHeadquarterComb;
    public String AddressHeadquarterPopulatedPlace;
    public String AddressHeadquarterZIP;
    public List<ManagerDocumentModel> Managers;
    public String DepositContractNumber;
    public LocalDateTime DepositContractDate;

    public void fillDepositData(
            LocalDateTime systemDate,
            LocalDateTime liabilityCreationDate,
            LocalDate liabilityDueDate,
            BigDecimal liabilityInitialAmount,
            BigDecimal liabilityCurrentAmount,
            BigDecimal liabilityInitialAmountOtherCurrency,
            BigDecimal liabilityCurrentAmountOtherCurrency,
            String liabilityCurrency,
            BigDecimal initialAmount,
            BigDecimal initialAmountOtherCurrency,
            String currency,
            String customerIdentifier,
            String customerNameComb,
            String customerNameCombTrsl,
            String customerNumber,
            String addressHeadquarterComb,
            String addressHeadquarterPopulatedPlace,
            String addressHeadquarterZIP,
            List<ManagerDocumentModel> managers,
            String depositContractNumber,
            LocalDateTime depositContractDate,
            String initialAmountWithWords,
            String liabilityInitialAmountWithWords
    ) {
        this.SystemDate = systemDate;
        this.LiabilityCreationDate = liabilityCreationDate;
        this.LiabilityDueDate = liabilityDueDate;
        this.LiabilityInitialAmount = liabilityInitialAmount;
        this.LiabilityCurrentAmount = liabilityCurrentAmount;
        this.LiabilityInitialAmountOtherCurrency = liabilityInitialAmountOtherCurrency;
        this.LiabilityCurrentAmountOtherCurrency = liabilityCurrentAmountOtherCurrency;
        this.LiabilityCurrency = liabilityCurrency;
        this.InitialAmount = initialAmount;
        this.InitialAmountOtherCurrency = initialAmountOtherCurrency;
        this.Currency = currency;
        this.CustomerIdentifier = customerIdentifier;
        this.CustomerNameComb = customerNameComb;
        this.CustomerNameCombTrsl = customerNameCombTrsl;
        this.CustomerNumber = customerNumber;
        this.AddressHeadquarterComb = addressHeadquarterComb;
        this.AddressHeadquarterPopulatedPlace = addressHeadquarterPopulatedPlace;
        this.AddressHeadquarterZIP = addressHeadquarterZIP;
        this.Managers = managers;
        this.DepositContractNumber = depositContractNumber;
        this.DepositContractDate = depositContractDate;
        this.InitialAmountWithWords = initialAmountWithWords;
        this.LiabilityInitialAmountWithWords = liabilityInitialAmountWithWords;
    }
}
