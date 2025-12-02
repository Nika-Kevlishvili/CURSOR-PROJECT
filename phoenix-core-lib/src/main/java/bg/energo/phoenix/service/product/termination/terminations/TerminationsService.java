package bg.energo.phoenix.service.product.termination.terminations;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.product.ProductTerminations;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.product.service.ServiceTermination;
import bg.energo.phoenix.model.entity.product.termination.terminations.Termination;
import bg.energo.phoenix.model.entity.product.termination.terminations.TerminationNotificationChannel;
import bg.energo.phoenix.model.enums.copy.domain.CopyDomain;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.enums.product.termination.terminations.TerminationAvailability;
import bg.energo.phoenix.model.enums.product.termination.terminations.TerminationNotificationChannelType;
import bg.energo.phoenix.model.enums.product.termination.terminations.TerminationStatus;
import bg.energo.phoenix.model.enums.product.termination.terminations.filter.TerminationSearchFields;
import bg.energo.phoenix.model.enums.product.termination.terminations.filter.TerminationSortFields;
import bg.energo.phoenix.model.enums.template.ContractTemplatePurposes;
import bg.energo.phoenix.model.enums.template.ContractTemplateStatus;
import bg.energo.phoenix.model.request.copy.domain.CopyDomainBaseRequest;
import bg.energo.phoenix.model.request.product.termination.terminations.AvailableTerminationSearchRequest;
import bg.energo.phoenix.model.request.product.termination.terminations.CreateTerminationRequest;
import bg.energo.phoenix.model.request.product.termination.terminations.TerminationListRequest;
import bg.energo.phoenix.model.response.copy.domain.CopyDomainListResponse;
import bg.energo.phoenix.model.response.terminations.AvailableTerminationResponse;
import bg.energo.phoenix.model.response.terminations.TerminationResponse;
import bg.energo.phoenix.model.response.terminations.TerminationsListResponse;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.repository.product.product.ProductTerminationRepository;
import bg.energo.phoenix.repository.product.service.subObject.ServiceTerminationRepository;
import bg.energo.phoenix.repository.product.termination.terminations.TerminationNotificationChannelsRepository;
import bg.energo.phoenix.repository.product.termination.terminations.TerminationRepository;
import bg.energo.phoenix.repository.template.ContractTemplateRepository;
import bg.energo.phoenix.security.PermissionService;
import bg.energo.phoenix.service.copy.domain.CopyDomainBaseService;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.permissions.PermissionEnum.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TerminationsService implements CopyDomainBaseService {

    private final TerminationRepository terminationRepository;
    private final PermissionService permissionService;
    private final TerminationNotificationChannelsRepository terminationNotificationChannelsRepository;
    private final ServiceTerminationRepository serviceTerminationRepository;
    private final ProductTerminationRepository productTerminationRepository;
    private final ContractTemplateRepository contractTemplateRepository;


    /**
     * Creates termination and termination notification channels
     * based on create termination request {@link Termination}, {@link TerminationNotificationChannel} {@link CreateTerminationRequest}
     *
     * @param request which consists data required to create termination and termination notification channels
     * @return termination response {@link TerminationResponse}
     */
    @Transactional
    public TerminationResponse createTermination(CreateTerminationRequest request) {
        trimFieldsAndChangeNullsWithFalse(request);

        Termination termination = new Termination();
        termination.setName(request.getName());
        termination.setContractClauseNumber(request.getContractClauseNumber());
        termination.setAutoTermination(request.getAutoTermination());
        termination.setAutoTerminationFrom(request.getAutoTerminationFrom());
        termination.setEvent(request.getEvent());
        termination.setNoticeDue(request.getNoticeDue());
        termination.setNoticeDueValueMin(request.getNoticeDueValueMin());
        termination.setNoticeDueValueMax(request.getNoticeDueValueMax());
        termination.setCalculateFrom(request.getCalculateFrom());
        termination.setNoticeDueType(request.getNoticeDueType());
        termination.setAutoEmailNotification(request.getAutoEmailNotification());
        termination.setAdditionalInfo(request.getAdditionalInfo());
        termination.setStatus(TerminationStatus.ACTIVE);
        //TODO TEMPLATE for delivery purpose - should be removed
        validateAndSetTemplate(request.getTemplateId(),termination);
        Set<TerminationNotificationChannel> terminationNotificationChannels =
                createTerminationNotificationChannels(request.getTerminationNotificationChannels(), termination);

        termination.setTerminationNotificationChannels(terminationNotificationChannels);

        Termination save = terminationRepository.save(termination);

        return new TerminationResponse(save);
    }

    /**
     * Creates set of termination notification channels {@link TerminationNotificationChannel}
     *
     * @param terminationNotificationChannelTypes notification channel types provided by user, which are used to create channels {@link TerminationNotificationChannelType}
     * @param termination                         Termination which contains created notification channels {@link Termination}
     * @return set of termination notification channels {@link TerminationNotificationChannel}
     */
    private Set<TerminationNotificationChannel> createTerminationNotificationChannels(Set<TerminationNotificationChannelType> terminationNotificationChannelTypes,
                                                                                      Termination termination) {
        Set<TerminationNotificationChannel> terminationNotificationChannels = new HashSet<>();
        for (TerminationNotificationChannelType terminationNotificationChannelType : terminationNotificationChannelTypes) {
            terminationNotificationChannels.add(createTerminationNotificationChannel(terminationNotificationChannelType, termination));
        }

        return terminationNotificationChannels;
    }

    /**
     * Creates termination notification channel and sets termination to it {@link TerminationNotificationChannel}, {@link Termination}
     *
     * @param terminationNotificationChannelType receives termination notification channel type which is used to create channel {@link TerminationNotificationChannelType}, {@link TerminationNotificationChannel}
     * @param termination                        receives termination which is set to notification channel {@link Termination}
     * @return created termination notification channel {@link TerminationNotificationChannel}
     */
    private TerminationNotificationChannel createTerminationNotificationChannel(TerminationNotificationChannelType terminationNotificationChannelType,
                                                                                Termination termination) {
        TerminationNotificationChannel terminationNotificationChannel = new TerminationNotificationChannel();
        terminationNotificationChannel.setTerminationNotificationChannelType(terminationNotificationChannelType);
        terminationNotificationChannel.setTermination(termination);
        return terminationNotificationChannel;
    }

    /**
     * Trims all String type fields in create termination request {@link CreateTerminationRequest}
     * and sets false to auto email notification if it is null {@link CreateTerminationRequest#autoEmailNotification}
     *
     * @param createTerminationRequest fields are trimmed and auto email notification set to false if it is null
     */
    private void trimFieldsAndChangeNullsWithFalse(CreateTerminationRequest createTerminationRequest) {
        createTerminationRequest.setName(createTerminationRequest.getName() == null ? null : createTerminationRequest.getName().trim());
        createTerminationRequest.setContractClauseNumber(createTerminationRequest.getContractClauseNumber() == null ? null : createTerminationRequest.getContractClauseNumber().trim());
        createTerminationRequest.setAdditionalInfo(createTerminationRequest.getAdditionalInfo() == null ? null : createTerminationRequest.getAdditionalInfo().trim());

        if (createTerminationRequest.getAutoEmailNotification() == null)
            createTerminationRequest.setAutoEmailNotification(false);
    }

    /**
     * Edits termination with provided id based on received request {@link Termination}, {@link CreateTerminationRequest}
     * and, if required, creates new termination notification channels and deletes old ones which are not used anymore {@link TerminationNotificationChannel}
     *
     * @param id      of the termination which is being edited
     * @param request is used to edit termination {@link CreateTerminationRequest}
     * @return termination response based on edited termination {@link Termination}, {@link TerminationResponse}
     * @throws DomainEntityNotFoundException if termination can't be found by provided id {@link Termination}, {@link DomainEntityNotFoundException}
     */
    @Transactional
    public TerminationResponse editTermination(Long id, CreateTerminationRequest request) {
        Termination termination = terminationRepository
                .findByIdAndStatusIn(id, List.of(TerminationStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Not found termination by id: " + id + ";"));
        checkPermissionsToEdit(termination);
        termination.setName(request.getName());
        termination.setContractClauseNumber(request.getContractClauseNumber());
        termination.setAutoTermination(request.getAutoTermination());
        termination.setAutoTerminationFrom(request.getAutoTerminationFrom());
        termination.setEvent(request.getEvent());
        termination.setNoticeDue(request.getNoticeDue());
        termination.setNoticeDueValueMin(request.getNoticeDueValueMin());
        termination.setNoticeDueValueMax(request.getNoticeDueValueMax());
        termination.setCalculateFrom(request.getCalculateFrom());
        termination.setNoticeDueType(request.getNoticeDueType());
        termination.setAutoEmailNotification(request.getAutoEmailNotification());
        termination.setAdditionalInfo(request.getAdditionalInfo());
        termination.setStatus(TerminationStatus.ACTIVE);
        //TODO TEMPLATE for delivery purpose - should be removed
        validateAndSetTemplate(request.getTemplateId(),termination);
        termination.setTerminationNotificationChannels(changeTerminationNotificationChannels(request.getTerminationNotificationChannels(), termination));
        return new TerminationResponse(terminationRepository.save(termination));
    }

    /**
     * Checks if authenticated user has permissions to edit termination
     *
     * @param termination which is being edited
     * @throws ClientException if user does not have permissions to edit termination
     */
    private void checkPermissionsToEdit(Termination termination) {
        if (terminationRepository.hasLockedConnection(termination.getId())) {
            List<String> context = permissionService.getPermissionsFromContext(PermissionContextEnum.TERMINATION);
            if (!context.contains(TERMINATION_EDIT_LOCKED.getId()))
                throw new OperationNotAllowedException("You can't edit price component because it is connected to the product contract, service contract or service order!;");
        }
    }

    /**
     * Deletes notification channels if they are not in new edit request and adds new ones which are provided instead {@link TerminationNotificationChannel}
     *
     * @param newChannels termination notification channel types which are provided in new request {@link TerminationNotificationChannelType}
     * @param termination which is being edited {@link Termination}
     * @return set of termination notification channels to set to termination {@link TerminationNotificationChannel}
     */
    private Set<TerminationNotificationChannel> changeTerminationNotificationChannels(Set<TerminationNotificationChannelType> newChannels, Termination termination) {
        Set<TerminationNotificationChannel> currentChannels = termination.getTerminationNotificationChannels();
        currentChannels = deleteNotProvidedChannels(newChannels, currentChannels);
        Set<TerminationNotificationChannelType> currentChannelTypes = currentChannels
                .stream()
                .map(TerminationNotificationChannel::getTerminationNotificationChannelType)
                .collect(Collectors.toSet());
        for (TerminationNotificationChannelType newChannelType : newChannels) {
            if (!currentChannelTypes.contains(newChannelType)) {
                currentChannels.add(createTerminationNotificationChannel(newChannelType, termination));
            }
        }
        return currentChannels;
    }

    /**
     * Deletes notification channels from current list if they are not included in new list
     *
     * @param newChannels     new notification channel types
     * @param currentChannels current notification channels of termination
     * @return final version of notification channels
     */
    private Set<TerminationNotificationChannel> deleteNotProvidedChannels(Set<TerminationNotificationChannelType> newChannels, Set<TerminationNotificationChannel> currentChannels) {
        Set<TerminationNotificationChannel> mustBeDeleted = new HashSet<>();
        Set<TerminationNotificationChannel> leftChannels = new HashSet<>();
        for (TerminationNotificationChannel channel : currentChannels) {
            if (!newChannels.contains(channel.getTerminationNotificationChannelType())) {
                mustBeDeleted.add(channel);
            } else {
                leftChannels.add(channel);
            }
        }
        terminationNotificationChannelsRepository.deleteAll(mustBeDeleted);
        return leftChannels;
    }

    /**
     * Changes termination's status to DELETED with provided id {@link Termination}
     *
     * @param id of termination which is being deleted
     * @return termination response with deleted termination's id {@link TerminationResponse}
     * @throws DomainEntityNotFoundException if termination can't be found by provided id {@link Termination}, {@link DomainEntityNotFoundException}
     * @throws ClientException               if termination is locked (is connected to group) {@link ClientException}
     */
    @Transactional
    public TerminationResponse deleteTermination(Long id) {
        Termination termination = terminationRepository
                .findByIdAndStatusIn(id, List.of(TerminationStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Not found termination by id: " + id + ";"));

        if (termination.getTerminationGroupDetailId() != null) {
            log.error("id-You can’t delete the termination with ID [%s] because it has connection to the group of termination".formatted(id));
            throw new ClientException("id-You can’t delete the termination with ID [%s] because it has connection to the group of termination".formatted(id), ErrorCode.OPERATION_NOT_ALLOWED);
        }

        if (terminationRepository.hasConnectionToProduct(termination.getId(), List.of(ProductStatus.ACTIVE), List.of(ProductSubObjectStatus.ACTIVE))) {
            log.error("id-You can’t delete the termination with ID [%s] because it has connection to the product".formatted(id));
            throw new ClientException("id-You can’t delete the termination with ID [%s] because it has connection to the product".formatted(id), ErrorCode.OPERATION_NOT_ALLOWED);
        }

        if (terminationRepository.hasConnectionToService(termination.getId(), List.of(ServiceStatus.ACTIVE), List.of(ServiceSubobjectStatus.ACTIVE))) {
            log.error("id-You can’t delete the termination with ID [%s] because it has connection to the service".formatted(id));
            throw new ClientException("id-You can’t delete the termination with ID [%s] because it has connection to the service".formatted(id), ErrorCode.OPERATION_NOT_ALLOWED);
        }

        termination.setStatus(TerminationStatus.DELETED);
        termination = terminationRepository.save(termination);
        TerminationResponse response = new TerminationResponse();
        response.setId(termination.getId());
        return response;
    }

    /**
     * View termination with provided id {@link Termination}
     *
     * @param id of termination which is being viewed
     * @return termination response of termination with provided id {@link Termination}, {@link TerminationResponse}
     * @throws DomainEntityNotFoundException if termination can't be found by provided id {@link DomainEntityNotFoundException}
     */
    public TerminationResponse view(Long id) {
        Termination termination = terminationRepository
                .findByIdAndStatusIn(id, getTerminationStatusesForFilter())
                .orElseThrow(() -> new DomainEntityNotFoundException("Not found termination by id: " + id + ";"));
//        boolean hasConnection = termination.getTerminationGroupDetailId() != null
//                || terminationRepository.hasConnectionToProduct(termination.getId(), List.of(ProductStatus.ACTIVE), List.of(ProductSubObjectStatus.ACTIVE))
//                || terminationRepository.hasConnectionToService(termination.getId(), List.of(ServiceStatus.ACTIVE), List.of(ServiceSubobjectStatus.ACTIVE));
//        return new TerminationResponse(termination, hasConnection);
        TerminationResponse terminationResponse = new TerminationResponse(termination);
        contractTemplateRepository.findTemplateResponseById(termination.getTemplateId(), LocalDate.now())
                .ifPresent(terminationResponse::setTemplateResponse);

        terminationResponse.setIsLocked(terminationRepository.hasLockedConnection(id));
        return terminationResponse;
    }

    /**
     * Returns page of termination responses which are filtered based on provided parameters {@link TerminationsListResponse}, {@link TerminationListRequest}
     *
     * @param request contains parameters which are used to filter search result {@link TerminationListRequest}
     * @return page of termination list response {@link TerminationsListResponse}
     */
    public Page<TerminationsListResponse> list(TerminationListRequest request) {
        log.debug("Retrieving a list of terminations by request: {}", request);

        Sort.Order order = new Sort.Order(
                Objects.requireNonNullElse(request.getSortDirection(), Sort.Direction.ASC),
                getSortField(request.getTerminationSortFields())
        );

        return terminationRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                getSearchField(request.getTerminationSearchFields()),
                getTerminationStatusesForFilter().stream().map(TerminationStatus::name).toList(),
                getAutoTermination(request.getAutoTermination()),
                request.getNoticeDue(),
                request.getAvailability() == TerminationAvailability.ALL ? null : request.getAvailability().toString(),
                PageRequest.of(
                        request.getPage(),
                        request.getSize(),
                        Sort.by(order)
                )
        );
    }


    /**
     * Returns auto termination value based on provided request {@link TerminationListRequest}.
     * If request contains more than one value (so, "select all" is selected in UI), null is returned to ignore this filter.
     *
     * @param autoTermination containing auto termination value
     * @return auto termination value
     */
    private Boolean getAutoTermination(List<Boolean> autoTermination) {
        return autoTermination != null && autoTermination.size() == 1 ? autoTermination.get(0) : null;
    }


    /**
     * Returns list of termination statuses based on authenticated users permissions {@link TerminationStatus}
     *
     * @return list of termination statuses {@link TerminationStatus}
     */
    private List<TerminationStatus> getTerminationStatusesForFilter() {
        List<TerminationStatus> statuses = new ArrayList<>();
        List<String> context = permissionService.getPermissionsFromContext(PermissionContextEnum.TERMINATION);
        if (context.contains(TERMINATION_VIEW_BASIC.getId())) statuses.add(TerminationStatus.ACTIVE);
        if (context.contains(TERMINATION_VIEW_DELETED.getId())) statuses.add(TerminationStatus.DELETED);
        return statuses;
    }

    /**
     * Returns name of termination's field which is used in select query {@link TerminationSearchFields}
     *
     * @param terminationSearchFields - field name used in search
     * @return name of termination's field
     */
    private String getSearchField(TerminationSearchFields terminationSearchFields) {
        if (terminationSearchFields == null) return TerminationSearchFields.ALL.getValue();
        return terminationSearchFields.getValue();
    }

    /**
     * Returns name of termination's field which is used to sort final result {@link TerminationSortFields}
     *
     * @param terminationSortFields - field name used to sort
     * @return name of termination's field
     */
    private String getSortField(TerminationSortFields terminationSortFields) {
        if (terminationSortFields == null) return TerminationSortFields.ID.getValue();
        return terminationSortFields.getValue();
    }


    /**
     * Returns list of all available terminations filtered by a prompt sorted by creation date desc.
     *
     * @return list of {@link AvailableTerminationResponse} objects
     */
    public Page<AvailableTerminationResponse> getAvailableTerminations(AvailableTerminationSearchRequest request) {
        log.debug("Retrieving a list of all available terminations by request: {}", request);

        Page<Termination> availableTerminations = terminationRepository
                .getAvailableTerminations(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        PageRequest.of(request.getPage(), request.getSize())
                );

        return availableTerminations.map(AvailableTerminationResponse::responseFromEntity);
    }

    @Override
    public CopyDomain getDomain() {
        return CopyDomain.TERMINATIONS;
    }

    @Override
    public Page<CopyDomainListResponse> filterCopyDomain(CopyDomainBaseRequest request) {
        request.setPrompt(request.getPrompt() == null ? null : request.getPrompt().trim());
        Sort.Order order = new Sort.Order(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(order));
        return terminationRepository.filterForCopy(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                List.of(TerminationStatus.ACTIVE),
                pageable
        );
    }

    public TerminationResponse viewForCopy(Long id) {
        Termination termination = terminationRepository
                .findByIdAndStatusIn(id, List.of(TerminationStatus.ACTIVE))
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Termination can't be found by id: " + id + ";"));

        TerminationResponse terminationResponse = new TerminationResponse(termination);
        contractTemplateRepository.findTemplateResponseForCopy(termination.getTemplateId(), LocalDate.now())
                .ifPresent(terminationResponse::setTemplateResponse);

        return terminationResponse;
    }


    /**
     * Adds terminations to service details
     *
     * @param terminationIds    ids of terminations to be added
     * @param serviceDetails    service details to which terminations will be added
     * @param exceptionMessages list of exception messages which will be populated in case of any error
     */
    @Transactional
    public void addTerminationsToService(List<Long> terminationIds, ServiceDetails serviceDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(terminationIds)) {
            // fetch all available terminations at the moment of adding
            List<Long> availableTerminations = terminationRepository.findAvailableTerminationIdsForService(terminationIds);
            List<ServiceTermination> tempList = new ArrayList<>();

            for (int i = 0; i < terminationIds.size(); i++) {
                Long terminationId = terminationIds.get(i);
                if (availableTerminations.contains(terminationId)) {
                    Optional<Termination> terminationOptional = terminationRepository.findByIdAndStatusIn(terminationId, List.of(TerminationStatus.ACTIVE));
                    if (terminationOptional.isEmpty()) {
                        log.error("terminations[%s]-Termination with ID %s not found;".formatted(i, terminationId));
                        exceptionMessages.add("additionalSettings.terminations[%s]-Termination with ID %s not found;".formatted(i, terminationId));
                        continue;
                    }

                    ServiceTermination st = new ServiceTermination();
                    st.setTermination(terminationOptional.get());
                    st.setServiceDetails(serviceDetails);
                    st.setStatus(ServiceSubobjectStatus.ACTIVE);
                    tempList.add(st);
                } else {
                    log.error("terminations[%s]-Termination with ID %s is not available;".formatted(i, terminationId));
                    exceptionMessages.add("additionalSettings.terminations[%s]-Termination with ID %s is not available;".formatted(i, terminationId));
                }
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all penalties
            serviceTerminationRepository.saveAll(tempList);
        }
    }


    /**
     * Updates terminations for service details existing version
     *
     * @param requestTerminationIds ids of terminations to be updated for service details
     * @param serviceDetails        service details to which terminations will be updated
     * @param exceptionMessages     list of exception messages which will be populated in case of any error
     */
    @Transactional
    public void updateServiceTerminationsForExistingVersion(List<Long> requestTerminationIds, ServiceDetails serviceDetails, List<String> exceptionMessages) {
        // fetch all active db terminations for service
        List<ServiceTermination> dbTerminations = serviceTerminationRepository
                .findByServiceDetailsIdAndStatusIn(serviceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

        if (CollectionUtils.isNotEmpty(requestTerminationIds)) {
            List<Long> dbTerminationIds = dbTerminations.stream().map(st -> st.getTermination().getId()).toList();

            // fetch all available terminations at the moment of adding
            List<Long> availableTerminations = terminationRepository.findAvailableTerminationIdsForService(requestTerminationIds);
            List<ServiceTermination> tempList = new ArrayList<>();

            for (int i = 0; i < requestTerminationIds.size(); i++) {
                Long terminationId = requestTerminationIds.get(i);
                if (!dbTerminationIds.contains(terminationId)) { // if termination is new, its availability should be checked
                    if (availableTerminations.contains(terminationId)) {
                        Optional<Termination> terminationOptional = terminationRepository.findById(terminationId);
                        if (terminationOptional.isEmpty()) {
                            log.error("terminations[%s]-Termination with ID %s not found;".formatted(i, terminationId));
                            exceptionMessages.add("additionalSettings.terminations[%s]-Termination with ID %s not found;".formatted(i, terminationId));
                            continue;
                        }

                        ServiceTermination st = new ServiceTermination();
                        st.setTermination(terminationOptional.get());
                        st.setServiceDetails(serviceDetails);
                        st.setStatus(ServiceSubobjectStatus.ACTIVE);
                        tempList.add(st);
                    } else {
                        log.error("terminations[%s]-Termination with ID %s is not available;".formatted(i, terminationId));
                        exceptionMessages.add("additionalSettings.terminations[%s]-Termination with ID %s is not available;".formatted(i, terminationId));
                    }
                }
            }

            // if validations failed, don't save anything
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all new penalties
            serviceTerminationRepository.saveAll(tempList);
        }

        if (CollectionUtils.isNotEmpty(dbTerminations)) {
            for (ServiceTermination st : dbTerminations) {
                // if user has removed termination from request, set status to deleted
                if (!requestTerminationIds.contains(st.getTermination().getId())) {
                    st.setStatus(ServiceSubobjectStatus.DELETED);
                    serviceTerminationRepository.save(st);
                }
            }
        }
    }


    /**
     * Updates terminations for service details new version
     *
     * @param requestTerminationIds ids of terminations to be updated for service details
     * @param updatedServiceDetails service details to which terminations will be added
     * @param sourceServiceDetails  service details (version from which new version was created)
     * @param exceptionMessages     list of exception messages which will be populated in case of any error
     */
    @Transactional
    public void updateServiceTerminationsForNewVersion(List<Long> requestTerminationIds,
                                                       ServiceDetails updatedServiceDetails,
                                                       ServiceDetails sourceServiceDetails,
                                                       List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(requestTerminationIds)) {
            // fetch all active db terminations from source version
            List<ServiceTermination> dbTerminations = serviceTerminationRepository
                    .findByServiceDetailsIdAndStatusIn(sourceServiceDetails.getId(), List.of(ServiceSubobjectStatus.ACTIVE));

            List<Long> dbTerminationIds = dbTerminations.stream().map(st -> st.getTermination().getId()).toList();

            // fetch all available terminations at the moment of adding
            List<Long> availableTerminations = terminationRepository.findAvailableTerminationIdsForService(requestTerminationIds);
            List<Termination> tempList = new ArrayList<>();

            for (int i = 0; i < requestTerminationIds.size(); i++) {
                Long terminationId = requestTerminationIds.get(i);
                if (dbTerminationIds.contains(terminationId)) { // if termination is from the source version, it should be cloned
                    Termination termination = dbTerminations.stream()
                            .filter(t -> t.getTermination().getId().equals(terminationId))
                            .findFirst().get().getTermination(); // will always be present, as we have collected the list above
                    Termination cloned = cloneTermination(termination.getId());
                    tempList.add(cloned);
                } else {
                    if (availableTerminations.contains(terminationId)) { // if termination is new, its availability should be checked
                        Optional<Termination> terminationOptional = terminationRepository.findById(terminationId);
                        if (terminationOptional.isEmpty()) {
                            log.error("terminations[%s]-Termination with ID %s not found;".formatted(i, terminationId));
                            exceptionMessages.add("additionalSettings.terminations[%s]-Termination with ID %s not found;".formatted(i, terminationId));
                            continue;
                        }

                        tempList.add(terminationOptional.get());
                    } else {
                        log.error("terminations[%s]-Termination with ID %s is not available;".formatted(i, terminationId));
                        exceptionMessages.add("additionalSettings.terminations[%s]-Termination with ID %s is not available;".formatted(i, terminationId));
                    }
                }
            }

            // if validations failed, don't save anything
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all new terminations
            for (Termination termination : tempList) {
                ServiceTermination st = new ServiceTermination();
                st.setTermination(termination);
                st.setServiceDetails(updatedServiceDetails);
                st.setStatus(ServiceSubobjectStatus.ACTIVE);
                serviceTerminationRepository.save(st);
            }
        }
    }

    /**
     * Adds terminations to product details
     *
     * @param terminationIdsSet ids of terminations to be added
     * @param productDetails    product details to which terminations will be added
     * @param exceptionMessages list of exception messages which will be populated in case of any error
     */
    @Transactional
    public void addTerminationsToProduct(List<Long> terminationIdsSet, ProductDetails productDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(terminationIdsSet)) {
            List<Long> terminationIds = new ArrayList<>(terminationIdsSet); // this is for the sake of getting index of element when handling errors

            // fetch all available terminations at the moment of adding
            List<Long> availableTerminations = terminationRepository.findAvailableTerminationIdsForProduct(terminationIds);
            List<ProductTerminations> tempList = new ArrayList<>();

            for (int i = 0; i < terminationIds.size(); i++) {
                Long terminationId = terminationIds.get(i);
                Optional<Termination> terminationOptional = terminationRepository.findByIdAndStatusIn(terminationId, List.of(TerminationStatus.ACTIVE));
                if (terminationOptional.isEmpty()) {
                    log.error("terminationIds[%s]-Termination with ID %s not found;".formatted(i, terminationId));
                    exceptionMessages.add("terminationIds[%s]-Termination with ID %s not found;".formatted(i, terminationId));
                    continue;
                }
                if (availableTerminations.contains(terminationId)) {
                    ProductTerminations st = new ProductTerminations();
                    st.setTermination(terminationOptional.get());
                    st.setProductDetails(productDetails);
                    st.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                    tempList.add(st);
                } else {
                    log.error("terminationIds[%s]-Termination with ID %s is not available;".formatted(i, terminationId));
                    exceptionMessages.add("additionalSettings.terminationIds[%s]-Termination with ID %s is not available;".formatted(i, terminationId));
                }
            }

            // if there are any errors - return without saving
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all penalties
            productTerminationRepository.saveAll(tempList);
        }
    }


    /**
     * Updates terminations for product details existing version
     *
     * @param requestTerminationIdsSet ids of terminations to be updated for product details
     * @param productDetails           product details to which terminations will be updated
     * @param exceptionMessages        list of exception messages which will be populated in case of any error
     */
    @Transactional
    public void updateProductTerminationsForExistingVersion(List<Long> requestTerminationIdsSet, ProductDetails productDetails, List<String> exceptionMessages) {
        // fetch all active db terminations for product
        List<ProductTerminations> dbTerminations = productTerminationRepository
                .findByProductDetailsIdAndProductSubObjectStatusIn(productDetails.getId(), List.of(ProductSubObjectStatus.ACTIVE));

        if (CollectionUtils.isNotEmpty(requestTerminationIdsSet)) {
            List<Long> requestTerminationIds = new ArrayList<>(requestTerminationIdsSet); // this is for the sake of getting index of element when handling errors
            List<Long> dbTerminationIds = dbTerminations.stream().map(st -> st.getTermination().getId()).toList();

            // fetch all available terminations at the moment of adding
            List<Long> availableTerminations = terminationRepository.findAvailableTerminationIdsForProduct(requestTerminationIds);
            List<ProductTerminations> tempList = new ArrayList<>();

            for (int i = 0; i < requestTerminationIds.size(); i++) {
                Long terminationId = requestTerminationIds.get(i);
                Optional<Termination> terminationOptional = terminationRepository.findById(terminationId);
                if (terminationOptional.isEmpty()) {
                    log.error("terminationIds[%s]-Termination with ID %s not found;".formatted(i, terminationId));
                    exceptionMessages.add("additionalSettings.terminationIds[%s]-Termination with ID %s not found;".formatted(i, terminationId));
                    continue;
                }
                if (!dbTerminationIds.contains(terminationId)) { // if termination is new, its availability should be checked
                    if (availableTerminations.contains(terminationId)) {
                        ProductTerminations productTerminations = new ProductTerminations();
                        productTerminations.setTermination(terminationOptional.get());
                        productTerminations.setProductDetails(productDetails);
                        productTerminations.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                        tempList.add(productTerminations);
                    } else {
                        log.error("terminationIds[%s]-Termination with ID %s is not available;".formatted(i, terminationId));
                        exceptionMessages.add("additionalSettings.terminationIds[%s]-Termination with ID %s is not available;".formatted(i, terminationId));
                    }
                }
            }

            // if validations failed, don't save anything
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all new penalties
            productTerminationRepository.saveAll(tempList);
        }

        if (CollectionUtils.isNotEmpty(dbTerminations)) {
            for (ProductTerminations productTerminations : dbTerminations) {
                // if user has removed termination from request, set status to deleted
                if (!requestTerminationIdsSet.contains(productTerminations.getTermination().getId())) {
                    productTerminations.setProductSubObjectStatus(ProductSubObjectStatus.DELETED);
                    productTerminationRepository.save(productTerminations);
                }
            }
        }
    }


    /**
     * Updates terminations for product details new version
     *
     * @param requestTerminationIdsSet ids of terminations to be updated for product details
     * @param updatedProductDetails    product details to which terminations will be added
     * @param sourceProductDetails     product details (version from which new version was created)
     * @param exceptionMessages        list of exception messages which will be populated in case of any error
     */
    @Transactional
    public void updateProductTerminationsForNewVersion(List<Long> requestTerminationIdsSet,
                                                       ProductDetails updatedProductDetails,
                                                       ProductDetails sourceProductDetails,
                                                       List<String> exceptionMessages) {
        if (CollectionUtils.isNotEmpty(requestTerminationIdsSet)) {
            List<Long> requestTerminationIds = new ArrayList<>(requestTerminationIdsSet); // this is for the sake of getting index of element when handling errors

            // fetch all active db terminations from source version
            List<ProductTerminations> dbTerminations = productTerminationRepository
                    .findByProductDetailsIdAndProductSubObjectStatusIn(sourceProductDetails.getId(), List.of(ProductSubObjectStatus.ACTIVE));

            List<Long> dbTerminationIds = dbTerminations.stream().map(st -> st.getTermination().getId()).toList();

            // fetch all available terminations at the moment of adding
            List<Long> availableTerminations = terminationRepository.findAvailableTerminationIdsForProduct(requestTerminationIds);
            List<Termination> tempList = new ArrayList<>();

            for (int i = 0; i < requestTerminationIds.size(); i++) {
                Long terminationId = requestTerminationIds.get(i);
                if (dbTerminationIds.contains(terminationId)) { // if termination is from the source version, it should be cloned
                    Termination termination = dbTerminations.stream()
                            .filter(t -> t.getTermination().getId().equals(terminationId))
                            .findFirst().get().getTermination(); // will always be present, as we have collected the list above
                    Termination cloned = cloneTermination(termination.getId());
                    tempList.add(cloned);
                } else {
                    Optional<Termination> terminationOptional = terminationRepository.findById(terminationId);
                    if (terminationOptional.isEmpty()) {
                        log.error("terminationIds[%s]-Termination with ID %s not found;".formatted(i, terminationId));
                        exceptionMessages.add("additionalSettings.terminationIds[%s]-Termination with ID %s not found;".formatted(i, terminationId));
                        continue;
                    }
                    if (availableTerminations.contains(terminationId)) { // if termination is new, its availability should be checked
                        tempList.add(terminationOptional.get());
                    } else {
                        log.error("terminationIds[%s]-Termination with ID %s is not available;".formatted(i, terminationId));
                        exceptionMessages.add("additionalSettings.terminationIds[%s]-Termination with ID %s is not available;".formatted(i, terminationId));
                    }
                }
            }

            // if validations failed, don't save anything
            if (CollectionUtils.isNotEmpty(exceptionMessages)) {
                return;
            }

            // save all new terminations
            for (Termination termination : tempList) {
                ProductTerminations st = new ProductTerminations();
                st.setTermination(termination);
                st.setProductDetails(updatedProductDetails);
                st.setProductSubObjectStatus(ProductSubObjectStatus.ACTIVE);
                productTerminationRepository.save(st);
            }
        }
    }

    public List<Termination> copyTerminationsForSubObjects(List<Termination> terminations) {
        List<Termination> activeTerminations = terminations.stream()
                .filter(x -> x.getStatus().equals(TerminationStatus.ACTIVE))
                .map(t -> cloneTermination(t.getId()))
                .toList();
        return terminationRepository.saveAll(activeTerminations);
    }


    /**
     * Clones a termination and returns the clone. Cloned termination is always active and available.
     *
     * @param terminationId the termination ID to be cloned
     * @return the clone
     */
    @Transactional
    public Termination cloneTermination(Long terminationId) {
        Termination source = terminationRepository
                .findById(terminationId)
                .orElseThrow(() -> new DomainEntityNotFoundException("terminationsList[%s].terminationId-Termination not found by ID ".formatted(terminationId)));

        Termination clone = new Termination();
        clone.setStatus(TerminationStatus.ACTIVE);
        clone.setName(source.getName());
        clone.setContractClauseNumber(source.getContractClauseNumber());
        clone.setAutoTermination(source.getAutoTermination());
        clone.setAutoTerminationFrom(source.getAutoTerminationFrom());
        clone.setNoticeDue(source.getNoticeDue());
        clone.setNoticeDueValueMin(source.getNoticeDueValueMin());
        clone.setNoticeDueValueMax(source.getNoticeDueValueMax());
        clone.setCalculateFrom(source.getCalculateFrom());
        clone.setNoticeDueType(source.getNoticeDueType());
        clone.setAutoEmailNotification(source.getAutoEmailNotification());
        clone.setAdditionalInfo(source.getAdditionalInfo());
        clone.setTerminationGroupDetailId(null); // clone is "available"
        clone.setEvent(source.getEvent());
        // notification channel is mandatory when creating a termination, so at least 1 active will be present
        Set<TerminationNotificationChannel> clonedNotificationChannels = cloneTerminationNotificationChannels(source.getTerminationNotificationChannels(), clone);
        clone.setTerminationNotificationChannels(clonedNotificationChannels);

        return terminationRepository.save(clone);
    }

    /**
     * Clones a termination notification channels
     *
     * @param terminationNotificationChannels set of termination notification channels
     * @return cloned termination notification channels
     */
    private Set<TerminationNotificationChannel> cloneTerminationNotificationChannels(Set<TerminationNotificationChannel> terminationNotificationChannels,
                                                                                     Termination clonedTermination) {
        return terminationNotificationChannels
                .stream()
                .map(terminationNotificationChannel ->
                        new TerminationNotificationChannel(
                                null,
                                terminationNotificationChannel.getTerminationNotificationChannelType(),
                                clonedTermination
                        )
                )
                .collect(Collectors.toSet());
    }

    private void validateAndSetTemplate(Long templateId, Termination termination) {
        if (Objects.equals(templateId, termination.getTemplateId()))
            return;
        if (templateId == null) {
            termination.setTemplateId(null);
            return;
        }
        if (!contractTemplateRepository.existsByIdAndTemplatePurposeAndStatus(templateId, ContractTemplatePurposes.TERMINATION, ContractTemplateStatus.ACTIVE)) {
            throw new DomainEntityNotFoundException("templateId-Template with id %s do not exist!;".formatted(templateId));
        }
        termination.setTemplateId(templateId);
    }
}