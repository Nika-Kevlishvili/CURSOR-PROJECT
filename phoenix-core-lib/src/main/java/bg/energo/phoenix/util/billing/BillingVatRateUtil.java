package bg.energo.phoenix.util.billing;

import bg.energo.phoenix.model.entity.billing.billingRun.BillingDetailedData;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingSummaryData;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.repository.nomenclature.product.VatRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingVatRateUtil {
    private final VatRateRepository vatRateRepository;

    /**
     * Checks and sets the VAT rate details for a billing detailed data object.
     *
     * @param globalVatRate a flag indicating whether to use the global VAT rate
     * @param vatRateId the ID of the VAT rate to use
     * @param errorMessages a list to store any error messages
     * @param billingDetailedData the billing detailed data object to update
     * @param requestName the name of the request, used for error messages
     */
    public void checkVatRateDetailed(Boolean globalVatRate, Long vatRateId, List<String> errorMessages, BillingDetailedData billingDetailedData, String requestName) {
        if (globalVatRate != null && globalVatRate) {
            if (!vatRateRepository.existsByGlobalVatRateAndStatusIn(true, List.of(NomenclatureItemStatus.ACTIVE))) {
                errorMessages.add(requestName + ".globalVatRate-[globalVatRate] global vat rate not found");
            } else {
                billingDetailedData.setGlobalVatRate(true);
            }
        } else if (vatRateId != null) {
            if (vatRateRepository.existsByIdAndStatusIn(vatRateId, List.of(NomenclatureItemStatus.ACTIVE))) {
                billingDetailedData.setVatRateId(vatRateId);
                billingDetailedData.setGlobalVatRate(false);
            } else {
                errorMessages.add(requestName + ".vatRateId-[vatRateId] vat rate not found");
            }
        } else {
            billingDetailedData.setGlobalVatRate(false);
            billingDetailedData.setVatRateId(null);
        }
    }

    /**
     * Checks and sets the VAT rate details for a billing summary data object.
     *
     * @param globalVatRate a flag indicating whether to use the global VAT rate
     * @param vatRateId the ID of the VAT rate to use
     * @param errorMessages a list to store any error messages
     * @param billingSummaryData the billing summary data object to update
     * @param requestName the name of the request, used for error messages
     */
    public void checkVatRateSummary(Boolean globalVatRate, Long vatRateId, List<String> errorMessages, BillingSummaryData billingSummaryData, String requestName) {
        if (globalVatRate != null && globalVatRate) {
            if (!vatRateRepository.existsByGlobalVatRateAndStatusIn(true, List.of(NomenclatureItemStatus.ACTIVE))) {
                errorMessages.add(requestName + ".globalVatRate-[globalVatRate] global vat rate not found");
            } else {
                billingSummaryData.setGlobalVatRate(true);
            }
        } else if (vatRateId != null) {
            if (vatRateRepository.existsByIdAndStatusIn(vatRateId, List.of(NomenclatureItemStatus.ACTIVE))) {
                billingSummaryData.setVatRateId(vatRateId);
                billingSummaryData.setGlobalVatRate(false);
            } else {
                errorMessages.add(requestName + ".vatRateId-[vatRateId] vat rate not found");
            }
        } else {
            billingSummaryData.setGlobalVatRate(false);
            billingSummaryData.setVatRateId(null);
        }
    }

    /**
     * Checks the VAT rate for a billing run.
     *
     * @param billingRun        the billing run to check the VAT rate for
     * @param globalVatRate     whether to use a global VAT rate
     * @param vatRateId         the ID of the VAT rate to use, if not using a global rate
     * @param errorMessages     a list to store any error messages
     * @param requestName       the name of the request
     */
    public void checkVatRateCommons(BillingRun billingRun, boolean globalVatRate, Long vatRateId, List<String> errorMessages, String requestName) {
        if (globalVatRate) {
            if (!vatRateRepository.existsByGlobalVatRateAndStatusIn(true, List.of(NomenclatureItemStatus.ACTIVE))) {
                errorMessages.add(requestName + ".globalVatRate-[globalVatRate] global vat rate not found");
            } else {
                billingRun.setGlobalVatRate(true);
            }
        } else if (vatRateId != null && !vatRateId.equals(billingRun.getVatRateId())) {
            if (vatRateRepository.existsByIdAndStatusIn(vatRateId, List.of(NomenclatureItemStatus.ACTIVE))) {
                billingRun.setVatRateId(vatRateId);
                billingRun.setGlobalVatRate(false);
            } else {
                errorMessages.add(requestName + ".vatRateId-[vatRateId] vat rate not found");
            }
        } else {
            billingRun.setGlobalVatRate(false);
            billingRun.setVatRateId(null);
        }
    }
}
