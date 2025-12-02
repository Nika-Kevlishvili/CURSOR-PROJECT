package bg.energo.phoenix.service.crm.smsCommunication;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.documentModels.EmailAndSmsDocumentModel;
import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunication;
import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunicationCustomers;
import bg.energo.phoenix.model.entity.template.ContractTemplate;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.enums.contract.ContractType;
import bg.energo.phoenix.repository.crm.smsCommunication.SmsCommunicationCustomersRepository;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.service.document.DocumentGenerationService;
import bg.energo.phoenix.service.document.EmailAndSmsDocumentCreationService;
import bg.energo.phoenix.service.document.EmailAndSmsDocumentRequest;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;
import static bg.energo.phoenix.service.document.DocumentParserService.parseDocx;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsCommunicationJobService {
    private final SmsCommunicationCustomersRepository smsCommunicationCustomersRepository;
    private final EmailAndSmsDocumentCreationService emailAndSmsDocumentCreationService;
    private final ContractTemplateRepository contractTemplateRepository;
    private final ContractTemplateDetailsRepository contractTemplateDetailsRepository;
    private final DocumentGenerationService documentGenerationService;
    private final DocumentGenerationUtil documentGenerationUtil;
    private final FileService fileService;
    @Value("${ftp.server.base.path}")
    private String ftpBasePath;
    private static final String FOLDER_PATH = "mass_sms_document";
    @Transactional
    public void generateBody() {
        List<Object[]> smsCommunicationCustomers = smsCommunicationCustomersRepository.findSmsForBodyGeneration();

        for(Object[] object : smsCommunicationCustomers) {
            SmsCommunication smsCommunication = (SmsCommunication) object[0];
            SmsCommunicationCustomers smsCommunicationCustomer = (SmsCommunicationCustomers) object[1];
            EmailAndSmsDocumentModel emailAndSmsDocumentModel = emailAndSmsDocumentCreationService
                    .generateDocumentJsonModel(new EmailAndSmsDocumentRequest(
                            smsCommunicationCustomer.getContractNumber(),
                            null,
                            smsCommunicationCustomer.getId(),
                            getContractDetailId(smsCommunicationCustomer),
                            smsCommunicationCustomer.getContractId(),
                            determineContractType(smsCommunicationCustomer)
                    ));
            String body = generateBody(smsCommunication.getTemplateId(),emailAndSmsDocumentModel);
            smsCommunicationCustomer.setSmsBody(body);
            smsCommunicationCustomersRepository.save(smsCommunicationCustomer);
            // todo send logic
        }
    }

    private Long getContractDetailId(SmsCommunicationCustomers smsCommunicationCustomers) {
        Long id = null;
        if(smsCommunicationCustomers.getProductContractDetailId()!=null) {
            id = smsCommunicationCustomers.getProductContractDetailId();
        } else if(smsCommunicationCustomers.getServiceContractDetailid()!=null) {
            id = smsCommunicationCustomers.getServiceContractDetailid();
        }
        return id;
    }

    private ContractType determineContractType(SmsCommunicationCustomers smsCommunicationCustomers) {
        ContractType contractType = null;
        if(smsCommunicationCustomers.getProductContractDetailId()!=null) {
            contractType = ContractType.PRODUCT_CONTRACT;
        } else if(smsCommunicationCustomers.getServiceContractDetailid()!=null) {
            contractType = ContractType.SERVICE_CONTRACT;
        }
        return contractType;
    }

    private String generateBody(Long templateId,EmailAndSmsDocumentModel emailAndSmsDocumentModel) {
        String destinationPath = String.format("%s/%s/%s", ftpBasePath, FOLDER_PATH, LocalDate.now());
        ContractTemplate template = contractTemplateRepository.findById(templateId).orElseThrow(
                () -> new DomainEntityNotFoundException("Can't find template with id: %s".formatted(templateId))
        );

        ContractTemplateDetail templateDetail = contractTemplateDetailsRepository.findById(template.getLastTemplateDetailId())
                .orElseThrow(() -> new DomainEntityNotFoundException(
                        "Can't find template details with id: %s".formatted(template.getLastTemplateDetailId())));
        String fileName = UUID.randomUUID().toString();

        try {
            ByteArrayResource templateFileResource = new ByteArrayResource(Files.readAllBytes(new File(documentGenerationUtil.getTemplateFileLocalPath(templateDetail)).toPath()));
            DocumentPathPayloads documentPathPayloads = documentGenerationService
                    .generateDocument(
                            templateFileResource,
                            destinationPath,
                            fileName,
                            emailAndSmsDocumentModel,
                            Set.of(FileFormat.DOCX),
                            false
                    );

            ByteArrayResource byteArrayResource = fileService.downloadFile(documentPathPayloads.docXPath());
            String emailBody = null;
            try {
                emailBody = parseDocx(byteArrayResource.getByteArray());
            } catch (Exception e) {
                log.error("Can't parse docx file", e);
                throw new ClientException("Can't parse docx", ErrorCode.APPLICATION_ERROR);
            }
            return emailBody;
        }catch (Exception e) {
            log.error("Exception while storing template file: {}", e.getMessage());
            throw new ClientException("Exception while storing template file: %s".formatted(e.getMessage()), APPLICATION_ERROR);
        }
    }
}
