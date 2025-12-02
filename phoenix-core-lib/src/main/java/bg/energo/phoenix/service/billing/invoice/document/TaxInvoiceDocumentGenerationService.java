package bg.energo.phoenix.service.billing.invoice.document;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceDocument;
import bg.energo.phoenix.model.entity.billing.invoice.ManualDebitOrCreditNoteInvoiceSummaryData;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.repository.billing.invoice.InvoiceDocumentRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.template.ContractTemplateFileRepository;
import bg.energo.phoenix.service.billing.billingRun.BillingRunDocumentDataCreationService;
import bg.energo.phoenix.service.billing.model.impl.BillingRunDocumentModelImpl;
import bg.energo.phoenix.service.billing.model.persistance.BillingRunDocumentModel;
import bg.energo.phoenix.service.billing.model.persistance.CompanyDetailedInformationModel;
import bg.energo.phoenix.service.document.DocumentGenerationService;
import bg.energo.phoenix.service.document.enums.DocumentPathPayloads;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Set;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxInvoiceDocumentGenerationService {

    @Value("${invoice.document.ftp_directory_path}")
    private String invoiceDocumentFtpDirectoryPath;
    private final FileService fileService;
    private final InvoiceRepository invoiceRepository;
    private final BillingRunDocumentDataCreationService billingRunDocumentDataCreationService;
    private final ContractTemplateFileRepository contractTemplateFileRepository;
    private final DocumentGenerationService documentGenerationService;
    private final InvoiceDocumentRepository invoiceDocumentRepository;
    private final DocumentGenerationUtil documentGenerationUtil;

    @Transactional
    public InvoiceDocument generateDocumentForInvoice(Invoice invoice, ManualDebitOrCreditNoteInvoiceSummaryData manualDebitOrCreditNoteInvoiceSummaryData, ContractTemplateDetail templateDetail) {
        LocalDate date = LocalDate.now();
        String destinationPath = "%s/%s".formatted(invoiceDocumentFtpDirectoryPath, date.toString());
        log.debug("Starting fetching respective company detailed information");
        CompanyDetailedInformationModel companyDetailedInformation = billingRunDocumentDataCreationService.fetchCompanyDetailedInformationModel(date);
        byte[] companyLogoContent = billingRunDocumentDataCreationService.fetchCompanyLogoContent(companyDetailedInformation);
        BillingRunDocumentModel documentSpecificData = invoiceRepository.getInvoiceDocumentModel(invoice.getId());

        BillingRunDocumentModelImpl documentModel = new BillingRunDocumentModelImpl();
        documentModel.fillCompanyDetailedInformation(companyDetailedInformation);
        documentModel.fillInvoiceData(documentSpecificData);
        documentModel.fillSummaryDataForTaxInvoice(manualDebitOrCreditNoteInvoiceSummaryData, documentSpecificData);
        documentModel.CompanyLogo = companyLogoContent;

        log.debug("Starting generation on Invoice with number: [%s]".formatted(invoice.getInvoiceNumber()));
        ByteArrayResource templateFileResource;
        try {
            templateFileResource = new ByteArrayResource(Files.readAllBytes(new File(documentGenerationUtil.getTemplateFileLocalPath(templateDetail)).toPath()));
            log.debug("Clone of template file created");

            String fileName = documentGenerationUtil.formatInvoiceFileName(invoice, templateDetail);
            log.debug("File name was formatted: [%s]".formatted(fileName));

            log.debug("Generating document");
            DocumentPathPayloads documentPathPayloads = documentGenerationService
                    .generateDocument(
                            templateFileResource,
                            destinationPath,
                            fileName,
                            documentModel,
                            Set.of(FileFormat.PDF), // hardcoded output file format
                            false
                    );
            log.debug("Documents was created successfully");

            log.debug("Saving invoice document entity");
            InvoiceDocument invoiceDocument = invoiceDocumentRepository.save(
                    InvoiceDocument
                            .builder()
                            .fileUrl(documentPathPayloads.pdfPath())
                            .originalTemplateId(templateDetail.getTemplateId())
                            .name(fileName)
                            .invoiceId(invoice.getId())
                            .build()
            );
            log.debug("Invoice document entity was created with id: [%s]".formatted(invoiceDocument.getId()));
            return invoiceDocument;
        } catch (Exception e) {
            log.error("exception while storing template file: {}", e.getMessage());
            throw new ClientException("exception while storing template file: %s".formatted(e.getMessage()), APPLICATION_ERROR);
        }
    }
}
