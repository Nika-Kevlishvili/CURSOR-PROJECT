package bg.energo.phoenix.service.billing.billingRun;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.billing.invoice.Invoice;
import bg.energo.phoenix.model.entity.billing.invoice.InvoiceDocumentFile;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.entity.template.ContractTemplateFiles;
import bg.energo.phoenix.model.enums.billing.invoice.InvoiceStatus;
import bg.energo.phoenix.model.enums.documents.DocumentSigners;
import bg.energo.phoenix.model.enums.documents.DocumentStatus;
import bg.energo.phoenix.model.enums.template.ContractTemplateSigning;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceDocumentFileRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateFileRepository;
import bg.energo.phoenix.service.archivation.edms.FileArchivationService;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAttributeProperties;
import bg.energo.phoenix.service.billing.invoice.numberGeneration.InvoiceNumberService;
import bg.energo.phoenix.service.billing.model.impl.BillingRunDocumentModelImpl;
import bg.energo.phoenix.service.billing.model.persistance.CompanyDetailedInformationModel;
import bg.energo.phoenix.service.document.DocumentGenerationService;
import bg.energo.phoenix.service.document.enums.DocumentPathPayloads;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.signing.SignerChainManager;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRunDocumentCreationService {
    private final InvoiceDocumentFileRepository invoiceDocumentFileRepository;
    private final DocumentGenerationService documentGenerationService;
    private final SignerChainManager signerChainManager;
    private final InvoiceRepository invoiceRepository;
    private final DocumentGenerationUtil documentGenerationUtil;
    private final ContractTemplateDetailsRepository contractTemplateDetailsRepository;
    private final ContractTemplateFileRepository contractTemplateFileRepository;
    private final FileService fileService;
    private final BillingRunDocumentDataCreationService billingRunDocumentDataCreationService;
    private final DocumentsRepository documentsRepository;
    private final InvoiceNumberService invoiceNumberService;
    private final EDMSAttributeProperties edmsAttributeProperties;
    private final FileArchivationService fileArchivationService;
    private final CustomerRepository customerRepository;
    private final BillingRunRepository billingRunRepository;
    private final ContractTemplateDetailsRepository templateDetailsRepository;

    @Value("${invoice.document.ftp_directory_path}")
    private String invoiceDocumentFtpDirectoryPath;

    @ExecutionTimeLogger
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pair<Invoice, Document> generate(LocalDate billingDate,
                                            BillingRun billingRun,
                                            Invoice invoice,
                                            Pair<String, ContractTemplateDetail> defaultContractTemplateData,
                                            CompanyDetailedInformationModel companyDetailedInformation,
                                            byte[] companyLogoContent
    ) throws Exception {

        invoice.setInvoiceStatus(InvoiceStatus.DRAFT_GENERATED);
        invoiceNumberService.fillInvoiceNumber(invoice);

        return generateDocumentForBillingInvoice(billingDate, billingRun, invoice, defaultContractTemplateData, companyDetailedInformation, companyLogoContent);
    }

    @ExecutionTimeLogger
    @Transactional(propagation = Propagation.REQUIRED)
    public void generateDocumentOnRegeneration(Invoice invoice) throws Exception {
        Optional<BillingRun> billingRun = billingRunRepository.getBillingRunById(invoice.getBillingId());

        if (billingRun.isEmpty()) {
            throw new DomainEntityNotFoundException("BillingRun for invoice with id: [%s] not found".formatted(invoice.getId()));
        }

        if (invoice.getInvoiceDocumentId() != null && invoice.getInvoiceDocumentId() != 0L) {
            Document currentInvoiceDocument = documentsRepository.getReferenceById(invoice.getInvoiceDocumentId());
            currentInvoiceDocument.setStatus(EntityStatus.DELETED);
            documentsRepository.saveAndFlush(currentInvoiceDocument);
        }

        BillingRun br = billingRun.get();
        LocalDate billingDate = LocalDate.now();

        Pair<String, ContractTemplateDetail> defaultContractTemplateData = downloadBillingRunDefaultTemplate(billingDate, br);

        CompanyDetailedInformationModel companyDetailedInformation = billingRunDocumentDataCreationService.fetchCompanyDetailedInformationModel(billingDate);
        byte[] companyLogoContent = billingRunDocumentDataCreationService.fetchCompanyLogoContent(companyDetailedInformation);

        Pair<Invoice, Document> generatedResult = generateDocumentForBillingInvoice(billingDate, br, invoice, defaultContractTemplateData, companyDetailedInformation, companyLogoContent);
    }


    public Pair<String, ContractTemplateDetail> downloadBillingRunDefaultTemplate(LocalDate billingRunDate, BillingRun billingRun) {
        if (Objects.nonNull(billingRun.getTemplateId())) {
            ContractTemplateDetail contractTemplateDetail = templateDetailsRepository
                    .findRespectiveTemplateDetailsByTemplateIdAndDate(billingRun.getTemplateId(), billingRunDate)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Respective Contract Template detail not found for billing run with id: [%s] and date: [%s]".formatted(billingRun.getId(), billingRunDate)));

            ContractTemplateFiles contractTemplateFile = contractTemplateFileRepository
                    .findByIdAndStatus(contractTemplateDetail.getTemplateFileId(), EntityStatus.ACTIVE)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Contract Template File with id: [%s] not found or DELETED;".formatted(contractTemplateDetail.getTemplateFileId())));

            try {
                log.debug("Start downloading template from FTP path: [%s]".formatted(contractTemplateFile.getFileUrl()));
                ByteArrayResource templateFileResource = fileService.downloadFile(contractTemplateFile.getFileUrl());
                Path tempFile = Files.createTempFile("cur", ".docx");
                String templateFileLocalPath;
                try {
                    Files.write(tempFile, templateFileResource.getByteArray());
                    templateFileLocalPath = tempFile.toAbsolutePath().toString();
                    log.debug("Template downloaded, local path: [%s]".formatted(templateFileLocalPath));
                } catch (Exception e) {
                    throw new ClientException("Exception while reading file: [%s]".formatted(e.getMessage()), APPLICATION_ERROR);
                }

                return Pair.of(templateFileLocalPath, contractTemplateDetail);
            } catch (Exception e) {
                throw new ClientException("Error handled while trying to download billing run template file;", APPLICATION_ERROR);
            }
        }

        return null;
    }

    private Pair<Invoice, Document> generateDocumentForBillingInvoice(LocalDate billingDate,
                                                                      BillingRun billingRun,
                                                                      Invoice invoice,
                                                                      Pair<String, ContractTemplateDetail> defaultContractTemplateData,
                                                                      CompanyDetailedInformationModel companyDetailedInformation,
                                                                      byte[] companyLogoContent
    ) throws Exception {
        MDC.put("billingNumber", billingRun.getBillingNumber());
        String destinationPath = "%s/%s".formatted(invoiceDocumentFtpDirectoryPath, billingDate.toString());

        Map<Long, Pair<String, ContractTemplateDetail>> contractTemplateDataCache = Collections.synchronizedMap(new HashMap<>());

        log.debug("Defining valid contract template");
        Pair<String, ContractTemplateDetail> validContractTemplateData = defineValidContractTemplateData(defaultContractTemplateData, invoice, contractTemplateDataCache);

        log.debug("Filling company detailed information");
        BillingRunDocumentModelImpl documentModel = billingRunDocumentDataCreationService.generateBillingRunDocumentModel(
                invoice,
                companyDetailedInformation,
                companyLogoContent
        );

        log.debug("Starting generation on Invoice with number: [%s]".formatted(invoice.getInvoiceNumber()));
        ByteArrayResource templateFileResource = new ByteArrayResource(Files.readAllBytes(new File(validContractTemplateData.getKey()).toPath()));
        log.debug("Clone of template file created");

        ContractTemplateDetail contractTemplateDetail = validContractTemplateData.getValue();
        String fileName = documentGenerationUtil.formatInvoiceFileName(invoice, contractTemplateDetail);
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


        boolean needSigningWithSystemCertificate = CollectionUtils
                .emptyIfNull(contractTemplateDetail.getFileSigning())
                .contains(ContractTemplateSigning.SIGNING_WITH_SYSTEM_CERTIFICATE);

        List<DocumentSigners> documentSigners = new ArrayList<>();
        if (needSigningWithSystemCertificate) {
            documentSigners.add(ContractTemplateSigning.SIGNING_WITH_SYSTEM_CERTIFICATE.getDocumentSigners());
        } else {
            documentSigners.add(ContractTemplateSigning.NO.getDocumentSigners());
        }

        Document document = Document
                .builder()
                .signers(documentSigners)
                .signedBy(new ArrayList<>())
                .name(fileName)
                .unsignedFileUrl(documentPathPayloads.pdfPath())
                .signedFileUrl(documentPathPayloads.pdfPath())
                .fileFormat(FileFormat.PDF)
                .templateId(contractTemplateDetail.getTemplateId())
                .documentStatus(DocumentStatus.UNSIGNED)
                .status(EntityStatus.ACTIVE)
                .build();

        documentsRepository.saveAndFlush(document);

        setAttributesToDocument(invoice, document);
        fileArchivationService.archiveDocument(document);
        documentsRepository.saveAndFlush(document);

        if (!documentSigners.contains(DocumentSigners.NO)) {
            document.setSignedFile(true);
            List<Attribute> attributes = document.getAttributes();
            if (!attributes.isEmpty()) {
                attributes.stream()
                        .filter(it -> it.getAttributeGuid().equals(edmsAttributeProperties.getSignedGuid()))
                        .findFirst()
                        .ifPresent(it -> it.setValue(true));
                document.setAttributes(attributes);
            }
            signerChainManager.startSign(List.of(document));
            fileArchivationService.archiveDocument(document);
            documentsRepository.saveAndFlush(document);
        }

        invoice.setInvoiceDocumentId(document.getId());

        log.debug("Updating invoice");
        invoiceRepository.saveAndFlush(invoice);

        log.debug("Createing invoice document file relation");
        InvoiceDocumentFile invoiceDocumentFile = new InvoiceDocumentFile(null, document.getId(), invoice.getId());
        invoiceDocumentFileRepository.save(invoiceDocumentFile);

        log.debug("Invoice document generation ended successfully");

        return Pair.of(invoice, document);
    }

    private void setAttributesToDocument(Invoice invoice, Document document) {
        Optional<Customer> customer = customerRepository.findById(invoice.getCustomerId());

        document.setNeedArchive(true);
        document.setArchivationConstraints(EDMSArchivationConstraints.DOCUMENT_TYPE_INVOICE_GENERATED_DOCUMENT);
        document.setArchivedFileType(EDMSArchivationConstraints.DOCUMENT_TYPE_INVOICE_GENERATED_DOCUMENT.getValue());
        document.setAttributes(
                List.of(
                        new Attribute(edmsAttributeProperties.getDocumentTypeGuid(), EDMSArchivationConstraints.DOCUMENT_TYPE_INVOICE_GENERATED_DOCUMENT),
                        new Attribute(edmsAttributeProperties.getDocumentNumberGuid(), invoice.getInvoiceNumber()),
                        new Attribute(edmsAttributeProperties.getDocumentDateGuid(), LocalDateTime.now()),
                        new Attribute(edmsAttributeProperties.getCustomerIdentifierGuid(), customer.<Object>map(Customer::getIdentifier).orElse(null)),
                        new Attribute(edmsAttributeProperties.getCustomerNumberGuid(), customer.map(Customer::getCustomerNumber).orElse(null)),
                        new Attribute(edmsAttributeProperties.getSignedGuid(), false)
                )
        );
        document.setSignedFile(false);
    }

    private Pair<String, ContractTemplateDetail> defineValidContractTemplateData(Pair<String, ContractTemplateDetail> defaultContractTemplateData, Invoice nonGeneratedInvoice, Map<Long, Pair<String, ContractTemplateDetail>> cachedContractTemplateData) throws Exception {
        if (defaultContractTemplateData == null) {
            Pair<String, ContractTemplateDetail> cachedData = cachedContractTemplateData.get(nonGeneratedInvoice.getTemplateDetailId());
            if (cachedData == null) {
                ContractTemplateDetail contractTemplateDetail = contractTemplateDetailsRepository
                        .findById(Optional.ofNullable(nonGeneratedInvoice.getTemplateDetailId())
                                .orElseThrow(() -> new DomainEntityNotFoundException("Cannot define valid template for invoice with id: [%s]".formatted(nonGeneratedInvoice.getId()))))
                        .orElseThrow(() -> new DomainEntityNotFoundException("Respective Contract Template detail not found for invoice with id: [%s]".formatted(nonGeneratedInvoice.getTemplateDetailId())));

                ContractTemplateFiles contractTemplateFile = contractTemplateFileRepository
                        .findByIdAndStatus(contractTemplateDetail.getTemplateFileId(), EntityStatus.ACTIVE)
                        .orElseThrow(() -> new DomainEntityNotFoundException("Contract Template File with id: [%s] not found or DELETED;".formatted(contractTemplateDetail.getTemplateFileId())));

                String templateFileLocalPath;
                try {
                    log.debug("Start downloading template from FTP path: [%s]".formatted(contractTemplateFile.getFileUrl()));
                    ByteArrayResource templateFileResource = fileService.downloadFile(contractTemplateFile.getFileUrl());
                    Path tempFile = Files.createTempFile("cur", ".docx");
                    try {
                        Files.write(tempFile, templateFileResource.getByteArray());
                        templateFileLocalPath = tempFile.toAbsolutePath().toString();
                        log.debug("Template downloaded, local path: [%s]".formatted(templateFileLocalPath));
                    } catch (Exception e) {
                        throw new ClientException("Exception while reading file: [%s]".formatted(e.getMessage()), APPLICATION_ERROR);
                    }
                } catch (Exception e) {
                    throw new ClientException("Error handled while trying to download invoice template file;", APPLICATION_ERROR);
                }

                Pair<String, ContractTemplateDetail> validContractTemplateData = Pair.of(templateFileLocalPath, contractTemplateDetail);
                cachedContractTemplateData.put(nonGeneratedInvoice.getTemplateDetailId(), validContractTemplateData);
                return validContractTemplateData;
            } else {
                return cachedData;
            }
        }
        return defaultContractTemplateData;
    }

}
