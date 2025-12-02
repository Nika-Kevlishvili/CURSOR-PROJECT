package bg.energo.phoenix.service.nomenclature.crm;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.OperationNotAllowedException;
import bg.energo.phoenix.model.customAnotations.PermissionMapping;
import bg.energo.phoenix.model.customAnotations.PermissionValidator;
import bg.energo.phoenix.model.entity.nomenclature.crm.TopicOfCommunication;
import bg.energo.phoenix.model.enums.nomenclature.Nomenclature;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsBaseFilterRequest;
import bg.energo.phoenix.model.request.nomenclature.NomenclatureItemsSortOrderRequest;
import bg.energo.phoenix.model.request.nomenclature.crm.TopicOfCommunicationRequest;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.crm.TopicOfCommunicationResponse;
import bg.energo.phoenix.repository.nomenclature.crm.TopicOfCommunicationRepository;
import bg.energo.phoenix.service.nomenclature.NomenclatureBaseService;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;
import static bg.energo.phoenix.model.enums.nomenclature.Nomenclature.TOPIC_OF_COMMUNICATION;
import static bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus.*;
import static bg.energo.phoenix.permissions.PermissionContextEnum.TOPICS_OF_COMMUNICATIONS;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_EDIT;
import static bg.energo.phoenix.permissions.PermissionEnum.NOMENCLATURE_VIEW;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopicOfCommunicationService implements NomenclatureBaseService {

    private final TopicOfCommunicationRepository topicOfCommunicationRepository;

    @Override
    public Nomenclature getNomenclatureType() {
        return TOPIC_OF_COMMUNICATION;
    }

    @Override
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = TOPICS_OF_COMMUNICATIONS,
                            permissions = {NOMENCLATURE_VIEW}
                    )
            }
    )
    public Page<NomenclatureResponse> filterNomenclature(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering topics of communication with statuses: {}", request);
        Page<TopicOfCommunication> topicsOfCommunication = topicOfCommunicationRepository
                .filterNomenclature(
                        EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                        request.getStatuses(),
                        request.getExcludedItemId(),
                        PageRequest.of(
                                request.getPage(),
                                request.getSize()
                        )
                );

        return topicsOfCommunication.map(this::nomenclatureResponseFromEntity);
    }

    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(context = TOPICS_OF_COMMUNICATIONS, permissions = {NOMENCLATURE_EDIT})
            }
    )
    @Override
    public void changeOrder(NomenclatureItemsSortOrderRequest request) {
        log.debug("Changing order of item with ID: {} in topic of communications", request.getId());

        TopicOfCommunication topicOfCommunication = topicOfCommunicationRepository
                .findById(request.getId())
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Communication of topic not found, ID: " + request.getId()));

        Long start;
        Long end;
        List<TopicOfCommunication> topicOfCommunications;

        if (topicOfCommunication.getOrderingId() > request.getOrderingId()) { // move top
            start = request.getOrderingId();
            end = topicOfCommunication.getOrderingId();
            topicOfCommunications = topicOfCommunicationRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            topicOfCommunication.getId(),
                            Sort.by(Sort.Direction.ASC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() + 1;
            for (TopicOfCommunication ra : topicOfCommunications) {
                ra.setOrderingId(tempOrderingId);
                tempOrderingId += 1;
            }
        } else { // move bottom
            start = topicOfCommunication.getOrderingId();
            end = request.getOrderingId();
            topicOfCommunications = topicOfCommunicationRepository
                    .findInOrderingIdRange(
                            start,
                            end,
                            topicOfCommunication.getId(),
                            Sort.by(Sort.Direction.DESC, "orderingId")
                    );

            long tempOrderingId = request.getOrderingId() - 1;
            for (TopicOfCommunication ra : topicOfCommunications) {
                ra.setOrderingId(tempOrderingId);
                tempOrderingId -= 1;
            }
        }

        topicOfCommunication.setOrderingId(request.getOrderingId());
        topicOfCommunications.add(topicOfCommunication);
        topicOfCommunicationRepository.saveAll(topicOfCommunications);
    }

    public Page<TopicOfCommunicationResponse> filter(NomenclatureItemsBaseFilterRequest request) {
        log.debug("Filtering topics of communication with statuses: {}", request);
        Page<TopicOfCommunication> topicsOfCommunication = topicOfCommunicationRepository.filter(
                EPBStringUtils.fromPromptToQueryParameter(request.getPrompt()),
                request.getStatuses(),
                request.getExcludedItemId(),
                request.getIncludedItemIds(),
                PageRequest.of(
                        request.getPage(),
                        request.getSize()
                )
        );
        return topicsOfCommunication.map(this::responseFromEntity);
    }

    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = TOPICS_OF_COMMUNICATIONS,
                            permissions = {NOMENCLATURE_EDIT}
                    )
            }
    )
    @Override
    public void sortAlphabetically() {
        log.debug("Sorting the topics of communication alphabetically");

        List<TopicOfCommunication> topicOfCommunications = topicOfCommunicationRepository.orderByName();
        long orderingId = 1;

        for (TopicOfCommunication topic : topicOfCommunications) {
            topic.setOrderingId(orderingId);
            orderingId++;
        }

        topicOfCommunicationRepository.saveAll(topicOfCommunications);
    }

    @Transactional
    @PermissionValidator(
            permissions = {
                    @PermissionMapping(
                            context = TOPICS_OF_COMMUNICATIONS,
                            permissions = {NOMENCLATURE_EDIT}
                    )
            }
    )
    @Override
    public void delete(Long id) {
        log.debug("Removing topic of communication with ID: {}", id);

        TopicOfCommunication topicOfCommunication = topicOfCommunicationRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Topic of communication not found, ID: " + id));

        if (topicOfCommunication.getStatus().equals(DELETED)) {
            log.error("Topic of communication {} is already deleted", id);
            throw new OperationNotAllowedException("id-Topic of communication " + id + " is already deleted");
        }

        if (Objects.nonNull(topicOfCommunication.getIsHardcoded()) && topicOfCommunication.getIsHardcoded()) {
            log.error("You cannot delete hardcoded topic of communication with id - {}", id);
            throw new OperationNotAllowedException("id-You cannot delete hardcoded topic of communication with id - " + id);
        }

        if (topicOfCommunicationRepository.getActiveConnectionsCount(id) > 0) {
            log.error("Item is connected to active object, cannot be deleted");
            throw new OperationNotAllowedException("Item is connected to active object, cannot be deleted");
        }

        topicOfCommunication.setStatus(DELETED);
        topicOfCommunicationRepository.save(topicOfCommunication);
    }

    @Override
    public boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses) {
        return topicOfCommunicationRepository.existsByIdAndStatusIn(id, statuses);
    }

    @Override
    public List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids) {
        return topicOfCommunicationRepository.findByIdIn(ids);
    }

    @Transactional
    public TopicOfCommunicationResponse add(TopicOfCommunicationRequest request) {
        log.debug("Adding topic of communication : {}", request);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot add item with status DELETED");
            throw new ClientException("status-Cannot add item with status DELETED", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        if (topicOfCommunicationRepository.countTopicOfCommunicationsByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0) {
            log.error("Topic of communication with such name already exists");
            throw new ClientException("Topic of communication with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long lastOrderingId = topicOfCommunicationRepository.findLastOrderingId();
        TopicOfCommunication topicOfCommunication = entityFromRequest(request);
        topicOfCommunication.setOrderingId(lastOrderingId == null ? 1 : lastOrderingId + 1);
        topicOfCommunication.setIsHardcoded(false);

        assignDefaultSelectionWhenAdding(request, topicOfCommunication);

        topicOfCommunicationRepository.save(topicOfCommunication);
        return responseFromEntity(topicOfCommunication);
    }

    public TopicOfCommunicationResponse view(Long id) {
        log.debug("Fetching topic of communication with ID: {}", id);
        TopicOfCommunication topicOfCommunication = topicOfCommunicationRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Topic of communication not found, ID: " + id));

        return responseFromEntity(topicOfCommunication);
    }

    @Transactional
    public TopicOfCommunicationResponse edit(Long id, TopicOfCommunicationRequest request) {
        log.debug("Editing topic of communication : {}, with ID: {}", request, id);

        if (request.getStatus().equals(DELETED)) {
            log.error("Cannot set DELETED status to item");
            throw new ClientException("status-Cannot set DELETED status to item", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        TopicOfCommunication topicOfCommunication = topicOfCommunicationRepository
                .findById(id)
                .orElseThrow(() -> new DomainEntityNotFoundException("id-Topic of communication not found, ID: " + id));

        if (topicOfCommunication.getStatus().equals(DELETED)) {
            log.error("Cannot edit DELETED item {}", id);
            throw new OperationNotAllowedException("status-Cannot edit DELETED item " + id);
        }

        if (Objects.nonNull(topicOfCommunication.getIsHardcoded()) && topicOfCommunication.getIsHardcoded()) {
            log.error("Cannot edit HARDCODED item {}", id);
            throw new OperationNotAllowedException("status-Cannot edit HARDCODED item " + id);
        }

        if (topicOfCommunicationRepository.countTopicOfCommunicationsByStatusAndName(request.getName().trim(), List.of(ACTIVE, INACTIVE)) > 0 && !topicOfCommunication.getName().equals(request.getName().trim())) {
            log.error("Topic of communication with such name already exists");
            throw new ClientException("Topic of communication with such name already exists", ILLEGAL_ARGUMENTS_PROVIDED);
        }

        assignDefaultSelectionWhenEditing(request, topicOfCommunication);

        topicOfCommunication.setName(request.getName().trim());
        topicOfCommunication.setStatus(request.getStatus());

        return responseFromEntity(topicOfCommunication);
    }


    public NomenclatureResponse nomenclatureResponseFromEntity(TopicOfCommunication topicOfCommunication) {
        return new NomenclatureResponse(
                topicOfCommunication.getId(),
                topicOfCommunication.getName(),
                topicOfCommunication.getOrderingId(),
                topicOfCommunication.isDefaultSelection(),
                topicOfCommunication.getStatus()
        );
    }

    public TopicOfCommunicationResponse responseFromEntity(TopicOfCommunication topicOfCommunication) {
        return new TopicOfCommunicationResponse(
                topicOfCommunication.getId(),
                topicOfCommunication.getName(),
                topicOfCommunication.getOrderingId(),
                topicOfCommunication.isDefaultSelection(),
                topicOfCommunication.getStatus(),
                topicOfCommunication.getIsHardcoded()
        );
    }

    public TopicOfCommunication entityFromRequest(TopicOfCommunicationRequest request) {
        return TopicOfCommunication
                .builder()
                .name(request.getName().trim())
                .defaultSelection(request.getDefaultSelection())
                .status(request.getStatus()).build();
    }

    private void assignDefaultSelectionWhenAdding(TopicOfCommunicationRequest request, TopicOfCommunication topicOfCommunication) {
        if (request.getStatus().equals(INACTIVE)) {
            topicOfCommunication.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                Optional<TopicOfCommunication> currentDefaultTopicOfCommunicationrOptional = topicOfCommunicationRepository.findByDefaultSelectionTrue();
                if (currentDefaultTopicOfCommunicationrOptional.isPresent()) {
                    TopicOfCommunication currentDefaultTopicOfCommunication = currentDefaultTopicOfCommunicationrOptional.get();
                    currentDefaultTopicOfCommunication.setDefaultSelection(false);
                    topicOfCommunicationRepository.save(currentDefaultTopicOfCommunication);
                }
            }
            topicOfCommunication.setDefaultSelection(request.getDefaultSelection());
        }
    }

    private void assignDefaultSelectionWhenEditing(TopicOfCommunicationRequest request, TopicOfCommunication topicOfCommunication) {
        if (request.getStatus().equals(INACTIVE)) {
            topicOfCommunication.setDefaultSelection(false);
        } else {
            if (request.getDefaultSelection()) {
                if (!topicOfCommunication.isDefaultSelection()) {
                    Optional<TopicOfCommunication> optionalTopicOfCommunication = topicOfCommunicationRepository.findByDefaultSelectionTrue();
                    if (optionalTopicOfCommunication.isPresent()) {
                        TopicOfCommunication currentTopicOfCommunication = optionalTopicOfCommunication.get();
                        currentTopicOfCommunication.setDefaultSelection(false);
                        topicOfCommunicationRepository.save(currentTopicOfCommunication);
                    }
                }
            }
            topicOfCommunication.setDefaultSelection(request.getDefaultSelection());
        }
    }


}
