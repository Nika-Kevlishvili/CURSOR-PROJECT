package bg.energo.phoenix.service.template;

import bg.energo.phoenix.exception.*;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.template.ContractTemplate;
import bg.energo.phoenix.model.entity.template.ContractTemplateDetail;
import bg.energo.phoenix.model.entity.template.ContractTemplateFiles;
import bg.energo.phoenix.model.enums.product.ExcludeVersions;
import bg.energo.phoenix.model.enums.template.*;
import bg.energo.phoenix.model.request.template.TemplateBaseRequest;
import bg.energo.phoenix.model.request.template.TemplateCreateRequest;
import bg.energo.phoenix.model.request.template.TemplateEditRequest;
import bg.energo.phoenix.model.request.template.TemplateListingRequest;
import bg.energo.phoenix.model.response.template.*;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.process.massImport.ExcelHelper;
import bg.energo.phoenix.repository.template.ContractTemplateDetailsRepository;
import bg.energo.phoenix.repository.template.ContractTemplateFileRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBDocxUtils;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import bg.energo.phoenix.util.template.TemplateRequestValidatorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;
import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {
    private final PermissionService permissionService;
    @Value("${ftp.server.base.path}")
    private String ftpBasePath;
    private final static String FOLDER_PATH = "template_files";

    private final FileService fileService;
    private final ContractTemplateRepository templateRepository;

    private final ContractTemplateDetailsRepository templateDetailsRepository;
    private final ContractTemplateFileRepository templateFileRepository;

    /**
     * Creates a new contract template and its initial detail.
     *
     * @param request the template creation request containing the necessary details
     * @return the ID of the newly created contract template
     */
    @Transactional
    public Long create(TemplateCreateRequest request) {
        log.debug("creating Template with request: {}", request);
        validateName(request.getName());
        validateFile(request.getFileId(), request.getOutputFileFormat(), null);
        validateDefaultCheckbox(request, null);
        ContractTemplate template = createTemplate(request);
        ContractTemplateDetail templateDetail = createTemplateDetail(request, template);
        template.setLastTemplateDetailId(templateDetail.getId());
        return template.getId();
    }

    /**
     * Retrieves the details of a contract template and its associated file.
     *
     * @param id        The ID of the contract template to retrieve.
     * @param versionId The version ID of the contract template to retrieve.
     * @return A TemplateViewResponse containing the details of the contract template and its associated file.
     * @throws DomainEntityNotFoundException if the contract template or its detail is not found.
     * @throws ClientException               if the user does not have permission to view a deleted contract template.
     */
    public TemplateViewResponse view(Long id, Integer versionId) {

        ContractTemplate contractTemplate = templateRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Template not found by given id: %s".formatted(id)));
        if (contractTemplate.getStatus() == ContractTemplateStatus.DELETED &&
            !hasPermission(PermissionEnum.VIEW_DELETED_TEMPLATE.getId())) {
            throw new ClientException("You don't have permission to view Template;", ErrorCode.OPERATION_NOT_ALLOWED);
        }

        ContractTemplateDetail contractTemplateDetail = templateDetailsRepository.findByTemplateIdAndVersion(id, versionId)
                .orElseThrow(() -> new DomainEntityNotFoundException("Template detail not found for template id: %s".formatted(id)));

        TemplateViewResponse response = new TemplateViewResponse(contractTemplateDetail, contractTemplate);
        Long templateFileId = contractTemplateDetail.getTemplateFileId();
        if (Objects.nonNull(templateFileId)) {
            TemplateFileResponse fileResponse = new TemplateFileResponse(templateFileRepository.findByIdAndStatus(templateFileId, EntityStatus.ACTIVE)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Template file not found by given id: %s".formatted(templateFileId))));
            response.setFile(fileResponse);
        }
        response.setVersions(templateDetailsRepository.getTemplateVersions(id));
        return response;
    }

    /**
     * Edits an existing contract template and its associated details.
     *
     * @param id        The ID of the contract template to edit.
     * @param versionId The version ID of the contract template to edit.
     * @param request   The request containing the updated details for the contract template.
     * @return The ID of the edited contract template.
     * @throws DomainEntityNotFoundException if the contract template or its detail is not found.
     * @throws ClientException               if there are any validation errors in the request.
     */
    @Transactional
    public Long edit(Long id, Integer versionId, TemplateEditRequest request) {
        ContractTemplate template = templateRepository.findByIdAndStatusIn(id, List.of(ContractTemplateStatus.ACTIVE, ContractTemplateStatus.INACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("Template not found by id: %s".formatted(id)));

        List<String> errorMessages = new ArrayList<>();

        TemplateRequestValidatorUtil.validateRequest(template.getTemplatePurpose(), request, errorMessages);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);

        validateNameOnEdit(request.getName(), template.getId());
        validateDefaultCheckbox(request, template);
        updateTemplate(template, request);
        if (!request.isSaveAsNewVersion()) {
            ContractTemplateDetail contractTemplateDetail = templateDetailsRepository.findByTemplateIdAndVersion(template.getId(), versionId)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Template Detail not found"));
            validateFile(request.getFileId(), request.getOutputFileFormat(), id);
            updateDetail(contractTemplateDetail, request);
        } else {
            validateStartDate(request.getStartDate(), id);
            validateFile(request.getFileId(), request.getOutputFileFormat(), id);
            ContractTemplateDetail templateDetail = createTemplateDetailOnEdit(request, template);
            template.setLastTemplateDetailId(templateDetail.getId());
        }
        //todo add check if template is used in any process
        return template.getId();
    }

    /**
     * Updates the properties of a {@link ContractTemplate} based on the provided {@link TemplateEditRequest}.
     *
     * @param template The {@link ContractTemplate} to be updated.
     * @param request  The {@link TemplateEditRequest} containing the updated properties.
     */
    private void updateTemplate(ContractTemplate template, TemplateEditRequest request) {
        template.setDefaultForGoodsOrderDocument(request.getDefaultGoodsOrderDocument());
        template.setDefaultForGoodsOrderEmail(request.getDefaultGoodsOrderEmail());
        template.setDefaultForLatePaymentFineEmail(request.getDefaultLatePaymentFineEmail());
        template.setDefaultForLatePaymentFineDocument(request.getDefaultLatePaymentFineDocument());
        template.setStatus(request.getTemplateStatus());
        templateRepository.saveAndFlush(template);
    }

    /**
     * Retrieves a paginated list of contract templates based on the provided {@link TemplateListingRequest}.
     * The list of templates returned is filtered based on the user's permissions and the request parameters.
     *
     * @param request The {@link TemplateListingRequest} containing the filtering and pagination criteria.
     * @return A {@link Page} of {@link TemplateListingResponse} representing the filtered and paginated list of contract templates.
     */
    public Page<TemplateListingResponse> list(TemplateListingRequest request) {
        List<ContractTemplateStatus> statuses = new ArrayList<>();
        if (CollectionUtils.isEmpty(request.statuses())) {
            if (hasPermission(PermissionEnum.VIEW_TEMPLATE.getId())) {
                statuses.add(ContractTemplateStatus.ACTIVE);
                statuses.add(ContractTemplateStatus.INACTIVE);
            }
            if (hasPermission(PermissionEnum.VIEW_DELETED_TEMPLATE.getId())) {
                statuses.add(ContractTemplateStatus.DELETED);
            }
        } else {
            if (hasPermission(PermissionEnum.VIEW_TEMPLATE.getId())) {
                statuses.addAll(request.statuses());
            }
        }

        return templateRepository.filter(EPBStringUtils.fromPromptToQueryParameter(request.prompt()),
                EPBListUtils.convertEnumListToDBEnumArray(request.customerTypes()),
                EPBListUtils.convertEnumListToDBEnumArray(request.consumptionPurposes()),
                EPBListUtils.convertEnumListToDBEnumArray(request.outputFileFormats()),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(request.templatePurposes()),
                EPBListUtils.convertEnumListToDBEnumArray(request.fileSignings()),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(request.languages()),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(request.types()),
                ExcludeVersions.getExcludeVersionFromCheckBoxes(request.excludeOldVersions(), request.excludeFutureVersions()).getValue(),
                EPBListUtils.convertEnumListIntoStringListIfNotNull(statuses),
                Objects.equals(request.defaultGoodsOrderDocument(), Boolean.TRUE) ? true : null,
                Objects.equals(request.defaultGoodsOrderEmail(), Boolean.TRUE) ? true : null,
                Objects.equals(request.defaultLatePaymentFineDocument(), Boolean.TRUE) ? true : null,
                Objects.equals(request.defaultLatePaymentFineEmail(), Boolean.TRUE) ? true : null,
                Objects.requireNonNullElse(request.searchBy(), TemplateListingRequest.TemplateSearchBy.ALL).name(),
                PageRequest.of(request.page(), request.size(), Sort.by(Objects.requireNonNullElse(
                                request.sortDirection(), Sort.Direction.DESC),
                        Objects.requireNonNullElse(request.sortBy(), TemplateListingRequest.TemplateSortBy.CREATION_DATE).getColumnName()
                ))
        ).map(TemplateListingResponse::new);
    }


    /**
     * Validates the default checkbox settings for the contract template.
     * This method ensures that only one template can be set as the default for each of the following:
     * - Goods Order Document
     * - Goods Order Email
     * - Late Payment Fine Document
     * - Late Payment Fine Email
     * <p>
     * If a default template already exists for any of these settings, and the current request does not explicitly set the template as the default, a conflict exception is thrown.
     * If a default template already exists, it is set to non-default before the new default template is set.
     *
     * @param request  The template request containing the default checkbox settings.
     * @param template The contract template being updated or created. Can be null for new templates.
     * @throws ClientException if a conflict is detected with an existing default template.
     */
    private void validateDefaultCheckbox(TemplateBaseRequest request, ContractTemplate template) {
        String exceptionMessage = "Another default template exists in system. Are you sure?";

        if (Objects.equals(request.getDefaultGoodsOrderDocument(), Boolean.TRUE)) {
            Optional<ContractTemplate> defaultTemplateOptional = templateRepository.findDefaultForGoodsOrderDocument();
            if (!Objects.equals(request.getSetDefault(), Boolean.TRUE) && ((defaultTemplateOptional.isPresent()))) {
                if (template == null || (!template.getId().equals(defaultTemplateOptional.get().getId()))) {
                    throw new ClientException(exceptionMessage, ErrorCode.CONFLICT);
                }
            }
            defaultTemplateOptional.ifPresent(t -> t.setDefaultForGoodsOrderDocument(false));
        }

        if (Objects.equals(request.getDefaultGoodsOrderEmail(), Boolean.TRUE)) {
            Optional<ContractTemplate> defaultTemplateOptional = templateRepository.findDefaultForGoodsOrderEmail();
            if (!Objects.equals(request.getSetDefault(), Boolean.TRUE) && (defaultTemplateOptional.isPresent())) {
                if (template == null || (!template.getId().equals(defaultTemplateOptional.get().getId()))) {
                    throw new ClientException(exceptionMessage, ErrorCode.CONFLICT);
                }
            }
            defaultTemplateOptional.ifPresent(t -> t.setDefaultForGoodsOrderEmail(false));
        }

        if (Objects.equals(request.getDefaultLatePaymentFineDocument(), Boolean.TRUE)) {
            Optional<ContractTemplate> defaultTemplateOptional = templateRepository.findDefaultForLatePaymentFineDocument();
            if (!Objects.equals(request.getSetDefault(), Boolean.TRUE) && (defaultTemplateOptional.isPresent())) {
                if (template == null || (!template.getId().equals(defaultTemplateOptional.get().getId()))) {
                    throw new ClientException(exceptionMessage, ErrorCode.CONFLICT);
                }
            }
            defaultTemplateOptional.ifPresent(t -> t.setDefaultForLatePaymentFineDocument(false));
        }

        if (Objects.equals(request.getDefaultLatePaymentFineEmail(), Boolean.TRUE)) {
            Optional<ContractTemplate> defaultTemplateOptional = templateRepository.findDefaultForLatePaymentFineEmail();
            if (!Objects.equals(request.getSetDefault(), Boolean.TRUE) && (defaultTemplateOptional.isPresent())) {
                if (template == null || (!template.getId().equals(defaultTemplateOptional.get().getId()))) {
                    throw new ClientException(exceptionMessage, ErrorCode.CONFLICT);
                }
            }
            defaultTemplateOptional.ifPresent(t -> t.setDefaultForLatePaymentFineEmail(false));
        }

    }

    /**
     * Checks if the current user has the specified permission for the template context.
     *
     * @param id The permission ID to check.
     * @return True if the user has the specified permission, false otherwise.
     */
    private boolean hasPermission(String id) {
        return permissionService.getPermissionsFromContext(PermissionContextEnum.TEMPLATE).contains(id);
    }

    /**
     * Validates the provided template file by checking its format and availability.
     *
     * @param fileId     The ID of the template file to validate.
     * @param fileFormat The list of allowed file formats for the template.
     * @param templateId The ID of the template the file is associated with (optional).
     * @throws DomainEntityNotFoundException If the template file with the given ID does not exist or is attached to another template.
     * @throws ClientException               If the file format is not valid or there was an error downloading the file.
     */
    private void validateFile(Long fileId, List<ContractTemplateFileFormat> fileFormat, Long templateId) {
        ContractTemplateFiles templateFile = templateId == null ? templateFileRepository.findByIdAndStatusAndNotAttachedToTemplate(fileId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("Template File with given id %s does not exist or is attached to other template".formatted(fileId))) :
                templateFileRepository.findByIdAndStatusAndIsNotAttachedToOtherTemplate(fileId, EntityStatus.ACTIVE, templateId)
                        .orElseThrow(() -> new ClientException("Template File with id %s does not exist or is attached to other Template".formatted(fileId), ILLEGAL_ARGUMENTS_PROVIDED));
        String name = templateFile.getName();
        try {
            fileService.downloadFile(templateFile.getFileUrl());
        } catch (Exception exception) {
            log.error("Could not fetch template file", exception);
            throw new ClientException("Could not fetch template file", APPLICATION_ERROR);
        }
        if (!fileIsValidFormat(fileFormat, name)) {
            throw new IllegalArgumentsProvidedException("Provided File Format is not valid");
        }
    }

    /**
     * Checks if the provided file name has a valid format based on the allowed file formats.
     *
     * @param fileFormat The list of allowed file formats for the template.
     * @param name       The name of the file to be validated.
     * @return True if the file name has a valid format, false otherwise.
     */
    private boolean fileIsValidFormat(List<ContractTemplateFileFormat> fileFormat, String name) {
        if (!CollectionUtils.isEmpty(fileFormat)) {
            return (fileFormat.contains(ContractTemplateFileFormat.XLSX) && name.endsWith(".xlsx") ||
                    ((fileFormat.contains(ContractTemplateFileFormat.DOCX) || fileFormat.contains(ContractTemplateFileFormat.PDF))
                     && name.endsWith(".docx")));
        }
        return name.endsWith(".docx");
    }

    /**
     * Validates that a template with the given name does not already exist in the active state.
     *
     * @param name The name of the template to validate.
     * @throws ClientException if a template with the given name already exists in the active state.
     */
    private void validateName(String name) {
        if (templateDetailsRepository.existsByNameAndTemplateStatus(name, ContractTemplateStatus.ACTIVE)) {
            log.error("template with given name - {} already exists", name);
            throw new ClientException("template with given name - %s already exists".formatted(name), ErrorCode.CONFLICT);
        }
    }

    /**
     * Creates a new contract template detail based on the provided template creation request.
     *
     * @param request  The template creation request containing the details to create the template detail.
     * @param template The contract template to associate the detail with.
     * @return The newly created contract template detail.
     */
    private ContractTemplateDetail createTemplateDetail(TemplateCreateRequest request, ContractTemplate template) {
        return templateDetailsRepository.saveAndFlush(ContractTemplateDetail.builder()
                .templateType(request.getTemplateType())
                .subject(request.getSubject())
                .consumptionPurposes(request.getConsumptionPurposes())
                .customerType(request.getCustomerTypes())
                .templateFileId(request.getFileId())
                .language(request.getLanguage())
                .fileName(request.getFileNames())
                .fileNamePrefix(request.getFileNamePrefix())
                .fileNameSuffix(request.getFileNameSuffix())
                .name(request.getName())
                .startDate(LocalDate.now())
                .outputFileFormat(request.getOutputFileFormat())
                .fileSigning(request.getFileSignings())
                .quantity(request.getQuantity())
                .templateId(template.getId())
                .version(1)
                .build());
    }

    /**
     * Creates a new contract template with the provided details.
     *
     * @param request The template creation request containing the details to create the template.
     * @return The newly created contract template.
     */
    private ContractTemplate createTemplate(TemplateCreateRequest request) {
        ContractTemplate template = new ContractTemplate();
        template.setStatus(request.getTemplateStatus());
        template.setTemplatePurpose(request.getTemplatePurpose());
        template.setDefaultForGoodsOrderEmail(request.getDefaultGoodsOrderEmail());
        template.setDefaultForGoodsOrderDocument(request.getDefaultGoodsOrderDocument());
        template.setDefaultForLatePaymentFineDocument(request.getDefaultLatePaymentFineDocument());
        template.setDefaultForLatePaymentFineEmail(request.getDefaultLatePaymentFineEmail());
        return templateRepository.saveAndFlush(template);
    }

    /**
     * Uploads a template file to the system and returns a response containing the uploaded file details.
     *
     * @param file        The file to be uploaded.
     * @param fileFormats The allowed file formats for the upload.
     * @return A TemplateFileResponse containing the details of the uploaded file.
     * @throws IllegalArgumentsProvidedException If the file name is not present or the file format is invalid.
     * @throws ClientException                   If the file has an invalid format.
     */
    public TemplateFileResponse uploadFile(MultipartFile file, List<ContractTemplateFileFormat> fileFormats) {
        log.debug("Uploading template file {}.", file.getName());

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            log.error("Template file name is not present.");
            throw new IllegalArgumentsProvidedException("Template file name is not present");
        }
        if (!CollectionUtils.isEmpty(fileFormats)) {
            if (fileFormats.size() > 1 && fileFormats.contains(ContractTemplateFileFormat.XLSX)) {
                log.error("File format must be either XLSX or PDF/DOCX");
                throw new IllegalArgumentsProvidedException("File format must be either XLSX or PDF/DOCX");
            }
            if ((fileFormats.contains(ContractTemplateFileFormat.PDF) || fileFormats.contains(ContractTemplateFileFormat.DOCX))) {
                EPBDocxUtils.validateFileFormat(file);
            } else if (fileFormats.contains(ContractTemplateFileFormat.XLSX) && (!ExcelHelper.isXLSX(file))) {
                log.error("File has invalid format");
                throw new ClientException("Invalid file format", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        } else {
            EPBDocxUtils.validateFileFormat(file);
        }

        String fileName = String.format("%s_%s", UUID.randomUUID(), originalFilename.replaceAll("\\s+", ""));
        String path = String.format("%s/%s/%s", ftpBasePath, FOLDER_PATH, LocalDate.now());
        String url = fileService.uploadFile(file, path, fileName);

        ContractTemplateFiles files = new ContractTemplateFiles();
        files.setName(fileName);
        files.setFileUrl(url);
        files.setStatus(EntityStatus.ACTIVE);
        templateFileRepository.saveAndFlush(files);

        return new TemplateFileResponse(files);
    }

    /**
     * Finds available contract templates based on the provided search criteria.
     *
     * @param prompt           The search prompt to filter templates by name or description.
     * @param templatePurposes The purposes to filter templates by.
     * @param languages        The languages to filter templates by.
     * @param types            The types to filter templates by.
     * @param size             The page size for the result.
     * @param page             The page number for the result.
     * @return A page of contract template short responses matching the search criteria.
     */
    public Page<ContractTemplateShortResponse> findAvailable(String prompt, ContractTemplatePurposes templatePurposes, List<ContractTemplateLanguage> languages, List<ContractTemplateType> types, Integer size, Integer page) {
        return templateRepository
                .findAvailable(
                        EPBStringUtils.fromPromptToQueryParameter(prompt),
                        templatePurposes,
                        LocalDate.now(),
                        languages,
                        types,
                        PageRequest.of(page, size));
    }

    /**
     * Downloads a contract template file by its ID.
     *
     * @param id The ID of the contract template file to download.
     * @return A TemplateFileContent object containing the file name and the downloaded file content.
     * @throws DomainEntityNotFoundException if the contract template file with the given ID does not exist or is not active.
     */
    public TemplateFileContent downloadTemplateFile(Long id) {
        ContractTemplateFiles templateFile = templateFileRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new DomainEntityNotFoundException("Template File with given id %s does not exist".formatted(id)));
        return new TemplateFileContent(templateFile.getName(), fileService.downloadFile(templateFile.getFileUrl()));
    }

    /**
     * Creates a new contract template detail on edit.
     *
     * @param request  The template edit request containing the details to create the new template detail.
     * @param template The contract template to associate the new detail with.
     * @return The newly created contract template detail.
     */
    private ContractTemplateDetail createTemplateDetailOnEdit(TemplateEditRequest request, ContractTemplate template) {
        return templateDetailsRepository.saveAndFlush(ContractTemplateDetail.builder()
                .templateType(request.getTemplateType())
                .subject(request.getSubject())
                .consumptionPurposes(request.getConsumptionPurposes())
                .customerType(request.getCustomerTypes())
                .templateFileId(request.getFileId())
                .language(request.getLanguage())
                .fileName(request.getFileNames())
                .fileNamePrefix(request.getFileNamePrefix())
                .fileNameSuffix(request.getFileNameSuffix())
                .name(request.getName())
                .startDate(LocalDate.now())
                .outputFileFormat(request.getOutputFileFormat())
                .fileSigning(request.getFileSignings())
                .quantity(request.getQuantity())
                .templateId(template.getId())
                .version(templateDetailsRepository.getMaxTemplateVersion(template.getId()) + 1)
                .startDate(request.getStartDate())
                .build());
    }

    /**
     * Validates the start date of a contract template detail to ensure it is not before the start date of the first version and does not conflict with the start date of any existing version.
     *
     * @param startDate The start date to validate.
     * @param id        The ID of the contract template detail.
     * @throws ClientException if the start date is before the start date of the first version or conflicts with an existing version.
     */
    private void validateStartDate(LocalDate startDate, Long id) {
        if (templateDetailsRepository.startDateBeforeFirstVersionStartDate(startDate, id)) {
            log.error("Start Date must not be before the Start Date of the First Version");
            throw new ClientException("Start Date must not be before the Start Date of the First Version", ErrorCode.CONFLICT);
        }
        if (templateDetailsRepository.startDateEqualToAnyVersionStartDate(startDate, id)) {
            log.error("Template version with selected Start Date already exists");
            throw new ClientException("Template version with selected Start Date already exists", ErrorCode.CONFLICT);
        }
    }

    /**
     * Validates that a template detail with the given name does not already exist in an active template.
     *
     * @param name The name to validate.
     * @param id   The ID of the template detail being edited.
     * @throws ClientException if a template detail with the given name already exists in an active template.
     */
    private void validateNameOnEdit(String name, Long id) {
        if (templateDetailsRepository.existsByNameAndStatusInOtherTemplate(name, ContractTemplateStatus.ACTIVE, id)) {
            log.error("template with given name - {} already exists", name);
            throw new ClientException("template with given name - %s already exists".formatted(name), ErrorCode.CONFLICT);
        }
    }

    /**
     * Updates the details of a contract template.
     *
     * @param detail  The contract template detail to update.
     * @param request The request containing the updated details.
     */
    private void updateDetail(ContractTemplateDetail detail, TemplateEditRequest request) {
        detail.setTemplateType(request.getTemplateType());
        detail.setSubject(request.getSubject());
        detail.setConsumptionPurposes(request.getConsumptionPurposes());
        detail.setCustomerType(request.getCustomerTypes());
        detail.setTemplateFileId(request.getFileId());
        detail.setLanguage(request.getLanguage());
        detail.setFileName(request.getFileNames());
        detail.setFileNamePrefix(request.getFileNamePrefix());
        detail.setFileNameSuffix(request.getFileNameSuffix());
        detail.setName(request.getName());
        detail.setOutputFileFormat(request.getOutputFileFormat());
        detail.setFileSigning(request.getFileSignings());
        detail.setQuantity(request.getQuantity());
        templateDetailsRepository.saveAndFlush(detail);
    }

    /**
     * Deletes a contract template by setting its status to 'DELETED'. The method checks if the template is already deleted or connected to any other object, and throws appropriate exceptions if either of these conditions is true.
     *
     * @param id The ID of the contract template to be deleted.
     * @return The ID of the deleted contract template.
     * @throws DomainEntityNotFoundException if the template with the given ID is not found.
     * @throws OperationNotAllowedException  if the template is already deleted or connected to another object.
     */
    @Transactional
    public Long delete(Long id) {
        ContractTemplate contractTemplate = templateRepository.findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("Template not found by id %s".formatted(id)));
        if (contractTemplate.getStatus().equals(ContractTemplateStatus.DELETED)) {
            log.error("Template with id: {} is already deleted", id);
            throw new OperationNotAllowedException("Template is already deleted");
        }
        Optional<String> connectedToAnyObject = templateRepository.findConnectionToAnyObject(id);
        if (connectedToAnyObject.isPresent()) {
            String object = connectedToAnyObject.get();
            log.debug("template with id: {} has connection to {}", id, object);
            throw new OperationNotAllowedException("You can’t delete the template because it is connected to the %s.".formatted(object));

        }

        contractTemplate.setStatus(ContractTemplateStatus.DELETED);
        return contractTemplate.getId();
    }
}
