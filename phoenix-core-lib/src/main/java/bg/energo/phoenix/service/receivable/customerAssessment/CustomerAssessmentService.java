package bg.energo.phoenix.service.receivable.customerAssessment;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.customer.AccountManager;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.nomenclature.receivable.CustomerAssessmentCriteria;
import bg.energo.phoenix.model.entity.nomenclature.receivable.CustomerAssessmentType;
import bg.energo.phoenix.model.entity.receivable.customerAssessment.*;
import bg.energo.phoenix.model.entity.receivable.rescheduling.Rescheduling;
import bg.energo.phoenix.model.entity.task.Task;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.receivable.customerAssessment.Assessment;
import bg.energo.phoenix.model.enums.receivable.customerAssessment.AssessmentCriteriaNamesWithoutValues;
import bg.energo.phoenix.model.enums.receivable.customerAssessment.AssessmentStatus;
import bg.energo.phoenix.model.enums.receivable.rescheduling.ReschedulingStatus;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import bg.energo.phoenix.model.enums.task.TaskConnectionType;
import bg.energo.phoenix.model.request.communication.edms.Attribute;
import bg.energo.phoenix.model.request.receivable.customerAssessment.*;
import bg.energo.phoenix.model.response.receivable.customerAssessment.*;
import bg.energo.phoenix.model.response.shared.FileWithStatusesResponse;
import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.model.response.task.TaskShortResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.permissions.PermissionEnum;
import bg.energo.phoenix.repository.customer.AccountManagerRepository;
import bg.energo.phoenix.repository.customer.CustomerDetailsRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.receivable.AdditionalConditionRepository;
import bg.energo.phoenix.repository.nomenclature.receivable.CustomerAssessmentCriteriaRepository;
import bg.energo.phoenix.repository.nomenclature.receivable.CustomerAssessmentTypeRepository;
import bg.energo.phoenix.repository.receivable.customerAssessment.*;
import bg.energo.phoenix.repository.receivable.rescheduling.ReschedulingRepository;
import bg.energo.phoenix.repository.task.TaskRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.archivation.edms.EDMSFileArchivationService;
import bg.energo.phoenix.service.archivation.edms.FileArchivationService;
import bg.energo.phoenix.service.archivation.edms.config.EDMSAttributeProperties;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.service.task.TaskService;
import bg.energo.phoenix.util.archivation.EDMSArchivationConstraints;
import bg.energo.phoenix.util.epb.EPBChainedExceptionTriggerUtil;
import bg.energo.phoenix.util.epb.EPBListUtils;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static bg.energo.phoenix.util.epb.EPBFinalFields.CUSTOMER_ASSESSMENT_NUMBER_PREFIX;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAssessmentService {
    private final FileService fileService;
    private final PermissionService permissionService;
    private final CustomerAssessmentRepository customerAssessmentRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDetailsRepository customerDetailsRepository;
    private final CustomerAssessmentCommentsRepository customerAssessmentCommentsRepository;
    private final CustomerAssessmentFilesRepository customerAssessmentFilesRepository;
    private final ReschedulingRepository reschedulingRepository;
    private final CustomerAssessmentAddConditionRepository customerAssessmentAddConditionRepository;
    private final CustomerAssessmentParametersRepository customerAssessmentParametersRepository;
    private final AccountManagerRepository accountManagerRepository;
    private final CustomerAssessmentCriteriaRepository customerAssessmentCriteriaRepository;
    private final AdditionalConditionRepository additionalConditionRepository;
    private final CustomerAssessmentTypeRepository customerAssessmentTypeRepository;
    private final TaskRepository taskRepository;
    private final TaskService taskService;
    private final CustomerAssessmentTasksRepository customerAssessmentTasksRepository;
    private final EDMSFileArchivationService archivationService;
    private final EDMSAttributeProperties attributeProperties;
    private final FileArchivationService fileArchivationService;

    @Value("${ftp.server.base.path}")
    private String ftpBasePath;

    /**
     * Creates a new customer assessment and saves it to the database.
     *
     * @param request the request object containing the details of the new customer assessment
     * @return the ID of the newly created customer assessment
     * @throws DomainEntityNotFoundException if the customer with the provided ID is not found
     * @throws ClientException               if there are any errors during the creation process
     */
    @Transactional
    public Long create(CustomerAssessmentBaseRequest request) {
        List<String> errorMessages = new ArrayList<>();
        CustomerAssessment customerAssessment = new CustomerAssessment();
        Long nextSequenceValue = generateAndSetNumberAndId(customerAssessment);
        customerAssessment.setAssessmentStatus(request.getStatus());
        Optional<Customer> customerOptional = customerRepository.findByIdAndStatuses(request.getCustomerId(), List.of(CustomerStatus.ACTIVE));
        if (customerOptional.isEmpty()) {
            throw new DomainEntityNotFoundException("customerId-Customer with id %s not found;".formatted(request.getCustomerId()));
        }
        customerAssessment.setCustomerId(request.getCustomerId());
        customerAssessment.setFinalAssessment(Assessment.YES.equals(request.getFinalAssessment()));
        customerAssessment.setStatus(EntityStatus.ACTIVE);
        if (customerAssessmentTypeRepository.existsByIdAndStatusIn(request.getTypeId(), List.of(NomenclatureItemStatus.ACTIVE))) {
            customerAssessment.setCustomerAssessmentTypeId(request.getTypeId());
        } else {
            errorMessages.add("typeId-type with id %s not found;".formatted(request.getTypeId()));
        }

        if (CollectionUtils.isNotEmpty(request.getFiles())) {
            setCustomerAssessmentIdToFiles(request, nextSequenceValue, errorMessages);
        }
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        customerAssessmentRepository.saveAndFlush(customerAssessment);
        if (CollectionUtils.isNotEmpty(request.getAdditionalConditions())) {
            saveAdditionalConditions(request, nextSequenceValue, errorMessages);
        }
        saveCustomerAssessmentParameters(request, nextSequenceValue, errorMessages);
        saveNewCustomerAssessmentComment(nextSequenceValue, request.getNewComment());
        archiveFile(customerAssessment, customerOptional);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        return customerAssessment.getId();
    }

    /**
     * Archives the files associated with a customer assessment.
     * This method retrieves all active customer assessment files for the given customer assessment, and then archives them using the provided EDMS archivation service. The method sets various attributes on the archived files, such as the document type, document number, document date, customer identifier, and customer number.
     * If any errors occur during the archivation process, they are logged but not propagated further.
     *
     * @param customerAssessment the customer assessment for which to archive the files
     * @param customerOptional   the optional customer associated with the customer assessment
     */
    private void archiveFile(CustomerAssessment customerAssessment, Optional<Customer> customerOptional) {
        List<CustomerAssessmentFiles> customerAssessmentFiles = customerAssessmentFilesRepository.findAllByCustomerAssessmentIdAndStatus(customerAssessment.getId(), EntityStatus.ACTIVE).orElse(new ArrayList<>());

        if (CollectionUtils.isNotEmpty(customerAssessmentFiles)) {
            for (CustomerAssessmentFiles customerAssessmentFile : customerAssessmentFiles) {
                try {
                    customerAssessmentFile.setAttributes(
                            List.of(
                                    new Attribute(attributeProperties.getDocumentTypeGuid(), EDMSArchivationConstraints.DOCUMENT_TYPE_CUSTOMER_ASSESSMENT_FILE),
                                    new Attribute(attributeProperties.getDocumentNumberGuid(), customerAssessment.getAssessmentNumber()),
                                    new Attribute(attributeProperties.getDocumentDateGuid(), LocalDateTime.now()),
                                    new Attribute(attributeProperties.getCustomerIdentifierGuid(), customerOptional.map(Customer::getIdentifier).orElse("")),
                                    new Attribute(attributeProperties.getCustomerNumberGuid(), customerOptional.isPresent() ? customerOptional.get().getCustomerNumber() : ""),
                                    new Attribute(attributeProperties.getSignedGuid(), false)
                            )
                    );
                    customerAssessmentFile.setNeedArchive(true);
                    customerAssessmentFile.setArchivationConstraints(EDMSArchivationConstraints.DOCUMENT_TYPE_CUSTOMER_ASSESSMENT_FILE);

                    fileArchivationService.archive(customerAssessmentFile);
                } catch (Exception e) {
                    log.error("Cannot archive file: ", e);
                }
            }
        }
    }

    /**
     * Retrieves a customer assessment by its ID and returns a detailed response.
     * This method fetches a customer assessment by its ID from the repository. If the customer assessment is deleted, it checks if the user has the necessary permission to view deleted customer assessments. If the user does not have the permission, an OperationNotAllowedException is thrown.
     * The method then maps the customer assessment to a CustomerAssessmentResponse object, which includes the following information:
     * - Customer assessment details
     * - Customer assessment comments and their history
     * - Customer details
     * - Customer assessment type
     * - Additional conditions
     * - Tasks
     * - Parameters information
     * - File information
     * - Rescheduling information
     *
     * @param id the ID of the customer assessment to retrieve
     * @return a CustomerAssessmentResponse object containing the detailed information about the customer assessment
     * @throws DomainEntityNotFoundException if the customer assessment with the given ID is not found
     * @throws OperationNotAllowedException  if the user does not have permission to view a deleted customer assessment
     */
    public CustomerAssessmentResponse view(Long id) {
        CustomerAssessment customerAssessment = customerAssessmentRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Customer Assessment with id %s not found".formatted(id)));

        if (customerAssessment.getStatus().equals(EntityStatus.DELETED)) {
            if (!permissionService.permissionContextContainsPermissions(PermissionContextEnum.CUSTOMER_ASSESSMENT, List.of(PermissionEnum.CUSTOMER_ASSESSMENT_VIEW_DELETED))) {
                throw new OperationNotAllowedException("you do not have permission to view deleted customer assessment;");
            }
        }

        CustomerAssessmentResponse response = new CustomerAssessmentResponse(customerAssessment);
        List<CustomerAssessmentComments> customerAssessmentComments = customerAssessmentCommentsRepository.findAllByCustomerAssessmentId(id);
        List<CustomerAssessmentCommentHistory> commentHistories = mapCustomerAssessmentCommentHistoryResponse(customerAssessmentComments, accountManagerRepository);
        response.setComments(commentHistories);

        Optional<Customer> customerOptional = customerRepository.findByIdAndStatuses(customerAssessment.getCustomerId(), List.of(CustomerStatus.ACTIVE));
        if (customerOptional.isPresent()) {
            Customer customer = customerOptional.get();
            Long lastCustomerDetailId = customer.getLastCustomerDetailId();
            Optional<ShortResponse> customerDetails = customerDetailsRepository.findByCustomerDetailsIdTemp(lastCustomerDetailId);
            customerDetails.ifPresent(response::setCustomer);
        }

        Optional<CustomerAssessmentType> assessmentType = customerAssessmentTypeRepository.findById(customerAssessment.getCustomerAssessmentTypeId());
        assessmentType.ifPresent(customerAssessmentType -> response.setCustomerAssessmentType(new ShortResponse(customerAssessmentType.getId(), customerAssessmentType.getName())));
        response.setAdditionalConditions(customerAssessmentAddConditionRepository.findAllByCustomerAssessmentId(id));
        response.setTasks(getTasksByCustomerAssessmentId(id));

        findAndSetParametersInfo(response);
        findAndSetFileInfo(response);
        findAndSetRescheduling(response);
        return response;
    }

    /**
     * Deletes a customer assessment with the specified ID.
     *
     * @param id the ID of the customer assessment to delete
     * @return the ID of the deleted customer assessment
     * @throws DomainEntityNotFoundException if the customer assessment with the given ID is not found
     * @throws OperationNotAllowedException  if the customer assessment is already deleted or has a final status
     */
    public Long delete(Long id) {
        CustomerAssessment customerAssessment = customerAssessmentRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Customer Assessment with id %s not found".formatted(id)));

        if (customerAssessment.getStatus().equals(EntityStatus.DELETED)) {
            throw new OperationNotAllowedException("Customer assessment with id %s is already deleted;".formatted(id));
        }

        if (customerAssessment.getAssessmentStatus().equals(AssessmentStatus.FINAL)) {
            throw new OperationNotAllowedException("Can not delete Customer assessment with final status;");
        }

        if (reschedulingRepository.existsByCustomerAssessmentIdAndStatus(id, EntityStatus.ACTIVE)) {
            throw new OperationNotAllowedException("Can not delete Customer assessment because it is linked to a rescheduling;");
        }

        customerAssessment.setStatus(EntityStatus.DELETED);
        customerAssessmentRepository.save(customerAssessment);
        return id;
    }

    /**
     * Filters and retrieves a page of customer assessment listing responses based on the provided request parameters.
     *
     * @param request the request object containing the filter criteria
     * @return a page of customer assessment listing responses
     */
    public Page<CustomerAssessmentListingResponse> filter(CustomerAssessmentListingRequest request) {
        List<EntityStatus> statuses = new ArrayList<>();

        Sort.Order order = new Sort.Order(Objects.requireNonNullElse(request.getDirection(), Sort.Direction.DESC), checkSortField(request));

        return customerAssessmentRepository
                .filter(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(request.getAssessmentStatuses()),
                        EPBListUtils.convertEnumListIntoStringListIfNotNull(getStatusesByPermission(statuses)),
                        request.getCreateDateFrom(),
                        request.getCreateDateTo(),
                        getFinalAssessments(request),
                        Objects.requireNonNullElse(request.getAssessmentTypeIds(), new ArrayList<>()),
                        getSearchByEnum(request),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize(),
                                Sort.by(order))
                )
                .map(CustomerAssessmentListingResponse::new);
    }

    /**
     * Edits an existing customer assessment with the specified ID.
     *
     * @param id          the ID of the customer assessment to edit
     * @param editRequest the request object containing the updated customer assessment details
     * @return the ID of the edited customer assessment
     * @throws DomainEntityNotFoundException if the customer assessment with the given ID is not found
     * @throws OperationNotAllowedException  if the customer assessment is already deleted or has a final status
     */
    @Transactional
    public Long edit(Long id, CustomerAssessmentBaseRequest editRequest) {
        List<String> errorMessages = new ArrayList<>();
        CustomerAssessment customerAssessment = customerAssessmentRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Customer Assessment with id %s not found".formatted(id)));

        if (customerAssessment.getStatus().equals(EntityStatus.DELETED)) {
            throw new OperationNotAllowedException("Can not edit deleted Customer assessment;");
        }

        if (customerAssessment.getAssessmentStatus().equals(AssessmentStatus.FINAL)) {
            throw new OperationNotAllowedException("Can not edit Customer assessment with Final status;");
        }

        customerAssessment.setAssessmentStatus(editRequest.getStatus());
        if (!editRequest.getCustomerId().equals(customerAssessment.getCustomerId())) {
            Optional<Customer> customerOptional = customerRepository.findByIdAndStatuses(editRequest.getCustomerId(), List.of(CustomerStatus.ACTIVE));
            if (customerOptional.isEmpty()) {
                throw new DomainEntityNotFoundException("customerId-Customer with id %s not found;".formatted(editRequest.getCustomerId()));
            }
            customerAssessment.setCustomerId(editRequest.getCustomerId());
        }

        customerAssessment.setFinalAssessment(Assessment.YES.equals(editRequest.getFinalAssessment()));
        if (!editRequest.getTypeId().equals(customerAssessment.getCustomerAssessmentTypeId())) {
            if (customerAssessmentTypeRepository.existsByIdAndStatusIn(editRequest.getTypeId(), List.of(NomenclatureItemStatus.ACTIVE))) {
                customerAssessment.setCustomerAssessmentTypeId(editRequest.getTypeId());
            } else {
                errorMessages.add("typeId-type with id %s not found;".formatted(editRequest.getTypeId()));
            }
        }

        addAndDeleteFiles(editRequest, id);
        if (CollectionUtils.isNotEmpty(editRequest.getFiles())) {
            setCustomerAssessmentIdToFiles(editRequest, id, errorMessages);
        }

        addAndDeleteAdditionalConditions(editRequest, id);
        if (CollectionUtils.isNotEmpty(editRequest.getAdditionalConditions())) {
            saveAdditionalConditions(editRequest, id, errorMessages);
        }

        updateParameters(editRequest, id, errorMessages);
        addEditTaskToCustomerAssessment(customerAssessment, editRequest.getTaskIds(), errorMessages);
        saveNewCustomerAssessmentComment(id, editRequest.getNewComment());

        Optional<Customer> customerOptional = customerRepository.findById(customerAssessment.getCustomerId());
        archiveFile(customerAssessment, customerOptional);
        EPBChainedExceptionTriggerUtil.throwExceptionIfRequired(errorMessages, log);
        customerAssessmentRepository.save(customerAssessment);
        return id;
    }

    /**
     * Retrieves a list of short responses for customer assessments that are eligible for rescheduling.
     *
     * @param customerId the ID of the customer whose assessments should be retrieved
     * @return a list of {@link CustomerAssessmentShortResponse} objects representing the customer assessments
     */
    public List<CustomerAssessmentShortResponse> getCustomerAssessmentsForRescheduling(Long customerId) {
        return customerAssessmentRepository.getCustomerAssessmentsForRescheduling(customerId).stream().map(CustomerAssessmentShortResponse::new).toList();
    }

    /**
     * Saves a new comment for the specified customer assessment.
     *
     * @param customerAssessmentId the ID of the customer assessment to add the comment to
     * @param newComment           the text of the new comment to save
     */
    private void saveNewCustomerAssessmentComment(Long customerAssessmentId, String newComment) {
        if (StringUtils.isNotBlank(newComment)) {
            customerAssessmentCommentsRepository.save(new CustomerAssessmentComments(null, newComment, customerAssessmentId));
        }
    }

    /**
     * Uploads a file to the customer assessment file storage and saves the file metadata to the database.
     *
     * @param file     the file to be uploaded
     * @param statuses the statuses to be associated with the uploaded file
     * @return a response containing the saved file metadata and the display name of the system user who uploaded the file
     * @throws ClientException if the file name is null
     */
    @Transactional
    public FileWithStatusesResponse upload(MultipartFile file, List<DocumentFileStatus> statuses) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new ClientException("File name is null;", ErrorCode.APPLICATION_ERROR);
        }
        String formattedFileName = originalFilename.replaceAll("\\s+", "");
        String fileName = String.format("%s_%s", UUID.randomUUID(), formattedFileName);
        String fileUrl = String.format("%s/%s/%s", ftpBasePath, "customer_assessment_files", LocalDate.now());
        String url = fileService.uploadFile(file, fileUrl, fileName);

        CustomerAssessmentFiles customerAssessmentFiles = CustomerAssessmentFiles
                .builder()
                .name(formattedFileName)
                .localFileUrl(url)
                .fileStatuses(statuses)
                .status(EntityStatus.ACTIVE)
                .build();

        CustomerAssessmentFiles saved = customerAssessmentFilesRepository.saveAndFlush(customerAssessmentFiles);
        return new FileWithStatusesResponse(saved, accountManagerRepository.findByUserName(saved.getSystemUserId())
                .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse(""));
    }

    /**
     * Downloads a customer assessment file from the file service.
     *
     * @param id the ID of the customer assessment file to download
     * @return a {@link CustomerAssessmentFileContent} object containing the file content and name
     * @throws DomainEntityNotFoundException if the file with the given ID is not found
     */
    public CustomerAssessmentFileContent download(Long id) {
        CustomerAssessmentFiles customerAssessmentFiles = customerAssessmentFilesRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("File with presented id not found;"));

        ByteArrayResource resource = fileService.downloadFile(customerAssessmentFiles.getLocalFileUrl());

        return new CustomerAssessmentFileContent(customerAssessmentFiles.getName(), resource.getByteArray());
    }

    /**
     * Downloads a customer assessment file from the file service, handling the case where the file has been archived.
     *
     * @param id the ID of the customer assessment file to download
     * @return a {@link CustomerAssessmentFileContent} object containing the file content and name
     * @throws DomainEntityNotFoundException if the file with the given ID is not found
     * @throws Exception                     if there is an error downloading the archived file
     */
    public CustomerAssessmentFileContent checkForArchivationAndDownload(Long id) throws Exception {
        CustomerAssessmentFiles customerAssessmentFiles = customerAssessmentFilesRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("File with presented id not found;"));

        if (Boolean.TRUE.equals(customerAssessmentFiles.getIsArchived())) {
            if (Objects.isNull(customerAssessmentFiles.getLocalFileUrl())) {
                ByteArrayResource fileContent = archivationService.downloadArchivedFile(customerAssessmentFiles.getDocumentId(), customerAssessmentFiles.getFileId());

                return new CustomerAssessmentFileContent(fileContent.getFilename(), fileContent.getContentAsByteArray());
            }
        }

        ByteArrayResource resource = fileService.downloadFile(customerAssessmentFiles.getLocalFileUrl());

        return new CustomerAssessmentFileContent(customerAssessmentFiles.getName(), resource.getByteArray());
    }

    /**
     * Generates and sets the unique ID and assessment number for a CustomerAssessment.
     *
     * @param customerAssessment the CustomerAssessment to generate the ID and number for
     * @return the generated ID for the CustomerAssessment
     */
    private Long generateAndSetNumberAndId(CustomerAssessment customerAssessment) {
        Long nextSequenceValue = customerAssessmentRepository.getNextSequenceValue();
        String number = "%s%s".formatted(CUSTOMER_ASSESSMENT_NUMBER_PREFIX, nextSequenceValue);
        customerAssessment.setId(nextSequenceValue);
        customerAssessment.setAssessmentNumber(number);

        return nextSequenceValue;
    }

    /**
     * Adds or edits tasks associated with a customer assessment.
     * <p>
     * This method performs the following steps:
     * 1. Removes all tasks associated with the customer assessment that are not in the provided list of task IDs.
     * 2. Retrieves the active tasks from the database that match the provided task IDs and have a connection type of "CUSTOMER".
     * 3. Finds the existing customer assessment tasks associated with the customer assessment and the provided task IDs.
     * 4. Creates new customer assessment tasks for any tasks that don't already have an associated customer assessment task.
     * 5. Saves the new and updated customer assessment tasks to the database.
     *
     * @param updatedCustomerAssessment the customer assessment to update
     * @param taskIds                   the list of task IDs to associate with the customer assessment
     * @param errorMassages             a list to store any error messages that occur during the process
     */
    private void addEditTaskToCustomerAssessment(CustomerAssessment updatedCustomerAssessment, List<Long> taskIds, List<String> errorMassages) {
        log.info("Add/edit customer assessment task for customer assessment with id: %s;".formatted(updatedCustomerAssessment.getId()));

        if (!CollectionUtils.isEmpty(taskIds)) {
            removeAllTasksOtherThanRequestTaskIds(taskIds);
            List<Task> tasks = taskRepository.findByIdInAndStatusInAndConnectionType(taskIds, List.of(EntityStatus.ACTIVE), TaskConnectionType.CUSTOMER);
            Map<Long, Task> taskMap = tasks.stream().collect(Collectors.toMap(Task::getId, Function.identity()));
            List<CustomerAssessmentTasks> customerAssessmentTasks = customerAssessmentTasksRepository.findByCustomerAssessmentIdAndTaskIdInAndStatusIn(
                    updatedCustomerAssessment.getId(),
                    taskIds,
                    List.of(EntityStatus.ACTIVE));
            Map<Long, CustomerAssessmentTasks> customerAssessmentTasksMap = customerAssessmentTasks.stream()
                    .collect(Collectors.toMap(CustomerAssessmentTasks::getTaskId, Function.identity()));
            List<CustomerAssessmentTasks> newCustomerAssessmentTasks = new ArrayList<>();

            for (int i = 0; i < taskIds.size(); i++) {
                Long id = taskIds.get(i);
                Task task = taskMap.get(id);
                if (task != null) {
                    CustomerAssessmentTasks customerAssessmentTask = customerAssessmentTasksMap.get(id);
                    if (customerAssessmentTask != null) {
                        customerAssessmentTask.setModifyDate(LocalDateTime.now());
                    } else {
                        CustomerAssessmentTasks newCustomerAssessmentTask = createCustomerAssessmentTask(updatedCustomerAssessment, task);
                        newCustomerAssessmentTasks.add(newCustomerAssessmentTask);
                    }
                } else {
                    errorMassages.add("taskId[%s]-Can't find active Task with id:%s;".formatted(i, taskIds));
                }

                customerAssessmentTasksRepository.saveAll(newCustomerAssessmentTasks);
            }
        } else {
            List<CustomerAssessmentTasks> customerAssessmentTasksList =
                    customerAssessmentTasksRepository.findByCustomerAssessmentIdAndStatusIn(
                            updatedCustomerAssessment.getId(),
                            List.of(EntityStatus.ACTIVE));
            List<CustomerAssessmentTasks> customerAssessmentTasksToDelete = new ArrayList<>();
            if (!CollectionUtils.isEmpty(customerAssessmentTasksList)) {
                for (CustomerAssessmentTasks item : customerAssessmentTasksList) {
                    item.setStatus(EntityStatus.DELETED);
                    customerAssessmentTasksToDelete.add(item);
                }
                if (!CollectionUtils.isEmpty(customerAssessmentTasksToDelete)) {
                    customerAssessmentTasksRepository.saveAll(customerAssessmentTasksToDelete);
                }
            }
        }
    }

    /**
     * Removes all customer assessment tasks that are not associated with the provided task IDs.
     *
     * @param taskIds The list of task IDs to keep associated with the customer assessment.
     */
    private void removeAllTasksOtherThanRequestTaskIds(List<Long> taskIds) {
        List<CustomerAssessmentTasks> customerAssessmentTasksList =
                customerAssessmentTasksRepository.findByTaskIdNotInAndStatusIn(
                        taskIds,
                        List.of(EntityStatus.ACTIVE));
        List<CustomerAssessmentTasks> customerAssessmentTasksListToDelete = new ArrayList<>();
        if (!CollectionUtils.isEmpty(customerAssessmentTasksList)) {
            for (CustomerAssessmentTasks item : customerAssessmentTasksList) {
                item.setStatus(EntityStatus.DELETED);
                customerAssessmentTasksListToDelete.add(item);
            }
            customerAssessmentTasksRepository.saveAll(customerAssessmentTasksListToDelete);
        }
    }

    /**
     * Creates a new {@link CustomerAssessmentTasks} instance with the provided {@link CustomerAssessment} and {@link Task}.
     *
     * @param updatedCustomerAssessment The {@link CustomerAssessment} to associate the new task with.
     * @param task                      The {@link Task} to associate with the new {@link CustomerAssessmentTasks} instance.
     * @return A new {@link CustomerAssessmentTasks} instance with the provided {@link CustomerAssessment} and {@link Task}.
     */
    private CustomerAssessmentTasks createCustomerAssessmentTask(CustomerAssessment updatedCustomerAssessment, Task task) {
        CustomerAssessmentTasks customerAssessmentTasks = new CustomerAssessmentTasks();
        customerAssessmentTasks.setCustomerAssessmentId(updatedCustomerAssessment.getId());
        customerAssessmentTasks.setTaskId(task.getId());
        customerAssessmentTasks.setStatus(EntityStatus.ACTIVE);
        return customerAssessmentTasks;
    }

    /**
     * Retrieves a list of short task responses for the given customer assessment ID.
     *
     * @param id The ID of the customer assessment to retrieve tasks for.
     * @return A list of {@link TaskShortResponse} objects representing the tasks associated with the customer assessment.
     */
    public List<TaskShortResponse> getTasksByCustomerAssessmentId(Long id) {
        return taskService.getTasksByCustomerAssessmentId(id);
    }

    /**
     * Retrieves a list of customer assessment parameters for the given customer ID.
     *
     * @param customerId The ID of the customer to retrieve parameters for.
     * @return A list of {@link CustomerAssessmentMiddleResponse} objects representing the customer assessment parameters.
     */
    public List<CustomerAssessmentMiddleResponse> getParametersByCustomerId(Long customerId) {
        return customerAssessmentParametersRepository.getParametersByCustomerId(customerId);
    }

    /**
     * Retrieves a list of short customer assessment responses for the given customer ID and prompt.
     *
     * @param customerId The ID of the customer to retrieve assessments for.
     * @param prompt     The prompt to filter the customer assessments by.
     * @return A list of {@link CustomerAssessmentShortResponse} objects representing the customer assessments.
     */
    public List<CustomerAssessmentShortResponse> getCustomerAssessmentsByCustomerId(Long customerId, String prompt) {
        return customerAssessmentRepository.getCustomerAssessmentsByCustomerId(customerId, EPBStringUtils.fromPromptToQueryParameter(prompt)).stream().map(CustomerAssessmentShortResponse::new).toList();
    }

    /**
     * Sets the customer assessment ID for the files associated with the provided customer assessment request.
     *
     * @param request           The customer assessment request containing the file IDs.
     * @param nextSequenceValue The next sequence value for the customer assessment.
     * @param errorMessages     A list to store any error messages encountered during the operation.
     */
    private void setCustomerAssessmentIdToFiles(CustomerAssessmentBaseRequest request, Long nextSequenceValue, List<String> errorMessages) {
        for (Long fileId : request.getFiles()) {
            Optional<CustomerAssessmentFiles> file = customerAssessmentFilesRepository.findById(fileId);
            if (file.isPresent()) {
                file.get().setCustomerAssessmentId(nextSequenceValue);
                customerAssessmentFilesRepository.save(file.get());
            } else {
                errorMessages.add("files-file with id %s not found;".formatted(fileId));
            }
        }
    }

    /**
     * Saves the additional conditions associated with a customer assessment request.
     *
     * @param request           The customer assessment request containing the additional condition IDs.
     * @param nextSequenceValue The next sequence value for the customer assessment.
     * @param errorMessages     A list to store any error messages encountered during the operation.
     */
    private void saveAdditionalConditions(CustomerAssessmentBaseRequest request, Long nextSequenceValue, List<String> errorMessages) {
        for (Long additionalConditionId : request.getAdditionalConditions()) {
            if (additionalConditionRepository.existsByIdAndStatusIn(additionalConditionId, List.of(NomenclatureItemStatus.ACTIVE))) {
                customerAssessmentAddConditionRepository.save(new CustomerAssessmentAddCondition(null, nextSequenceValue, additionalConditionId, EntityStatus.ACTIVE));
            } else {
                errorMessages.add("additionalConditions-additionalConditions with id %s not found;".formatted(additionalConditionId));
            }
        }
    }

    /**
     * Saves the customer assessment parameters associated with a customer assessment request.
     *
     * @param request           The customer assessment request containing the parameter information.
     * @param nextSequenceValue The next sequence value for the customer assessment.
     * @param errorMessages     A list to store any error messages encountered during the operation.
     */
    private void saveCustomerAssessmentParameters(CustomerAssessmentBaseRequest request, Long nextSequenceValue, List<String> errorMessages) {
        Set<Long> encounteredCriteriaIds = new HashSet<>();
        List<CustomerAssessmentParameters> customerAssessmentParametersList = new ArrayList<>();

        for (CustomerAssessmentParametersRequest parameters : request.getParameters()) {
            Long parameterCriteriaId = parameters.getParameterCriteriaId();
            if (encounteredCriteriaIds.contains(parameterCriteriaId)) {
                errorMessages.add("parameterCriteriaId-parameterCriteria with id %s is duplicated;".formatted(parameterCriteriaId));
                continue;
            } else {
                encounteredCriteriaIds.add(parameterCriteriaId);
            }
            Optional<CustomerAssessmentCriteria> customerAssessmentCriteria = customerAssessmentCriteriaRepository
                    .findByIdAndStatus(parameterCriteriaId, NomenclatureItemStatus.ACTIVE);
            if (customerAssessmentCriteria.isPresent()) {
                String parameterValue = parameters.getParameterValue();

                boolean parameterAssessmentForSave = parameters.getParameterAssessment().equals(Assessment.YES);
                List<AssessmentCriteriaNamesWithoutValues> enumList = Arrays.asList(AssessmentCriteriaNamesWithoutValues.values());
                List<String> enumValues = EPBListUtils.convertEnumListIntoStringListIfNotNull(enumList);
                if (enumValues.contains(customerAssessmentCriteria.get().getCriteriaType().name())) {
                    if (isNotValidSingleSelectValue(parameterValue)) {
                        errorMessages.add("parameterValue-parameterValue must be 'Yes' or 'No' for criteria with id %s;".formatted(parameterCriteriaId));
                        continue;
                    }
                    Assessment parameterAssessment = parameters.getParameterAssessment();
                    parameterAssessmentForSave = customerAssessmentCriteria.get().getValue().equals(parameterAssessment.equals(Assessment.YES));
                }

                CustomerAssessmentParameters customerAssessmentParameters =
                        new CustomerAssessmentParameters(
                                null,
                                parameters.getParameterValue(),
                                nextSequenceValue,
                                parameterAssessmentForSave,
                                parameters.getParameterFinalAssessment().equals(Assessment.YES),
                                parameterCriteriaId
                        );
                customerAssessmentParametersList.add(customerAssessmentParameters);
            } else {
                errorMessages.add("parameterCriteriaId-parameterCriteria with id %s not found;".formatted(parameterCriteriaId));
            }
        }

        customerAssessmentParametersRepository.saveAll(customerAssessmentParametersList);
    }

    /**
     * Maps a list of {@link CustomerAssessmentComments} to a list of {@link CustomerAssessmentCommentHistory} objects.
     * For each {@link CustomerAssessmentComments}, it retrieves the display name of the commenter from the {@link AccountManagerRepository}
     * and creates a corresponding {@link CustomerAssessmentCommentHistory} object with the commenter's display name, the comment, and the creation date.
     *
     * @param customerAssessmentComments The list of {@link CustomerAssessmentComments} to be mapped.
     * @param accountManagerRepository   The {@link AccountManagerRepository} used to retrieve the commenter's display name.
     * @return A list of {@link CustomerAssessmentCommentHistory} objects representing the comment history.
     */
    public List<CustomerAssessmentCommentHistory> mapCustomerAssessmentCommentHistoryResponse(List<CustomerAssessmentComments> customerAssessmentComments, AccountManagerRepository accountManagerRepository) {
        List<CustomerAssessmentCommentHistory> customerAssessmentCommentHistories = new ArrayList<>();
        customerAssessmentComments.forEach(customerAssessmentComment -> {
            String commenter = customerAssessmentComment.getSystemUserId();
            Optional<AccountManager> commenterOptional = accountManagerRepository.findByUserName(customerAssessmentComment.getSystemUserId());
            if (commenterOptional.isPresent()) {
                commenter = commenterOptional.get().getDisplayName();
            }
            CustomerAssessmentCommentHistory customerAssessmentCommentHistory = new CustomerAssessmentCommentHistory();
            customerAssessmentCommentHistory.setCommenter(commenter);
            customerAssessmentCommentHistory.setCreateDate(customerAssessmentComment.getCreateDate());
            customerAssessmentCommentHistory.setComment(customerAssessmentComment.getComment());
            customerAssessmentCommentHistories.add(customerAssessmentCommentHistory);
        });

        return customerAssessmentCommentHistories;
    }

    /**
     * Checks if the provided value is a valid single-select value, which can only be "Yes" or "No" (case-insensitive).
     *
     * @param value The value to be checked.
     * @return {@code true} if the value is a valid single-select value, {@code false} otherwise.
     */
    private boolean isNotValidSingleSelectValue(String value) {
        return value == null || (!value.equalsIgnoreCase("Yes") && !value.equalsIgnoreCase("No"));
    }

    /**
     * Finds and sets the file information for the given CustomerAssessmentResponse.
     * Retrieves all active files associated with the CustomerAssessment and maps them to a list of FileWithStatusesResponse objects,
     * which include the file details and the display name of the system user who created the file.
     *
     * @param response The CustomerAssessmentResponse for which to find and set the file information.
     */
    private void findAndSetFileInfo(CustomerAssessmentResponse response) {
        customerAssessmentFilesRepository
                .findAllByCustomerAssessmentIdAndStatus(response.getId(), EntityStatus.ACTIVE)
                .ifPresent(files -> {
                    List<FileWithStatusesResponse> filesShortResponse = files.stream()
                            .map(file -> new FileWithStatusesResponse(file, accountManagerRepository.findByUserName(file.getSystemUserId())
                                    .map(manager -> " (".concat(manager.getDisplayName()).concat(")")).orElse("")))
                            .toList();
                    response.setFiles(filesShortResponse);
                });
    }

    /**
     * Finds and sets the rescheduling information for the given CustomerAssessmentResponse.
     * Retrieves the executed rescheduling associated with the CustomerAssessment and maps it to a ShortResponse object,
     * which includes the rescheduling ID and number.
     *
     * @param response The CustomerAssessmentResponse for which to find and set the rescheduling information.
     */
    private void findAndSetRescheduling(CustomerAssessmentResponse response) {
        Optional<Rescheduling> rescheduling = reschedulingRepository.findByCustomerAssessmentIdAndReschedulingStatus(response.getId(), ReschedulingStatus.EXECUTED);
        if (rescheduling.isPresent()) {
            ShortResponse reschedulingResponse = new ShortResponse(rescheduling.get().getId(), rescheduling.get().getReschedulingNumber());
            response.setRescheduling(reschedulingResponse);
        }
    }

    /**
     * Finds and sets the parameters information for the given CustomerAssessmentResponse.
     * Retrieves the parameters associated with the CustomerAssessment and maps them to a list of CustomerAssessmentParametersResponse objects,
     * which include the parameter criteria, value, assessment, and final assessment.
     * The parameters retrieved and the information included in the response depends on the assessment status of the CustomerAssessment.
     *
     * @param response The CustomerAssessmentResponse for which to find and set the parameters information.
     */
    private void findAndSetParametersInfo(CustomerAssessmentResponse response) {
        List<CustomerAssessmentParametersResponse> parameters;
        if (response.getAssessmentStatus().equals(AssessmentStatus.DRAFT)) {
            parameters = customerAssessmentParametersRepository.getParametersForDraft(response.getCustomer().id(), response.getId())
                    .stream()
                    .map(param -> {
                        CustomerAssessmentCriteria customerAssessmentCriteria = customerAssessmentCriteriaRepository
                                .findByIdAndStatus(param.getCustomerAssessmentCriteriaId(), NomenclatureItemStatus.ACTIVE)
                                .orElseThrow(() -> new DomainEntityNotFoundException("CustomerAssessmentCriteria with id %s not found;".formatted(param.getCustomerAssessmentCriteriaId())));
                        Assessment assessment = param.getAssessment() ? Assessment.YES : Assessment.NO;

                        if (param.getIsValue()) {
                            Boolean assessmentBoolValue = (param.getValue().equals(Assessment.YES.name()));
                            assessment = assessmentBoolValue.equals(customerAssessmentCriteria.getValue()) ? Assessment.YES : Assessment.NO;
                        }
                        CustomerAssessmentParametersResponse paramResponse = new CustomerAssessmentParametersResponse();
                        paramResponse.setParameterCriteria(new ShortResponse(param.getCustomerAssessmentCriteriaId(), param.getConditions()));
                        paramResponse.setParameterValue(param.getValue());
                        paramResponse.setParameterAssessment(assessment);
                        paramResponse.setParameterFinalAssessment(param.getFinalAssessment() ? Assessment.YES : Assessment.NO);
                        paramResponse.setIsValue(param.getIsValue());
                        paramResponse.setValueFrom(param.getValueFrom());
                        paramResponse.setValueTo(param.getValueTo());
                        paramResponse.setCustomerType(param.getCustomerType());
                        return paramResponse;
                    })
                    .collect(Collectors.toList());
        } else {
            parameters = customerAssessmentParametersRepository.getParametersForFinal(response.getId())
                    .stream()
                    .map(param -> {
                        CustomerAssessmentParametersResponse paramResponse = new CustomerAssessmentParametersResponse();
                        paramResponse.setParameterCriteria(new ShortResponse(param.getCustomerAssessmentCriteriaId(), param.getConditions()));
                        paramResponse.setParameterValue(param.getValue());
                        paramResponse.setParameterAssessment(param.getAssessment() ? Assessment.YES : Assessment.NO);
                        paramResponse.setParameterFinalAssessment(param.getFinalAssessment() ? Assessment.YES : Assessment.NO);
                        return paramResponse;
                    })
                    .collect(Collectors.toList());
        }
        response.setParameters(parameters);
    }

    /**
     * Checks the sort field for the CustomerAssessmentListingRequest.
     * If the sortBy field is null, it returns the default sort field (CustomerAssessmentListColumns.NUMBER).
     * Otherwise, it returns the value of the sortBy field.
     *
     * @param request The CustomerAssessmentListingRequest to check the sort field for.
     * @return The sort field to use for the request.
     */
    private String checkSortField(CustomerAssessmentListingRequest request) {
        if (request.getSortBy() == null) {
            return CustomerAssessmentListColumns.NUMBER.getValue();
        } else
            return request.getSortBy().getValue();
    }

    /**
     * Checks the search field for the CustomerAssessmentListingRequest.
     * If the searchBy field is null, it returns the default search field (CustomerAssessmentSearchByEnums.ALL).
     * Otherwise, it returns the value of the searchBy field.
     *
     * @param request The CustomerAssessmentListingRequest to check the search field for.
     * @return The search field to use for the request.
     */
    private String getSearchByEnum(CustomerAssessmentListingRequest request) {
        String searchByField;
        if (request.getSearchBy() != null) {
            searchByField = request.getSearchBy().getValue();
        } else
            searchByField = CustomerAssessmentSearchByEnums.ALL.getValue();
        return searchByField;
    }

    /**
     * Retrieves the list of entity statuses that the current user is permitted to view based on their permissions.
     * If the user has the 'CUSTOMER_ASSESSMENT_VIEW_DELETED' permission, the 'DELETED' status is added to the list.
     * If the user has the 'CUSTOMER_ASSESSMENT_VIEW' permission, the 'ACTIVE' status is added to the list.
     *
     * @param statuses The initial list of entity statuses to filter.
     * @return The filtered list of entity statuses that the user is permitted to view.
     */
    private List<EntityStatus> getStatusesByPermission(List<EntityStatus> statuses) {
        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.CUSTOMER_ASSESSMENT, List.of(PermissionEnum.CUSTOMER_ASSESSMENT_VIEW_DELETED))) {
            statuses.add(EntityStatus.DELETED);
        }

        if (permissionService.permissionContextContainsPermissions(PermissionContextEnum.CUSTOMER_ASSESSMENT, List.of(PermissionEnum.CUSTOMER_ASSESSMENT_VIEW))) {
            statuses.add(EntityStatus.ACTIVE);
        }

        return statuses;
    }

    /**
     * Adds and deletes files associated with a customer assessment.
     * <p>
     * This method handles the logic for adding and deleting files related to a customer assessment. It compares the list of file IDs from the request with the existing files in the database, and performs the necessary updates.
     *
     * @param request The CustomerAssessmentBaseRequest containing the updated file information.
     * @param id      The ID of the customer assessment.
     */
    private void addAndDeleteFiles(CustomerAssessmentBaseRequest request, Long id) {
        List<Long> FileIdListFromRequest = Objects.requireNonNullElse(request.getFiles(), new ArrayList<>());
        List<CustomerAssessmentFiles> fileIdsFromDb = getRelatedFiles(id);
        List<Long> oldFileIdList = fileIdsFromDb.stream().map(CustomerAssessmentFiles::getId).toList();

        List<Long> deletedFromList = EPBListUtils.getDeletedElementsFromList(oldFileIdList, FileIdListFromRequest);
        deletedFromList
                .stream()
                .filter(Objects::nonNull)
                .map(aLong -> fileIdsFromDb
                        .stream()
                        .filter(file -> Objects.equals(file.getId(), aLong))
                        .findFirst()
                        .orElse(null)
                )
                .filter(Objects::nonNull)
                .forEach(file -> {
                            file.setStatus(EntityStatus.DELETED);
                            customerAssessmentFilesRepository.save(file);
                        }
                );
        request.setFiles(EPBListUtils.getAddedElementsFromList(oldFileIdList, FileIdListFromRequest));
    }

    /**
     * Adds and deletes additional conditions associated with a customer assessment.
     * <p>
     * This method handles the logic for adding and deleting additional conditions related to a customer assessment. It compares the list of additional condition IDs from the request with the existing additional conditions in the database, and performs the necessary updates.
     *
     * @param request The CustomerAssessmentBaseRequest containing the updated additional condition information.
     * @param id      The ID of the customer assessment.
     */
    private void addAndDeleteAdditionalConditions(CustomerAssessmentBaseRequest request, Long id) {
        List<Long> AdditionalConditionsIdListFromRequest = Objects.requireNonNullElse(request.getAdditionalConditions(), new ArrayList<>());
        List<CustomerAssessmentAddCondition> additionalConditionsIdsFromDb = getRelatedAdditionalConditions(id);
        List<Long> oldAdditionalConditionsIdList = additionalConditionsIdsFromDb.stream().map(CustomerAssessmentAddCondition::getId).toList();

        List<Long> deletedFromList = EPBListUtils.getDeletedElementsFromList(oldAdditionalConditionsIdList, AdditionalConditionsIdListFromRequest);
        deletedFromList
                .stream()
                .filter(Objects::nonNull)
                .map(aLong -> additionalConditionsIdsFromDb
                        .stream()
                        .filter(additionalCondition -> Objects.equals(additionalCondition.getId(), aLong))
                        .findFirst()
                        .orElse(null)
                )
                .filter(Objects::nonNull)
                .forEach(additionalCondition -> {
                            additionalCondition.setStatus(EntityStatus.DELETED);
                            customerAssessmentAddConditionRepository.save(additionalCondition);
                        }
                );
        request.setFiles(EPBListUtils.getAddedElementsFromList(oldAdditionalConditionsIdList, AdditionalConditionsIdListFromRequest));
    }

    /**
     * Retrieves the list of active customer assessment files associated with the specified customer assessment ID.
     *
     * @param customerAssessmentId The ID of the customer assessment.
     * @return The list of active customer assessment files, or an empty list if none are found.
     */
    private List<CustomerAssessmentFiles> getRelatedFiles(Long customerAssessmentId) {
        Optional<List<CustomerAssessmentFiles>> files = customerAssessmentFilesRepository.findAllByCustomerAssessmentIdAndStatus(customerAssessmentId, EntityStatus.ACTIVE);
        return files.orElseGet(ArrayList::new);
    }

    /**
     * Retrieves the list of active additional conditions associated with the specified customer assessment ID.
     *
     * @param customerAssessmentId The ID of the customer assessment.
     * @return The list of active additional conditions, or an empty list if none are found.
     */
    private List<CustomerAssessmentAddCondition> getRelatedAdditionalConditions(Long customerAssessmentId) {
        Optional<List<CustomerAssessmentAddCondition>> additionalConditions = customerAssessmentAddConditionRepository.findAllByCustomerAssessmentIdAndStatus(customerAssessmentId, EntityStatus.ACTIVE);
        return additionalConditions.orElseGet(ArrayList::new);
    }

    /**
     * Updates the parameters for a customer assessment.
     *
     * @param request              The CustomerAssessmentBaseRequest containing the updated parameter information.
     * @param customerAssessmentId The ID of the customer assessment.
     * @param errorMessages        A list to store any error messages encountered during the update process.
     */
    private void updateParameters(CustomerAssessmentBaseRequest request, Long customerAssessmentId, List<String> errorMessages) {
        Map<Long, CustomerAssessmentParametersRequest> criteriaMap = EPBListUtils.transformToMap(request.getParameters(), CustomerAssessmentParametersRequest::getParameterCriteriaId);
        List<CustomerAssessmentParameters> updatedParameters = new ArrayList<>();

        for (Long criteriaId : criteriaMap.keySet()) {
            Optional<CustomerAssessmentParameters> parameterFromDb = customerAssessmentParametersRepository.findByCustomerAssessmentIdAndCustomerAssessmentCriteriaId(customerAssessmentId, criteriaId);
            if (parameterFromDb.isPresent()) {
                CustomerAssessmentParameters customerAssessmentParameter = parameterFromDb.get();
                CustomerAssessmentParametersRequest parameterRequest = criteriaMap.get(criteriaId);

                customerAssessmentParameter.setAssessment(parameterRequest.getParameterAssessment().equals(Assessment.YES));
                customerAssessmentParameter.setFinalAssessment(parameterRequest.getParameterFinalAssessment().equals(Assessment.YES));

                String parameterValue = parameterRequest.getParameterValue();

                CustomerAssessmentCriteria customerAssessmentCriteria = customerAssessmentCriteriaRepository
                        .findByIdAndStatus(criteriaId, NomenclatureItemStatus.ACTIVE)
                        .orElseThrow(() -> new DomainEntityNotFoundException("CustomerAssessmentCriteria with id %s not found;".formatted(criteriaId)));

                List<AssessmentCriteriaNamesWithoutValues> enumList = Arrays.asList(AssessmentCriteriaNamesWithoutValues.values());
                List<String> enumValues = EPBListUtils.convertEnumListIntoStringListIfNotNull(enumList);
                if (enumValues.contains(customerAssessmentCriteria.getCriteriaType().name())) {
                    if (isNotValidSingleSelectValue(parameterValue)) {
                        errorMessages.add("parameterValue-parameterValue must be 'Yes' or 'No' for criteria with id %s;".formatted(criteriaId));
                        continue;
                    }
                }

                customerAssessmentParameter.setValue(parameterValue);
                updatedParameters.add(customerAssessmentParameter);
            } else {
                errorMessages.add("parameterCriteriaId-parameterCriteria with id %s not found;".formatted(criteriaId));
            }
        }

        if (errorMessages.isEmpty()) {
            customerAssessmentParametersRepository.saveAll(updatedParameters);
        }
    }

    /**
     * Retrieves a list of final assessment values based on the provided CustomerAssessmentListingRequest.
     *
     * @param request The CustomerAssessmentListingRequest containing the final assessment values to be processed.
     * @return A list of String values representing the final assessment values, where "true" represents Assessment.YES and "false" represents anything else.
     */
    private List<String> getFinalAssessments(CustomerAssessmentListingRequest request) {
        List<String> finalAssessment = new ArrayList<>();
        if (Objects.nonNull(request.getFinalAssessment())) {
            request.getFinalAssessment().forEach(finAssessment -> {
                if (finAssessment.equals(Assessment.YES)) {
                    finalAssessment.add("true");
                } else {
                    finalAssessment.add("false");
                }
            });
        }
        return finalAssessment;
    }

}
