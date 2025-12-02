package bg.energo.phoenix.service.product.termination.terminations;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.documentModels.mlo.Manager;
import bg.energo.phoenix.model.documentModels.termination.TerminationEmailDocumentResponse;
import bg.energo.phoenix.model.documentModels.termination.TerminationEmailModel;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationAttachment;
import bg.energo.phoenix.model.entity.customer.communication.CustomerCommunicationContacts;
import bg.energo.phoenix.model.entity.documents.Document;
import bg.energo.phoenix.model.entity.nomenclature.crm.EmailMailboxes;
import bg.energo.phoenix.model.entity.nomenclature.crm.TopicOfCommunication;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.customer.CustomerCommContactTypes;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.crm.emailCommunication.DocumentEmailCommunicationCreateRequest;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyDetailRepository;
import bg.energo.phoenix.repository.billing.companyDetails.CompanyLogoRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationAttachmentRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.communicationData.CustomerCommunicationContactsRepository;
import bg.energo.phoenix.repository.documents.DocumentsRepository;
import bg.energo.phoenix.repository.nomenclature.crm.EmailMailboxesRepository;
import bg.energo.phoenix.repository.nomenclature.crm.TopicOfCommunicationRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.crm.emailCommunication.EmailCommunicationService;
import bg.energo.phoenix.service.document.AbstractDocumentCreationService;
import bg.energo.phoenix.service.document.DocumentGenerationService;
import bg.energo.phoenix.service.document.DocumentParserService;
import bg.energo.phoenix.service.document.enums.DocumentPathPayloads;
import bg.energo.phoenix.service.document.enums.FileFormat;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.signing.SignerChainManager;
import bg.energo.phoenix.util.template.document.DocumentGenerationUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ContractTerminationEmailSenderService extends AbstractDocumentCreationService {
    private final ProductContractRepository productContractRepository;
    private final ServiceContractsRepository serviceContractsRepository;
    private final EmailCommunicationService emailCommunicationService;
    private final CustomerRepository customerRepository;
    private final TopicOfCommunicationRepository topicOfCommunicationRepository;
    private final EmailMailboxesRepository emailMailboxesRepository;
    private final CustomerCommunicationContactsRepository customerCommunicationContactsRepository;
    private final EmailCommunicationAttachmentRepository emailCommunicationAttachmentRepository;
    private final static String FOLDER_PATH = "termination_files";

    public ContractTerminationEmailSenderService(
            ContractTemplateDetailsRepository contractTemplateDetailsRepository,
            ContractTemplateRepository contractTemplateRepository,
            DocumentGenerationService documentGenerationService,
            DocumentGenerationUtil documentGenerationUtil,
            DocumentsRepository documentsRepository,
            CompanyDetailRepository companyDetailRepository,
            CompanyLogoRepository companyLogoRepository,
            SignerChainManager signerChainManager,
            FileService fileService,
            ProductContractRepository productContractRepository,
            ServiceContractsRepository serviceContractsRepository,
            EmailCommunicationService emailCommunicationService,
            CustomerRepository customerRepository, TopicOfCommunicationRepository topicOfCommunicationRepository, EmailMailboxesRepository emailMailboxesRepository, CustomerCommunicationContactsRepository customerCommunicationContactsRepository, EmailCommunicationAttachmentRepository emailCommunicationAttachmentRepository
    ) {
        super(
                contractTemplateDetailsRepository,
                contractTemplateRepository,
                documentGenerationService,
                documentGenerationUtil,
                documentsRepository,
                companyDetailRepository,
                companyLogoRepository,
                signerChainManager,
                fileService
        );
        this.productContractRepository = productContractRepository;
        this.serviceContractsRepository = serviceContractsRepository;
        this.emailCommunicationService = emailCommunicationService;
        this.customerRepository = customerRepository;
        this.topicOfCommunicationRepository = topicOfCommunicationRepository;
        this.emailMailboxesRepository = emailMailboxesRepository;
        this.customerCommunicationContactsRepository = customerCommunicationContactsRepository;
        this.emailCommunicationAttachmentRepository = emailCommunicationAttachmentRepository;
    }

    @Transactional
    public void createAndSendTerminationEmail(
            Long contractDetailId,
            boolean forProductContract,
            Long customerDetailId,
            Long communicationId,
            Long terminationId
    ) {

        TerminationEmailDocumentResponse response = forProductContract ?
                productContractRepository.fetchTerminationEmailResponse(contractDetailId, terminationId)
                : serviceContractsRepository.fetchTerminationEmailResponse(contractDetailId, terminationId);
        List<Manager> managers = customerRepository.getManagersByCustomer(response.getCustomerDetailId());

        TerminationEmailModel bodyModel = buildModelWithSharedInfo(new TerminationEmailModel());
        bodyModel.from(response, managers);
        Pair<Long, Pair<String, String>> templateInfo = getEmailSubjectAndBody(terminationId, bodyModel);

        List<Document> documents = forProductContract ?
                documentsRepository.findActionDocumentsForProductContractDetail(contractDetailId) :
                documentsRepository.findActionDocumentsForServiceContractDetail(contractDetailId);

        Pair<String, String> documentNameAndUrl = documents
                .stream()
                .map(a -> Pair.of(a.getName(), a.getSignedFileUrl()))
                .findFirst()
                .orElse(null);

        TopicOfCommunication topicOfCommunication = topicOfCommunicationRepository.findByNameAndStatusAndIsHardcodedTrue("Contract Termination", NomenclatureItemStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("cannot find Contract Termination topic of communication"));

        EmailMailboxes emailMailboxes = emailMailboxesRepository.findByDefaultSelectionTrue()
                .orElseThrow(() -> new DomainEntityNotFoundException("cannot find default email Mailbox"));
        String mails = customerCommunicationContactsRepository
                .findByCustomerCommIdContactTypesAndStatuses(communicationId, List.of(CustomerCommContactTypes.EMAIL), List.of(Status.ACTIVE))
                .stream()
                .map(CustomerCommunicationContacts::getContactValue)
                .collect(Collectors.joining(";"));

        EmailCommunicationAttachment attachment = null;
        if (Objects.nonNull(documentNameAndUrl)) {
            attachment = emailCommunicationAttachmentRepository.save(EmailCommunicationAttachment.builder()
                    .name(documentNameAndUrl.getLeft())
                    .fileUrl(documentNameAndUrl.getRight())
                    .status(EntityStatus.ACTIVE)
                    .build());
        }


        emailCommunicationService.createEmailFromDocument(
                DocumentEmailCommunicationCreateRequest
                        .builder()
                        .emailBody(templateInfo.getRight().getRight())
                        .emailSubject(templateInfo.getRight().getLeft())
                        .emailBoxId(emailMailboxes.getId())
                        .communicationTopicId(topicOfCommunication.getId())
                        .customerDetailId(customerDetailId)
                        .emailTemplateId(templateInfo.getKey())
                        .customerEmailAddress(mails)
                        .attachmentFileIds(attachment != null ? Set.of(attachment.getId()) : Collections.emptySet())
                        .customerCommunicationId(communicationId)
                        .build(),
                false
        );
    }

    @SneakyThrows
    private Pair<Long, Pair<String, String>> getEmailSubjectAndBody(Long terminationId, TerminationEmailModel model) {
        ContractTemplateDetail templateDetail = contractTemplateDetailsRepository
                .fetchTerminationEmailTemplate(terminationId)
                .orElseThrow(() -> new DomainEntityNotFoundException("cannot find Termination email template;"));

        LocalDate currentDate = LocalDate.now();
        String destinationPath = "%s/%s/%s".formatted(ftpBasePath, FOLDER_PATH, currentDate);

        ByteArrayResource templateFileResource = createTemplateFileResource(templateDetail);

        log.debug("Template file cloned successfully.");

        String fileName = String.format(UUID.randomUUID().toString(), ".docx");
        log.debug("Formatted file name: [{}]", fileName);

        DocumentPathPayloads documentPathPayloads = documentGenerationService.generateDocument(
                templateFileResource,
                destinationPath,
                fileName,
                model,
                Set.of(FileFormat.DOCX),
                false
        );
        log.debug("Document created successfully.");

        ByteArrayResource downloadedFile = getFileService().downloadFile(documentPathPayloads.docXPath());
        return Pair.of(
                templateDetail.getTemplateId(),
                Pair.of(
                        templateDetail.getSubject(),
                        DocumentParserService.parseDocxToHtml(downloadedFile.getByteArray())
                )
        );
    }

    @Override
    protected String folderPath() {
        return FOLDER_PATH;
    }
}
