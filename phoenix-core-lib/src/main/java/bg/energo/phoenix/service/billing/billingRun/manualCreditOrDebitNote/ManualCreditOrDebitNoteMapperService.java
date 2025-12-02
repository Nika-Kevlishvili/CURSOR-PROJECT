package bg.energo.phoenix.service.billing.billingRun.manualCreditOrDebitNote;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.billing.billingRun.manualCreditOrDebitNote.BillingRunInvoiceResponse;
import bg.energo.phoenix.model.response.billing.billingRun.manualCreditOrDebitNote.ManualCreditOrDebitNoteBasicDataParametersResponse;
import bg.energo.phoenix.model.response.contract.InterestRate.InterestRateShortResponse;
import bg.energo.phoenix.model.response.nomenclature.customer.BankResponse;
import bg.energo.phoenix.model.response.nomenclature.product.VatRateResponse;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunInvoicesRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.nomenclature.product.VatRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ManualCreditOrDebitNoteMapperService {

    private final InterestRateRepository interestRateRepository;

    private final VatRateRepository vatRateRepository;

    private final BankRepository bankRepository;

    private final BillingRunInvoicesRepository billingRunInvoicesRepository;

    private final InvoiceRepository invoiceRepository;

    public ManualCreditOrDebitNoteBasicDataParametersResponse mapBasicDataParameters(BillingRun billingRun) {
        ManualCreditOrDebitNoteBasicDataParametersResponse basicDataParametersResponse = new ManualCreditOrDebitNoteBasicDataParametersResponse();
        basicDataParametersResponse.setBasisForIssuing(billingRun.getBasisForIssuing());
        basicDataParametersResponse.setNumberOfIncomeAccount(billingRun.getNumberOfIncomeAccount());
        basicDataParametersResponse.setNumberOfIncomeAccountManual(billingRun.getNumberOfIncomeAccount() != null);
        basicDataParametersResponse.setCostCenterControllingOrder(billingRun.getCostCenterControllingOrder());
        basicDataParametersResponse.setCostCenterControllingOrderManual(billingRun.getCostCenterControllingOrder() != null);

        if (billingRun.getVatRateId() != null || (billingRun.getGlobalVatRate() != null && billingRun.getGlobalVatRate().equals(Boolean.TRUE))) {
            basicDataParametersResponse.setVatRateManual(true);
            if (billingRun.getVatRateId() != null) {
                basicDataParametersResponse.setVatRate(getVatRate(billingRun.getVatRateId()));
            }
        }

        if (billingRun.getGlobalVatRate() != null) {
            basicDataParametersResponse.setGlobalVatRate(billingRun.getGlobalVatRate());
        }

        if (billingRun.getInterestRateId() != null) {
            basicDataParametersResponse.setApplicableInterestRateManual(true);
            basicDataParametersResponse.setApplicableInterestRate(getInterestRate(billingRun.getInterestRateId()));
        }

        basicDataParametersResponse.setDirectDebit(billingRun.getDirectDebit());
        basicDataParametersResponse.setDirectDebitManual(billingRun.getDirectDebit() != null);

        if (billingRun.getBankId() != null) {
            basicDataParametersResponse.setBank(getBank(billingRun.getBankId()));
        }

        basicDataParametersResponse.setIban(billingRun.getIban());
        basicDataParametersResponse.setDocumentType(billingRun.getDocumentType());
        basicDataParametersResponse.setInvoiceResponseList(getInvoiceResponseList(billingRun.getId()));

        return basicDataParametersResponse;
    }

    private InterestRateShortResponse getInterestRate(Long interestRateId) {
        return new InterestRateShortResponse(
                interestRateRepository.findByIdAndStatusIn(interestRateId, List.of(InterestRateStatus.ACTIVE))
                        .orElseThrow(() ->
                                new DomainEntityNotFoundException("interest rate with given id: %s not found".formatted(interestRateId))
                        )
        );
    }

    private VatRateResponse getVatRate(Long vatRateId) {
        return new VatRateResponse(
                vatRateRepository.findByIdAndStatus(vatRateId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE,NomenclatureItemStatus.DELETED))
                        .orElseThrow(() ->
                                new DomainEntityNotFoundException("vat rate with given id: %s not found".formatted(vatRateId)))
        );
    }

    private BankResponse getBank(Long bankId) {
        return new BankResponse(
                bankRepository.findByIdAndStatus(bankId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE,NomenclatureItemStatus.DELETED))
                        .orElseThrow(() -> new DomainEntityNotFoundException("bank with given id: %s not found;".formatted(bankId)))
        );
    }

    private List<BillingRunInvoiceResponse> getInvoiceResponseList(Long billingRunId) {
        return billingRunInvoicesRepository.findManualCreditNoteInvoices(billingRunId);
    }
}
