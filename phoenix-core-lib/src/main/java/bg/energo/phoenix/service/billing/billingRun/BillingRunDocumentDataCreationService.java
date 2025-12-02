package bg.energo.phoenix.service.billing.billingRun;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyLogos;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyDetailRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyLogoRepository;
import bg.energo.phoenix.repository.billing.compensation.CompensationRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceDocumentDataRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.service.billing.model.impl.BillingRunDocumentModelImpl;
import bg.energo.phoenix.service.billing.model.persistance.BillingRunDocumentModel;
import bg.energo.phoenix.service.billing.model.persistance.CompanyDetailedInformationModel;
import bg.energo.phoenix.service.billing.model.persistance.dao.BillingRunDocumentCompensationDAO;
import bg.energo.phoenix.service.billing.model.persistance.dao.BillingRunDocumentDetailedDataDAO;
import bg.energo.phoenix.service.billing.model.persistance.dao.BillingRunDocumentSummeryDataDAO;
import bg.energo.phoenix.service.billing.model.persistance.projection.BillingRunDocumentVatBaseProjection;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.util.epb.EPBDecimalUtils;
import bg.energo.phoenix.util.transliteration.BulgarianTransliterationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRunDocumentDataCreationService {
    private final CompanyDetailRepository companyDetailRepository;
    private final CompanyLogoRepository companyLogoRepository;
    private final FileService fileService;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceDocumentDataRepository invoiceDocumentDataRepository;
    private final BillingRunRepository billingRunRepository;
    private final CompensationRepository compensationRepository;

    public BillingRunDocumentModelImpl generateBillingRunDocumentModel(Invoice invoice,
                                                                       CompanyDetailedInformationModel companyDetailedInformationModel,
                                                                       byte[] companyLogo) {
        BillingRunDocumentModelImpl documentModel = createDocumentModel();

        BillingRunDocumentModel documentSpecificData = invoiceRepository.getInvoiceDocumentModel(invoice.getId());
        List<BillingRunDocumentSummeryDataDAO> summaryData = fetchSummaryData(invoice);
        List<BillingRunDocumentDetailedDataDAO> detailedData = fetchDetailedData(invoice);
        List<BillingRunDocumentCompensationDAO> compensations = compensationRepository.findCompensationDaoByInvoiceId(
                invoice.getId());
        List<BillingRunDocumentVatBaseProjection> vatBaseProjections = invoiceDocumentDataRepository.findVatBaseDataByInvoiceId(invoice.getId());
        List<Invoice> connectedInvoices = fetchConnectedInvoices(invoice);

        documentModel.fillCompanyDetailedInformation(companyDetailedInformationModel);
        documentModel.fillInvoiceData(documentSpecificData);
        documentModel.fillCompensations(compensations);
        documentModel.fillVatBase(vatBaseProjections);
        calculateFinalLiabilityAmount(documentModel, compensations, vatBaseProjections);
        documentModel.fillSummaryData(summaryData);
        documentModel.fillConnectedInvoices(connectedInvoices);
        documentModel.fillDetailedData(detailedData, compensations,vatBaseProjections);
        documentModel.CompanyLogo = companyLogo;

        return documentModel;
    }

    private void calculateFinalLiabilityAmount(BillingRunDocumentModelImpl documentModel, List<BillingRunDocumentCompensationDAO> compensations, List<BillingRunDocumentVatBaseProjection> vatBaseProjections) {
        BigDecimal totalCompensationAmount = compensations.stream().reduce(BigDecimal.ZERO, (first, second) -> first.add(ObjectUtils.defaultIfNull(second.getAmount(), BigDecimal.ZERO)), BigDecimal::add);
        BigDecimal vatBaseAmount = vatBaseProjections.stream().reduce(BigDecimal.ZERO, (first, second) -> first.add(ObjectUtils.defaultIfNull(second.getValue(), BigDecimal.ZERO)), BigDecimal::add);
        BigDecimal totalInclVat = documentModel.TotalInclVat;

        BigDecimal finalLiabilityAmount = EPBDecimalUtils.convertToCurrencyScale(totalInclVat.subtract(totalCompensationAmount).subtract(vatBaseAmount));
        documentModel.FinalLiabilityAmount = finalLiabilityAmount;

        try {
            BigDecimal amount = new BigDecimal(finalLiabilityAmount.toString());
            BigDecimal integerAmount = amount.setScale(0, RoundingMode.FLOOR);
            BigDecimal fractionAmount = amount.subtract(amount.setScale(0, RoundingMode.FLOOR))
                    .movePointRight(amount.scale());

            documentModel.FinalLiabilityAmountWithWords = BulgarianTransliterationUtil.convertAmountToWords(
                    integerAmount.toBigInteger()
                            .intValue(),
                    fractionAmount.intValue()
            );
        } catch (Exception e) {
            log.error("Cannot transliterate amount in words", e);
        }

    }

    private List<Invoice> fetchConnectedInvoices(Invoice invoice) {
        return invoiceRepository.findConnectedCreditDebitNotes(invoice.getId());
    }

    private List<BillingRunDocumentSummeryDataDAO> fetchSummaryData(Invoice invoice) {
        switch (invoice.getInvoiceType()) {
            case MANUAL -> {
                switch (invoice.getInvoiceDocumentType()) {
                    case INVOICE -> {
                        return invoiceDocumentDataRepository.getManualInvoiceSummaryData(invoice.getId())
                                .stream()
                                .map(BillingRunDocumentSummeryDataDAO::new)
                                .toList();
                    }
                    case CREDIT_NOTE, DEBIT_NOTE -> {
                        return invoiceDocumentDataRepository.getManualDebitOrCreditNoteSummaryData(invoice.getId())
                                .stream()
                                .map(BillingRunDocumentSummeryDataDAO::new)
                                .toList();
                    }
                }
            }
            case INTERIM_AND_ADVANCE_PAYMENT -> {
                if (billingRunRepository.isBillingRunTypeManualInterim(invoice.getId())) {
                    return invoiceDocumentDataRepository.getManualInterimAdvancePaymentSummaryData(invoice.getId())
                            .stream()
                            .map(BillingRunDocumentSummeryDataDAO::new)
                            .toList();
                } else {
                    return invoiceDocumentDataRepository.getStandardInterimAdvancePaymentSummaryData(invoice.getId())
                            .stream()
                            .map(BillingRunDocumentSummeryDataDAO::new)
                            .toList();
                }
            }
            default -> {
                return invoiceDocumentDataRepository.getStandardInvoiceSummaryData(invoice.getId())
                        .stream()
                        .map(BillingRunDocumentSummeryDataDAO::new)
                        .toList();
            }
        }
        return new ArrayList<>();
    }

    private List<BillingRunDocumentDetailedDataDAO> fetchDetailedData(Invoice invoice) {
        switch (invoice.getInvoiceType()) {
            case MANUAL -> {
                switch (invoice.getInvoiceDocumentType()) {
                    case INVOICE -> {
                        return invoiceDocumentDataRepository.getManualInvoiceDetailedData(invoice.getId());
                    }
                    case DEBIT_NOTE, CREDIT_NOTE -> {
                        return invoiceDocumentDataRepository.getManualDebitCreditNoteInvoiceDetailedData(invoice.getId());
                    }
                }
            }
            default -> {
                return invoiceDocumentDataRepository
                        .getStandardInvoiceDetailedData(invoice.getId())
                        .stream()
                        .map(BillingRunDocumentDetailedDataDAO::new)
                        .peek(x -> {

                        })
                        .toList();
            }
        }
        return new ArrayList<>();
    }

    public CompanyDetailedInformationModel fetchCompanyDetailedInformationModel(LocalDate billingDate) {
        log.debug("Starting fetching respective company detailed information");
        return companyDetailRepository.getCompanyDetailedInformation(billingDate);
    }

    public byte[] fetchCompanyLogoContent(CompanyDetailedInformationModel companyDetailedInformation) {
        try {
            Optional<CompanyLogos> companyLogoOptional = companyLogoRepository
                    .findFirstByCompanyDetailIdAndStatus(companyDetailedInformation.getCompanyDetailId(), EntityStatus.ACTIVE);
            if (companyLogoOptional.isPresent()) {
                CompanyLogos companyLogo = companyLogoOptional.get();

                return fileService.downloadFile(companyLogo.getFileUrl()).getContentAsByteArray();
            }
        } catch (Exception e) {
            log.error("Exception handled while trying to download company logo");
        }
        return null;
    }

    private BillingRunDocumentModelImpl createDocumentModel() {
        return new BillingRunDocumentModelImpl();
    }
}
