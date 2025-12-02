package bg.energo.phoenix.service.contract.relatedEntities;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderRelatedGoodsOrder;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderRelatedGoodsOrder;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderRelatedServiceOrder;
import bg.energo.phoenix.model.entity.contract.product.ProductContractRelatedGoodsOrder;
import bg.energo.phoenix.model.entity.contract.product.ProductContractRelatedProductContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractRelatedServiceContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractRelatedServiceOrder;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractRelatedGoodsOrder;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractRelatedServiceContract;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractRelatedServiceOrder;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.request.contract.relatedEntities.EntityType;
import bg.energo.phoenix.model.request.contract.relatedEntities.RelatedEntityRequest;
import bg.energo.phoenix.model.request.contract.relatedEntities.RelatedEntityType;
import bg.energo.phoenix.model.response.contract.productContract.FilteredContractOrderEntityResponse;
import bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderRelatedGoodsOrderRepository;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderRepository;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderRelatedGoodsOrderRepository;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderRelatedServiceOrderRepository;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderRepository;
import bg.energo.phoenix.repository.contract.product.*;
import bg.energo.phoenix.repository.contract.service.ServiceContractRelatedGoodsOrderRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractRelatedServiceContractRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractRelatedServiceOrderRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.util.epb.EPBStringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Stream;

import static bg.energo.phoenix.model.request.contract.relatedEntities.RelatedEntityType.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RelatedContractsAndOrdersService {

    private final ProductContractRepository productContractRepository;
    private final ServiceContractsRepository serviceContractsRepository;
    private final GoodsOrderRepository goodsOrderRepository;
    private final ServiceOrderRepository serviceOrderRepository;

    private final ProductContractRelatedProductContractRepository productContractRelatedProductContractRepository;
    private final ProductContractRelatedServiceContractRepository productContractRelatedServiceContractRepository;
    private final ProductContractRelatedServiceOrderRepository productContractRelatedServiceOrderRepository;
    private final ProductContractRelatedGoodsOrderRepository productContractRelatedGoodsOrderRepository;
    private final ServiceContractRelatedServiceContractRepository serviceContractRelatedServiceContractRepository;
    private final ServiceContractRelatedServiceOrderRepository serviceContractRelatedServiceOrderRepository;
    private final ServiceContractRelatedGoodsOrderRepository serviceContractRelatedGoodsOrderRepository;
    private final ServiceOrderRelatedServiceOrderRepository serviceOrderRelatedServiceOrderRepository;
    private final ServiceOrderRelatedGoodsOrderRepository serviceOrderRelatedGoodsOrderRepository;
    private final GoodsOrderRelatedGoodsOrderRepository goodsOrderRelatedGoodsOrderRepository;


    /**
     * Filters contracts or orders based on the provided prompt and entity type.
     * This method queries either the contract repository or the order repository
     * depending on the specified entity type to retrieve a list of filtered results.
     *
     * @param prompt the search string used to filter contracts or orders
     * @param type the type of entity to filter; should be either CONTRACT or ORDER
     * @return a list of filtered contract or order entity responses
     * @throws DomainEntityNotFoundException if no matching entities are found for the given prompt and type
     */
    public Page<FilteredContractOrderEntityResponse> filterContractsAndOrders(String prompt, EntityType type, int page, int size) {
        log.debug("filtering contracts and orders by prompt: {}", prompt);

        Page<FilteredContractOrderEntityResponse> response;
        if (type.equals(EntityType.CONTRACT)) {
            response = productContractRepository.filterContracts(EPBStringUtils.fromPromptToQueryParameter(prompt), PageRequest.of(page, size));
        } else {
            response = goodsOrderRepository.filterOrders(EPBStringUtils.fromPromptToQueryParameter(prompt), PageRequest.of(page, size));
        }

        if (response == null) {
            log.debug("No object found with prompt: {}", prompt);
            throw new DomainEntityNotFoundException("No object found with prompt: %s and type: %s;".formatted(prompt, type));
        }

        return response;
    }


    /**
     * Gets all related entities for the selected object with extended information on the relation.
     *
     * @param objectId   id of the selected object for which the relations are being processed
     * @param objectType type of the selected object for which the relations are being processed
     * @return list of all related entities
     */
    public List<RelatedEntityResponse> getRelatedEntities(Long objectId, RelatedEntityType objectType) {
        switch (objectType) {
            case PRODUCT_CONTRACT -> {
                return getRelatedEntitiesForProductContract(objectId);
            }
            case SERVICE_CONTRACT -> {
                return getRelatedEntitiesForServiceContract(objectId);
            }
            case SERVICE_ORDER -> {
                return getRelatedEntitiesForServiceOrder(objectId);
            }
            case GOODS_ORDER -> {
                return getRelatedEntitiesForGoodsOrder(objectId);
            }
            default -> throw new IllegalStateException("Unexpected value: " + objectType);
        }
    }


    /**
     * Gets all related entities for the selected product contract with extended information on the relation.
     *
     * @param objectId id of the selected object for which the relations are being processed
     * @return list of all related entities
     */
    private List<RelatedEntityResponse> getRelatedEntitiesForGoodsOrder(Long objectId) {
        List<RelatedEntityResponse> goodsOrderToGoodsOrders = goodsOrderRelatedGoodsOrderRepository
                .findByGoodsOrderIdAndStatusIn(objectId, List.of(EntityStatus.ACTIVE));

        List<RelatedEntityResponse> goodsOrderToProductContracts = productContractRelatedGoodsOrderRepository
                .findByGoodsOrderIdAndStatusIn(objectId, List.of(EntityStatus.ACTIVE));

        List<RelatedEntityResponse> goodsOrderToServiceContracts = serviceContractRelatedGoodsOrderRepository
                .findByGoodsOrderIdAndStatusIn(objectId, List.of(EntityStatus.ACTIVE));

        List<RelatedEntityResponse> goodsOrderToServiceOrders = serviceOrderRelatedGoodsOrderRepository
                .findByGoodsOrderIdAndStatusIn(objectId, List.of(EntityStatus.ACTIVE));

        return Stream.of(
                        goodsOrderToGoodsOrders,
                        goodsOrderToProductContracts,
                        goodsOrderToServiceContracts,
                        goodsOrderToServiceOrders
                )
                .flatMap(List::stream)
                .sorted(Comparator.comparing(RelatedEntityResponse::getCreateDate))
                .toList();
    }


    /**
     * Gets all related entities for the selected service order with extended information on the relation.
     *
     * @param objectId id of the selected object for which the relations are being processed
     * @return list of all related entities
     */
    private List<RelatedEntityResponse> getRelatedEntitiesForServiceOrder(Long objectId) {
        List<RelatedEntityResponse> serviceOrderToServiceOrders = serviceOrderRelatedServiceOrderRepository
                .findByServiceOrderIdAndStatusIn(objectId, List.of(EntityStatus.ACTIVE));

        List<RelatedEntityResponse> serviceOrderToProductContracts = productContractRelatedServiceOrderRepository
                .findByServiceOrderIdAndStatusIn(objectId, List.of(EntityStatus.ACTIVE));

        List<RelatedEntityResponse> serviceOrderToServiceContracts = serviceContractRelatedServiceOrderRepository
                .findByServiceOrderIdAndStatusIn(objectId, List.of(EntityStatus.ACTIVE));

        List<RelatedEntityResponse> serviceOrderToGoodsOrders = serviceOrderRelatedGoodsOrderRepository
                .findByServiceOrderIdAndStatusIn(objectId, List.of(EntityStatus.ACTIVE));

        return Stream.of(
                        serviceOrderToServiceOrders,
                        serviceOrderToProductContracts,
                        serviceOrderToServiceContracts,
                        serviceOrderToGoodsOrders
                )
                .flatMap(List::stream)
                .sorted(Comparator.comparing(RelatedEntityResponse::getCreateDate))
                .toList();
    }


    /**
     * Gets all related entities for the selected service contract with extended information on the relation.
     *
     * @param objectId id of the selected object for which the relations are being processed
     * @return list of all related entities
     */
    private List<RelatedEntityResponse> getRelatedEntitiesForServiceContract(Long objectId) {
        List<RelatedEntityResponse> serviceContractToServiceContracts = serviceContractRelatedServiceContractRepository
                .findByServiceContractIdAndStatusIn(objectId, List.of(EntityStatus.ACTIVE));

        List<RelatedEntityResponse> serviceContractToProductContracts = productContractRelatedServiceContractRepository
                .findByServiceContractIdAndStatusIn(objectId, List.of(EntityStatus.ACTIVE));

        List<RelatedEntityResponse> serviceContractToServiceOrders = serviceContractRelatedServiceOrderRepository
                .findByServiceContractIdAndStatusIn(objectId, List.of(EntityStatus.ACTIVE));

        List<RelatedEntityResponse> serviceContractToGoodsOrders = serviceContractRelatedGoodsOrderRepository
                .findByServiceContractIdAndStatusIn(objectId, List.of(EntityStatus.ACTIVE));

        return Stream.of(
                        serviceContractToServiceContracts,
                        serviceContractToProductContracts,
                        serviceContractToServiceOrders,
                        serviceContractToGoodsOrders
                )
                .flatMap(List::stream)
                .sorted(Comparator.comparing(RelatedEntityResponse::getCreateDate))
                .toList();
    }


    /**
     * Gets all related entities for the selected product contract with extended information on the relation.
     *
     * @param objectId id of the selected object for which the relations are being processed
     * @return list of all related entities
     */
    private List<RelatedEntityResponse> getRelatedEntitiesForProductContract(Long objectId) {
        List<RelatedEntityResponse> productContractToProductContracts = productContractRelatedProductContractRepository
                .findByProductContractIdAndStatusIn(objectId, List.of(EntityStatus.ACTIVE));

        List<RelatedEntityResponse> productContractToServiceContracts = productContractRelatedServiceContractRepository
                .findByProductContractIdAndStatusIn(objectId, List.of(EntityStatus.ACTIVE));

        List<RelatedEntityResponse> productContractToServiceOrders = productContractRelatedServiceOrderRepository
                .findByProductContractIdAndStatusIn(objectId, List.of(EntityStatus.ACTIVE));

        List<RelatedEntityResponse> productContractToGoodsOrders = productContractRelatedGoodsOrderRepository
                .findByProductContractIdAndStatusIn(objectId, List.of(EntityStatus.ACTIVE));

        return Stream.of(
                        productContractToProductContracts,
                        productContractToServiceContracts,
                        productContractToServiceOrders,
                        productContractToGoodsOrders
                )
                .flatMap(List::stream)
                .sorted(Comparator.comparing(RelatedEntityResponse::getCreateDate))
                .toList();
    }


    /**
     * Processes new entity relations depending on the selected object type.
     *
     * @param objectId      id of the selected object for which the relations are being processed
     * @param objectType    type of the selected object for which the relations are being processed
     * @param requests      list of all related entities
     * @param errorMessages list of error messages that will be returned to the user
     */
    public void createEntityRelations(Long objectId,
                                      RelatedEntityType objectType,
                                      List<RelatedEntityRequest> requests,
                                      List<String> errorMessages) {
        handleRetainedRelations(objectId, objectType, requests, errorMessages);
    }


    /**
     * Processes entity relations depending on the selected object type.
     *
     * @param objectId      id of the selected object for which the relations are being processed
     * @param objectType    type of the selected object for which the relations are being processed
     * @param requests      list of all related entities
     * @param errorMessages list of error messages that will be returned to the user
     */
    public void updateEntityRelations(Long objectId,
                                      RelatedEntityType objectType,
                                      List<RelatedEntityRequest> requests,
                                      List<String> errorMessages) {
        switch (objectType) {
            case PRODUCT_CONTRACT -> handleProductContractRelations(objectId, objectType, requests, errorMessages);
            case SERVICE_CONTRACT -> handleServiceContractRelations(objectId, objectType, requests, errorMessages);
            case SERVICE_ORDER -> handleServiceOrderRelations(objectId, objectType, requests, errorMessages);
            case GOODS_ORDER -> handleGoodsOrderRelations(objectId, objectType, requests, errorMessages);
        }
    }


    /**
     * Handles entity relations depending on the selected object type.
     *
     * @param objectId      id of the selected object for which the relations are being processed
     * @param objectType    type of the selected object for which the relations are being processed
     * @param requests      list of all related entities
     * @param errorMessages list of error messages that will be returned to the user
     */
    private void handleProductContractRelations(Long objectId, RelatedEntityType objectType, List<RelatedEntityRequest> requests, List<String> errorMessages) {
        deleteRemovedProductContractToProductContractRelations(objectId, requests);
        deleteRemovedProductContractToServiceOrderRelations(objectId, objectType, requests);
        deleteRemovedProductContractToServiceContractRelations(objectId, objectType, requests);
        deleteRemovedProductContractToGoodsOrderRelations(objectId, objectType, requests);
        handleRetainedRelations(objectId, objectType, requests, errorMessages);
    }


    /**
     * Handles entity relations depending on the selected object type.
     *
     * @param objectId      id of the selected object for which the relations are being processed
     * @param objectType    type of the selected object for which the relations are being processed
     * @param requests      list of all related entities
     * @param errorMessages list of error messages that will be returned to the user
     */
    private void handleServiceContractRelations(Long objectId, RelatedEntityType objectType, List<RelatedEntityRequest> requests, List<String> errorMessages) {
        deleteRemovedServiceContractToServiceContractRelations(objectId, requests);
        deleteRemovedProductContractToServiceContractRelations(objectId, objectType, requests);
        deleteRemovedServiceContractToServiceOrderRelations(objectId, objectType, requests);
        deleteRemovedServiceContractToGoodsOrderRelations(objectId, objectType, requests);
        handleRetainedRelations(objectId, objectType, requests, errorMessages);
    }


    /**
     * Handles entity relations depending on the selected object type.
     *
     * @param objectId      id of the selected object for which the relations are being processed
     * @param objectType    type of the selected object for which the relations are being processed
     * @param requests      list of all related entities
     * @param errorMessages list of error messages that will be returned to the user
     */
    private void handleServiceOrderRelations(Long objectId, RelatedEntityType objectType, List<RelatedEntityRequest> requests, List<String> errorMessages) {
        deleteRemovedServiceOrderToServiceOrderRelations(objectId, requests);
        deleteRemovedProductContractToServiceOrderRelations(objectId, objectType, requests);
        deleteRemovedServiceContractToServiceOrderRelations(objectId, objectType, requests);
        deleteRemovedServiceOrderToGoodsOrderRelations(objectId, objectType, requests);
        handleRetainedRelations(objectId, objectType, requests, errorMessages);
    }


    /**
     * Handles entity relations depending on the selected object type.
     *
     * @param objectId      id of the selected object for which the relations are being processed
     * @param objectType    type of the selected object for which the relations are being processed
     * @param requests      list of all related entities
     * @param errorMessages list of error messages that will be returned to the user
     */
    private void handleGoodsOrderRelations(Long objectId, RelatedEntityType objectType, List<RelatedEntityRequest> requests, List<String> errorMessages) {
        deleteRemovedGoodsOrderToGoodsOrderRelations(objectId, requests);
        deleteRemovedProductContractToGoodsOrderRelations(objectId, objectType, requests);
        deleteRemovedServiceContractToGoodsOrderRelations(objectId, objectType, requests);
        deleteRemovedServiceOrderToGoodsOrderRelations(objectId, objectType, requests);
        handleRetainedRelations(objectId, objectType, requests, errorMessages);
    }


    /**
     * Handles retained and newly added relations depending on the selected object type.
     *
     * @param requests      list of all related entities
     * @param errorMessages list of error messages that will be returned to the user
     */
    private void handleRetainedRelations(Long objectId,
                                         RelatedEntityType objectType,
                                         List<RelatedEntityRequest> requests,
                                         List<String> errorMessages) {
        if (CollectionUtils.isEmpty(requests)) {
            return;
        }

        for (int i = 0; i < requests.size(); i++) {
            RelatedEntityRequest request = requests.get(i);

            if (request.getId() != null) {
                // User has not modified this relation (applied on update operation).
                // No need to re-validate the related entity, because deletion of an active related object is restricted.
                continue;
            }

            request.setEntityId(objectId);
            request.setEntityType(objectType);

            switch (objectType) {
                case PRODUCT_CONTRACT -> {
                    switch (request.getRelatedEntityType()) {
                        case PRODUCT_CONTRACT ->
                                relateProductContractToProductContract(objectId, request, errorMessages, i);
                        case SERVICE_ORDER ->
                                relateProductContractToServiceOrder(objectType, request, errorMessages, i);
                        case SERVICE_CONTRACT ->
                                relateProductContractToServiceContract(objectType, request, errorMessages, i);
                        case GOODS_ORDER ->
                                relatedProductContractToGoodsOrder(objectType, request, errorMessages, i);
                    }
                }
                case SERVICE_ORDER -> {
                    switch (request.getRelatedEntityType()) {
                        case PRODUCT_CONTRACT ->
                                relateProductContractToServiceOrder(objectType, request, errorMessages, i);
                        case SERVICE_CONTRACT ->
                                relateServiceContractToServiceOrder(objectType, request, errorMessages, i);
                        case GOODS_ORDER -> relateServiceOrderToGoodsOrder(objectType, request, errorMessages, i);
                        case SERVICE_ORDER -> relateServiceOrderToServiceOrder(objectId, request, errorMessages, i);
                    }
                }
                case SERVICE_CONTRACT -> {
                    switch (request.getRelatedEntityType()) {
                        case PRODUCT_CONTRACT ->
                                relateProductContractToServiceContract(objectType, request, errorMessages, i);
                        case SERVICE_ORDER ->
                                relateServiceContractToServiceOrder(objectType, request, errorMessages, i);
                        case SERVICE_CONTRACT ->
                                relateServiceContractToServiceContract(objectId, request, errorMessages, i);
                        case GOODS_ORDER ->
                                relateServiceContractToGoodsOrder(objectType, request, errorMessages, i);
                    }
                }
                case GOODS_ORDER -> {
                    switch (request.getRelatedEntityType()) {
                        case PRODUCT_CONTRACT ->
                                relatedProductContractToGoodsOrder(objectType, request, errorMessages, i);
                        case SERVICE_ORDER -> relateServiceOrderToGoodsOrder(objectType, request, errorMessages, i);
                        case SERVICE_CONTRACT ->
                                relateServiceContractToGoodsOrder(objectType, request, errorMessages, i);
                        case GOODS_ORDER -> relateGoodsOrderToGoodsOrder(objectId, request, errorMessages, i);
                    }
                }
                default -> throw new IllegalStateException("Unexpected value: " + objectType);
            }
        }
    }


    /**
     * Sets deleted status to removed goods order to goods order relations.
     *
     * @param objectId id of the selected object for which the relations are being deleted
     * @param requests list of all related entities
     */
    private void deleteRemovedGoodsOrderToGoodsOrderRelations(Long objectId, List<RelatedEntityRequest> requests) {
        log.debug("deleting removed goods order to goods order relations.");

        List<Long> goodsOrderToGoodsOrderList = Objects.requireNonNullElse(requests, new ArrayList<RelatedEntityRequest>())
                .stream()
                .filter(r -> r.getId() != null && GOODS_ORDER.equals(r.getEntityType()) && GOODS_ORDER.equals(r.getRelatedEntityType()))
                .map(RelatedEntityRequest::getId)
                .toList();

        List<GoodsOrderRelatedGoodsOrder> persistedGoodsOrderToGoodsOrderRelations = goodsOrderRelatedGoodsOrderRepository
                .findByOrderIdAndStatusIn(objectId, List.of(EntityStatus.ACTIVE));

        for (GoodsOrderRelatedGoodsOrder relation : persistedGoodsOrderToGoodsOrderRelations) {
            if (!goodsOrderToGoodsOrderList.contains(relation.getId())) {
                relation.setStatus(EntityStatus.DELETED);
                goodsOrderRelatedGoodsOrderRepository.save(relation);
            }
        }
    }


    /**
     * Sets deleted status to removed service order to goods order relations.
     *
     * @param objectId   id of the selected object for which the relations are being deleted
     * @param objectType type of the selected object for which the relations are being deleted
     * @param requests   list of all related entities
     */
    private void deleteRemovedServiceOrderToGoodsOrderRelations(Long objectId, RelatedEntityType objectType, List<RelatedEntityRequest> requests) {
        log.debug("deleting removed service order to goods order relations.");

        List<Long> serviceOrderToGoodsOrderList = Objects.requireNonNullElse(requests, new ArrayList<RelatedEntityRequest>())
                .stream()
                .filter(r -> r.getId() != null && SERVICE_ORDER.equals(r.getEntityType()) && GOODS_ORDER.equals(r.getRelatedEntityType()))
                .map(RelatedEntityRequest::getId)
                .toList();

        List<ServiceOrderRelatedGoodsOrder> persistedServiceOrderToGoodsOrderRelations = serviceOrderRelatedGoodsOrderRepository
                .findByServiceOrderIdOrGoodsOrderIdAndStatusIn(objectType.name(), objectId, List.of(EntityStatus.ACTIVE));

        for (ServiceOrderRelatedGoodsOrder relation : persistedServiceOrderToGoodsOrderRelations) {
            if (!serviceOrderToGoodsOrderList.contains(relation.getId())) {
                relation.setStatus(EntityStatus.DELETED);
                serviceOrderRelatedGoodsOrderRepository.save(relation);
            }
        }
    }


    /**
     * Sets deleted status to removed service order to service order relations.
     *
     * @param objectId id of the selected object for which the relations are being deleted
     * @param requests list of all related entities
     */
    private void deleteRemovedServiceOrderToServiceOrderRelations(Long objectId, List<RelatedEntityRequest> requests) {
        log.debug("deleting removed service order to service order relations.");

        List<Long> serviceOrderToServiceOrderList = Objects.requireNonNullElse(requests, new ArrayList<RelatedEntityRequest>())
                .stream()
                .filter(r -> r.getId() != null && SERVICE_ORDER.equals(r.getEntityType()) && SERVICE_ORDER.equals(r.getRelatedEntityType()))
                .map(RelatedEntityRequest::getId)
                .toList();

        List<ServiceOrderRelatedServiceOrder> persistedServiceOrderToServiceOrderRelations = serviceOrderRelatedServiceOrderRepository
                .findByOrderIdAndStatusIn(objectId, List.of(EntityStatus.ACTIVE));

        for (ServiceOrderRelatedServiceOrder relation : persistedServiceOrderToServiceOrderRelations) {
            if (!serviceOrderToServiceOrderList.contains(relation.getId())) {
                relation.setStatus(EntityStatus.DELETED);
                serviceOrderRelatedServiceOrderRepository.save(relation);
            }
        }
    }


    /**
     * Sets deleted status to removed service contract to goods order relations.
     *
     * @param objectId   id of the selected object for which the relations are being deleted
     * @param objectType type of the selected object for which the relations are being deleted
     * @param requests   list of all related entities
     */
    private void deleteRemovedServiceContractToGoodsOrderRelations(Long objectId, RelatedEntityType objectType, List<RelatedEntityRequest> requests) {
        log.debug("deleting removed service contract to goods order relations.");

        List<Long> serviceContractToGoodsOrderList = Objects.requireNonNullElse(requests, new ArrayList<RelatedEntityRequest>())
                .stream()
                .filter(r -> r.getId() != null && SERVICE_CONTRACT.equals(r.getEntityType()) && GOODS_ORDER.equals(r.getRelatedEntityType()))
                .map(RelatedEntityRequest::getId)
                .toList();

        List<ServiceContractRelatedGoodsOrder> persistedServiceContractToGoodsOrderRelations = serviceContractRelatedGoodsOrderRepository
                .findByServiceContractIdOrGoodsOrderIdAndStatusIn(objectType.name(), objectId, List.of(EntityStatus.ACTIVE));

        for (ServiceContractRelatedGoodsOrder relation : persistedServiceContractToGoodsOrderRelations) {
            if (!serviceContractToGoodsOrderList.contains(relation.getId())) {
                relation.setStatus(EntityStatus.DELETED);
                serviceContractRelatedGoodsOrderRepository.save(relation);
            }
        }
    }


    /**
     * Sets deleted status to removed service contract to service order relations.
     *
     * @param objectId   id of the selected object for which the relations are being deleted
     * @param objectType type of the selected object for which the relations are being deleted
     * @param requests   list of all related entities
     */
    private void deleteRemovedServiceContractToServiceOrderRelations(Long objectId, RelatedEntityType objectType, List<RelatedEntityRequest> requests) {
        log.debug("deleting removed service contract to service order relations.");

        List<Long> serviceContractToServiceOrderList = Objects.requireNonNullElse(requests, new ArrayList<RelatedEntityRequest>())
                .stream()
                .filter(r -> r.getId() != null && SERVICE_CONTRACT.equals(r.getEntityType()) && SERVICE_ORDER.equals(r.getRelatedEntityType()))
                .map(RelatedEntityRequest::getId)
                .toList();

        List<ServiceContractRelatedServiceOrder> persistedServiceContractToServiceOrderRelations = serviceContractRelatedServiceOrderRepository
                .findByServiceContractIdOrServiceOrderIdAndStatusIn(objectType.name(), objectId, List.of(EntityStatus.ACTIVE));

        for (ServiceContractRelatedServiceOrder relation : persistedServiceContractToServiceOrderRelations) {
            if (!serviceContractToServiceOrderList.contains(relation.getId())) {
                relation.setStatus(EntityStatus.DELETED);
                serviceContractRelatedServiceOrderRepository.save(relation);
            }
        }
    }


    /**
     * Sets deleted status to removed service contract to service contract relations.
     *
     * @param objectId id of the selected object for which the relations are being deleted
     * @param requests list of all related entities
     */
    private void deleteRemovedServiceContractToServiceContractRelations(Long objectId, List<RelatedEntityRequest> requests) {
        log.debug("deleting removed service contract to service contract relations.");

        List<Long> serviceContractToServiceContractsList = Objects.requireNonNullElse(requests, new ArrayList<RelatedEntityRequest>())
                .stream()
                .filter(r -> r.getId() != null && SERVICE_CONTRACT.equals(r.getEntityType()) && SERVICE_CONTRACT.equals(r.getRelatedEntityType()))
                .map(RelatedEntityRequest::getId)
                .toList();

        List<ServiceContractRelatedServiceContract> persistedServiceContractToServiceContractRelations = serviceContractRelatedServiceContractRepository
                .findByContractIdAndStatusIn(objectId, List.of(EntityStatus.ACTIVE));

        for (ServiceContractRelatedServiceContract relation : persistedServiceContractToServiceContractRelations) {
            if (!serviceContractToServiceContractsList.contains(relation.getId())) {
                relation.setStatus(EntityStatus.DELETED);
                serviceContractRelatedServiceContractRepository.save(relation);
            }
        }
    }


    /**
     * Sets deleted status to removed product contract to goods order relations.
     *
     * @param objectId           id of the selected object for which the relations are being deleted
     * @param selectedObjectType type of the selected object for which the relations are being deleted
     * @param requests           list of all related entities
     */
    private void deleteRemovedProductContractToGoodsOrderRelations(Long objectId, RelatedEntityType selectedObjectType, List<RelatedEntityRequest> requests) {
        log.debug("deleting removed product contract to goods order relations.");

        List<Long> productContractToGoodsOrderList = Objects.requireNonNullElse(requests, new ArrayList<RelatedEntityRequest>())
                .stream()
                .filter(r -> r.getId() != null && PRODUCT_CONTRACT.equals(r.getEntityType()) && GOODS_ORDER.equals(r.getRelatedEntityType()))
                .map(RelatedEntityRequest::getId)
                .toList();

        List<ProductContractRelatedGoodsOrder> persistedProductContractToGoodsOrderRelations = productContractRelatedGoodsOrderRepository
                .findByProductContractIdOrGoodsOrderIdAndStatusIn(selectedObjectType.name(), objectId, List.of(EntityStatus.ACTIVE));

        for (ProductContractRelatedGoodsOrder relation : persistedProductContractToGoodsOrderRelations) {
            if (!productContractToGoodsOrderList.contains(relation.getId())) {
                relation.setStatus(EntityStatus.DELETED);
                productContractRelatedGoodsOrderRepository.save(relation);
            }
        }
    }


    /**
     * Sets deleted status to removed product contract to service contract relations.
     *
     * @param objectId   id of the selected object for which the relations are being deleted
     * @param objectType type of the selected object for which the relations are being deleted
     * @param requests   list of all related entities
     */
    private void deleteRemovedProductContractToServiceContractRelations(Long objectId, RelatedEntityType objectType, List<RelatedEntityRequest> requests) {
        log.debug("deleting removed product contract to service contract relations.");

        List<Long> productContractToServiceContractList = Objects.requireNonNullElse(requests, new ArrayList<RelatedEntityRequest>())
                .stream()
                .filter(r -> r.getId() != null && PRODUCT_CONTRACT.equals(r.getEntityType()) && SERVICE_CONTRACT.equals(r.getRelatedEntityType()))
                .map(RelatedEntityRequest::getId)
                .toList();

        List<ProductContractRelatedServiceContract> persistedProductContractToServiceContractRelations = productContractRelatedServiceContractRepository
                .findByProductContractIdOrServiceContractIdAndStatusIn(objectType.name(), objectId, List.of(EntityStatus.ACTIVE));

        for (ProductContractRelatedServiceContract relation : persistedProductContractToServiceContractRelations) {
            if (!productContractToServiceContractList.contains(relation.getId())) {
                relation.setStatus(EntityStatus.DELETED);
                productContractRelatedServiceContractRepository.save(relation);
            }
        }
    }


    /**
     * Sets deleted status to removed product contract to service order relations.
     *
     * @param objectId   id of the selected object for which the relations are being deleted
     * @param objectType type of the selected object for which the relations are being deleted
     * @param requests   list of all related entities
     */
    private void deleteRemovedProductContractToServiceOrderRelations(Long objectId, RelatedEntityType objectType, List<RelatedEntityRequest> requests) {
        log.debug("deleting removed product contract to service order relations.");

        List<Long> productContractToServiceOrderList = Objects.requireNonNullElse(requests, new ArrayList<RelatedEntityRequest>())
                .stream()
                .filter(r -> r.getId() != null && PRODUCT_CONTRACT.equals(r.getEntityType()) && SERVICE_ORDER.equals(r.getRelatedEntityType()))
                .map(RelatedEntityRequest::getId)
                .toList();

        List<ProductContractRelatedServiceOrder> persistedProductContractToServiceOrderRelations = productContractRelatedServiceOrderRepository
                .findByProductContractIdOrServiceOrderIdAndStatusIn(objectType.name(), objectId, List.of(EntityStatus.ACTIVE));

        for (ProductContractRelatedServiceOrder relation : persistedProductContractToServiceOrderRelations) {
            if (!productContractToServiceOrderList.contains(relation.getId())) {
                relation.setStatus(EntityStatus.DELETED);
                productContractRelatedServiceOrderRepository.save(relation);
            }
        }
    }


    /**
     * Sets deleted status to removed product contract to product contract relations.
     *
     * @param objectId id of the selected product contract for which the relations are being deleted
     * @param requests list of all related entities
     */
    private void deleteRemovedProductContractToProductContractRelations(Long objectId, List<RelatedEntityRequest> requests) {
        log.debug("deleting removed product contract to product contract relations.");

        List<Long> productContractToProductContractList = Objects.requireNonNullElse(requests, new ArrayList<RelatedEntityRequest>())
                .stream()
                .filter(r -> r.getId() != null && PRODUCT_CONTRACT.equals(r.getEntityType()) && PRODUCT_CONTRACT.equals(r.getRelatedEntityType()))
                .map(RelatedEntityRequest::getId)
                .toList();

        List<ProductContractRelatedProductContract> persistedProductContractToProductContractRelations = productContractRelatedProductContractRepository
                .findByContractIdAndStatusIn(objectId, List.of(EntityStatus.ACTIVE));

        for (ProductContractRelatedProductContract relation : persistedProductContractToProductContractRelations) {
            if (!productContractToProductContractList.contains(relation.getId())) {
                relation.setStatus(EntityStatus.DELETED);
                productContractRelatedProductContractRepository.save(relation);
            }
        }
    }


    /**
     * Validates against duplicate relations and creates new product contract to product contract relation.
     *
     * @param request       request with relation data
     * @param errorMessages list of error messages that will be returned to the user
     * @param index         index of the relation in the list of all related entities
     */
    private void relateProductContractToProductContract(Long objectId,
                                                        RelatedEntityRequest request,
                                                        List<String> errorMessages,
                                                        int index) {
        log.debug("relating product contract to product contract, request: {}", request);

        if (request.getEntityId().equals(request.getRelatedEntityId())) {
            log.error("basicParameters.relatedEntities[%s]-Cannot relate contract to itself;");
            errorMessages.add("basicParameters.relatedEntities[%s]-Cannot relate contract to itself;".formatted(index));
            return;
        }

        Optional<ProductContractRelatedProductContract> persistedRelationOptional = productContractRelatedProductContractRepository
                .findByContractIdAndRelatedContractIdAndStatusIn(
                        request.getEntityId(),
                        request.getRelatedEntityId(),
                        List.of(EntityStatus.ACTIVE)
                );

        if (persistedRelationOptional.isPresent()) {
            log.error("relatedEntities[%s]-Relation already exists;".formatted(index));
            errorMessages.add("basicParameters.relatedEntities[%s]-Relation already exists;".formatted(index));
            return;
        }

        if (!productContractRepository.existsByIdAndStatusIn(request.getRelatedEntityId(), List.of(ProductContractStatus.ACTIVE))) {
            log.error("relatedEntities[%s]-Object cannot be added;".formatted(index));
            errorMessages.add("basicParameters.relatedEntities[%s]-Object cannot be added;".formatted(index));
            return;
        }

        createProductContractToProductContractRelation(objectId, request);
    }


    /**
     * Creates new product contract to product contract relation.
     *
     * @param objectId id of the selected object for which the relations are being processed
     * @param request  request with relation data
     */
    private void createProductContractToProductContractRelation(Long objectId, RelatedEntityRequest request) {
        ProductContractRelatedProductContract newRelation = new ProductContractRelatedProductContract();
        if (request.getEntityId().equals(objectId)) {
            newRelation.setProductContractId(request.getEntityId());
            newRelation.setRelatedProductContractId(request.getRelatedEntityId());
        } else {
            newRelation.setProductContractId(request.getRelatedEntityId());
            newRelation.setRelatedProductContractId(request.getEntityId());
        }
        newRelation.setStatus(EntityStatus.ACTIVE);
        productContractRelatedProductContractRepository.save(newRelation);
    }


    /**
     * Validates against duplicate relations and creates new product contract to service order relation.
     *
     * @param objectType    type of the selected object for which the relations are being processed
     * @param request       request with relation data
     * @param errorMessages list of error messages that will be returned to the user
     * @param index         index of the relation in the list of all related entities
     */
    private void relateProductContractToServiceOrder(RelatedEntityType objectType,
                                                     RelatedEntityRequest request,
                                                     List<String> errorMessages,
                                                     int index) {
        log.debug("relating product contract to service order, request: {}", request);

        Optional<ProductContractRelatedServiceOrder> persistedRelationOptional = productContractRelatedServiceOrderRepository
                .findByProductContractIdAndServiceOrderIdAndStatusIn(
                        objectType.equals(PRODUCT_CONTRACT) ? request.getEntityId() : request.getRelatedEntityId(),
                        objectType.equals(PRODUCT_CONTRACT) ? request.getRelatedEntityId() : request.getEntityId(),
                        List.of(EntityStatus.ACTIVE)
                );

        if (persistedRelationOptional.isPresent()) {
            log.error("relatedEntities[%s]-Relation already exists;".formatted(index));
            errorMessages.add("basicParameters.relatedEntities[%s]-Relation already exists;".formatted(index));
            return;
        }

        if (objectType.equals(PRODUCT_CONTRACT)) {
            if (!serviceOrderRepository.existsByIdAndStatusIn(request.getRelatedEntityId(), List.of(EntityStatus.ACTIVE))) {
                log.error("relatedEntities[%s]-Object cannot be added;".formatted(index));
                errorMessages.add("basicParameters.relatedEntities[%s]-Object cannot be added;".formatted(index));
                return;
            }
        } else {
            if (!productContractRepository.existsByIdAndStatusIn(request.getRelatedEntityId(), List.of(ProductContractStatus.ACTIVE))) {
                log.error("relatedEntities[%s]-Object cannot be added;".formatted(index));
                errorMessages.add("basicParameters.relatedEntities[%s]-Object cannot be added;".formatted(index));
                return;
            }
        }

        createProductContractToServiceOrderRelation(objectType, request);
    }


    /**
     * Creates new product contract to service order relation.
     *
     * @param objectType type of the selected object for which the relations are being processed
     * @param request    request with relation data
     */
    private void createProductContractToServiceOrderRelation(RelatedEntityType objectType, RelatedEntityRequest request) {
        ProductContractRelatedServiceOrder newRelation = new ProductContractRelatedServiceOrder();
        if (objectType.equals(PRODUCT_CONTRACT)) {
            newRelation.setProductContractId(request.getEntityId());
            newRelation.setServiceOrderId(request.getRelatedEntityId());
        } else {
            newRelation.setProductContractId(request.getRelatedEntityId());
            newRelation.setServiceOrderId(request.getEntityId());
        }
        newRelation.setStatus(EntityStatus.ACTIVE);
        productContractRelatedServiceOrderRepository.save(newRelation);
    }


    /**
     * Validates against duplicate relations and creates new product contract to service contract relation.
     *
     * @param objectType    type of the selected object for which the relations are being processed
     * @param request       request with relation data
     * @param errorMessages list of error messages that will be returned to the user
     * @param index         index of the relation in the list of all related entities
     */
    private void relateProductContractToServiceContract(RelatedEntityType objectType,
                                                        RelatedEntityRequest request,
                                                        List<String> errorMessages,
                                                        int index) {
        log.debug("relating product contract to service contract, request: {}", request);

        Optional<ProductContractRelatedServiceContract> persistedReltionOptional = productContractRelatedServiceContractRepository
                .findByProductContractIdAndServiceContractIdAndStatusIn(
                        objectType.equals(PRODUCT_CONTRACT) ? request.getEntityId() : request.getRelatedEntityId(),
                        objectType.equals(PRODUCT_CONTRACT) ? request.getRelatedEntityId() : request.getEntityId(),
                        List.of(EntityStatus.ACTIVE)
                );

        if (persistedReltionOptional.isPresent()) {
            log.error("relatedEntities[%s]-Relation already exists;".formatted(index));
            errorMessages.add("basicParameters.relatedEntities[%s]-Relation already exists;".formatted(index));
            return;
        }

        if (objectType.equals(PRODUCT_CONTRACT)) {
            if (!serviceContractsRepository.existsByIdAndStatusIn(request.getRelatedEntityId(), List.of(EntityStatus.ACTIVE))) {
                log.error("relatedEntities[%s]-Object cannot be added;".formatted(index));
                errorMessages.add("basicParameters.relatedEntities[%s]-Object cannot be added;".formatted(index));
                return;
            }
        } else {
            if (!productContractRepository.existsByIdAndStatusIn(request.getRelatedEntityId(), List.of(ProductContractStatus.ACTIVE))) {
                log.error("relatedEntities[%s]-Object cannot be added;".formatted(index));
                errorMessages.add("basicParameters.relatedEntities[%s]-Object cannot be added;".formatted(index));
                return;
            }
        }

        createProductContractToServiceContractRelation(objectType, request);
    }


    /**
     * Creates new product contract to service contract relation.
     *
     * @param objectType type of the selected object for which the relations are being processed
     * @param request    request with relation data
     */
    private void createProductContractToServiceContractRelation(RelatedEntityType objectType, RelatedEntityRequest request) {
        ProductContractRelatedServiceContract newRelation = new ProductContractRelatedServiceContract();
        if (objectType.equals(PRODUCT_CONTRACT)) {
            newRelation.setProductContractId(request.getEntityId());
            newRelation.setServiceContractId(request.getRelatedEntityId());
        } else {
            newRelation.setProductContractId(request.getRelatedEntityId());
            newRelation.setServiceContractId(request.getEntityId());
        }
        newRelation.setStatus(EntityStatus.ACTIVE);
        productContractRelatedServiceContractRepository.save(newRelation);
    }


    /**
     * Validates against duplicate relations and creates new product contract to goods order relation.
     *
     * @param objectType    type of the selected object for which the relations are being processed
     * @param request       request with relation data
     * @param errorMessages list of error messages that will be returned to the user
     * @param index         index of the relation in the list of all related entities
     */
    private void relatedProductContractToGoodsOrder(RelatedEntityType objectType,
                                                    RelatedEntityRequest request,
                                                    List<String> errorMessages,
                                                    int index) {
        log.debug("relating product contract to goods order, request: {}", request);

        Optional<ProductContractRelatedGoodsOrder> persistedRelationOptional = productContractRelatedGoodsOrderRepository
                .findByProductContractIdAndGoodsOrderIdAndStatusIn(
                        objectType.equals(PRODUCT_CONTRACT) ? request.getEntityId() : request.getRelatedEntityId(),
                        objectType.equals(PRODUCT_CONTRACT) ? request.getRelatedEntityId() : request.getEntityId(),
                        List.of(EntityStatus.ACTIVE)
                );

        if (persistedRelationOptional.isPresent()) {
            log.error("relatedEntities[%s]-Relation already exists;".formatted(index));
            errorMessages.add("basicParameters.relatedEntities[%s]-Relation already exists;".formatted(index));
            return;
        }

        if (objectType.equals(PRODUCT_CONTRACT)) {
            if (!goodsOrderRepository.existsByIdAndStatusIn(request.getRelatedEntityId(), List.of(EntityStatus.ACTIVE))) {
                log.error("relatedEntities[%s]-Object cannot be added;".formatted(index));
                errorMessages.add("basicParameters.relatedEntities[%s]-Object cannot be added;".formatted(index));
                return;
            }
        } else {
            if (!productContractRepository.existsByIdAndStatusIn(request.getRelatedEntityId(), List.of(ProductContractStatus.ACTIVE))) {
                log.error("relatedEntities[%s]-Object cannot be added;".formatted(index));
                errorMessages.add("basicParameters.relatedEntities[%s]-Object cannot be added;".formatted(index));
                return;
            }
        }

        createProductContractToGoodsOrderRelation(objectType, request);
    }


    /**
     * Creates new product contract to goods order relation.
     *
     * @param objectType type of the selected object for which the relations are being processed
     * @param request    request with relation data
     */
    private void createProductContractToGoodsOrderRelation(RelatedEntityType objectType, RelatedEntityRequest request) {
        ProductContractRelatedGoodsOrder newRelation = new ProductContractRelatedGoodsOrder();
        if (objectType.equals(PRODUCT_CONTRACT)) {
            newRelation.setProductContractId(request.getEntityId());
            newRelation.setGoodsOrderId(request.getRelatedEntityId());
        } else {
            newRelation.setProductContractId(request.getRelatedEntityId());
            newRelation.setGoodsOrderId(request.getEntityId());
        }
        newRelation.setStatus(EntityStatus.ACTIVE);
        productContractRelatedGoodsOrderRepository.save(newRelation);
    }


    /**
     * Validates against duplicate relations and creates new service contract to goods order relation.
     *
     * @param objectId      id of the selected object for which the relations are being processed
     * @param request       request with relation data
     * @param errorMessages list of error messages that will be returned to the user
     * @param index         index of the relation in the list of all related entities
     */
    private void relateServiceContractToServiceContract(Long objectId,
                                                        RelatedEntityRequest request,
                                                        List<String> errorMessages,
                                                        int index) {
        log.debug("relating service contract to service contract, request: {}", request);

        if (request.getEntityId().equals(request.getRelatedEntityId())) {
            log.error("relatedEntities[%s]-Cannot relate contract to itself;");
            errorMessages.add("basicParameters.relatedEntities[%s]-Cannot relate contract to itself;".formatted(index));
            return;
        }

        Optional<ServiceContractRelatedServiceContract> persistedRelationOptional = serviceContractRelatedServiceContractRepository
                .findByContractIdAndRelatedContractIdAndStatusIn(
                        request.getEntityId(),
                        request.getRelatedEntityId(),
                        List.of(EntityStatus.ACTIVE)
                );

        if (persistedRelationOptional.isPresent()) {
            log.error("relatedEntities[%s]-Relation already exists;".formatted(index));
            errorMessages.add("basicParameters.relatedEntities[%s]-Relation already exists;".formatted(index));
            return;
        }

        if (!serviceContractsRepository.existsByIdAndStatusIn(request.getRelatedEntityId(), List.of(EntityStatus.ACTIVE))) {
            log.error("relatedEntities[%s]-Object cannot be added;".formatted(index));
            errorMessages.add("basicParameters.relatedEntities[%s]-Object cannot be added;".formatted(index));
            return;
        }

        createServiceContractToServiceContractRelation(objectId, request);
    }


    /**
     * Creates new service contract to service contract relation.
     *
     * @param objectId id of the selected object for which the relations are being processed
     * @param request  request with relation data
     */
    private void createServiceContractToServiceContractRelation(Long objectId, RelatedEntityRequest request) {
        ServiceContractRelatedServiceContract newRelation = new ServiceContractRelatedServiceContract();
        if (request.getEntityId().equals(objectId)) {
            newRelation.setServiceContractId(request.getEntityId());
            newRelation.setRelatedServiceContractId(request.getRelatedEntityId());
        } else {
            newRelation.setServiceContractId(request.getRelatedEntityId());
            newRelation.setRelatedServiceContractId(request.getEntityId());
        }
        newRelation.setStatus(EntityStatus.ACTIVE);
        serviceContractRelatedServiceContractRepository.save(newRelation);
    }


    /**
     * Validates against duplicate relations and creates new service contract to service order relation.
     *
     * @param objectType    type of the selected object for which the relations are being processed
     * @param request       request with relation data
     * @param errorMessages list of error messages that will be returned to the user
     * @param index         index of the relation in the list of all related entities
     */
    private void relateServiceContractToServiceOrder(RelatedEntityType objectType,
                                                     RelatedEntityRequest request,
                                                     List<String> errorMessages,
                                                     int index) {
        log.debug("relating service contract to service order, request: {}", request);

        Optional<ServiceContractRelatedServiceOrder> persistedRelationOptional = serviceContractRelatedServiceOrderRepository
                .findByServiceContractIdAndServiceOrderIdAndStatusIn(
                        objectType.equals(SERVICE_CONTRACT) ? request.getEntityId() : request.getRelatedEntityId(),
                        objectType.equals(SERVICE_CONTRACT) ? request.getRelatedEntityId() : request.getEntityId(),
                        List.of(EntityStatus.ACTIVE)
                );

        if (persistedRelationOptional.isPresent()) {
            log.error("relatedEntities[%s]-Relation already exists;".formatted(index));
            errorMessages.add("basicParameters.relatedEntities[%s]-Relation already exists;".formatted(index));
            return;
        }

        if (objectType.equals(SERVICE_CONTRACT)) {
            if (!serviceOrderRepository.existsByIdAndStatusIn(request.getRelatedEntityId(), List.of(EntityStatus.ACTIVE))) {
                log.error("relatedEntities[%s]-Object cannot be added;".formatted(index));
                errorMessages.add("basicParameters.relatedEntities[%s]-Object cannot be added;".formatted(index));
                return;
            }
        } else {
            if (!serviceContractsRepository.existsByIdAndStatusIn(request.getRelatedEntityId(), List.of(EntityStatus.ACTIVE))) {
                log.error("relatedEntities[%s]-Object cannot be added;".formatted(index));
                errorMessages.add("basicParameters.relatedEntities[%s]-Object cannot be added;".formatted(index));
                return;
            }
        }

        createServiceContractToServiceOrderRelation(objectType, request);
    }


    /**
     * Creates new service contract to service order relation.
     *
     * @param objectType type of the selected object for which the relations are being processed
     * @param request    request with relation data
     */
    private void createServiceContractToServiceOrderRelation(RelatedEntityType objectType, RelatedEntityRequest request) {
        ServiceContractRelatedServiceOrder newRelation = new ServiceContractRelatedServiceOrder();
        if (objectType.equals(SERVICE_CONTRACT)) {
            newRelation.setServiceContractId(request.getEntityId());
            newRelation.setServiceOrderId(request.getRelatedEntityId());
        } else {
            newRelation.setServiceContractId(request.getRelatedEntityId());
            newRelation.setServiceOrderId(request.getEntityId());
        }
        newRelation.setStatus(EntityStatus.ACTIVE);
        serviceContractRelatedServiceOrderRepository.save(newRelation);
    }


    /**
     * Validates against duplicate relations and creates new service contract to goods order relation.
     *
     * @param objectType    type of the selected object for which the relations are being processed
     * @param request       request with relation data
     * @param errorMessages list of error messages that will be returned to the user
     * @param index         index of the relation in the list of all related entities
     */
    private void relateServiceContractToGoodsOrder(RelatedEntityType objectType,
                                                   RelatedEntityRequest request,
                                                   List<String> errorMessages,
                                                   int index) {
        log.debug("relating service contract to goods order, request: {}", request);

        Optional<ServiceContractRelatedGoodsOrder> persistedRelationOptional = serviceContractRelatedGoodsOrderRepository
                .findByServiceContractIdAndGoodsOrderIdAndStatusIn(
                        objectType.equals(SERVICE_CONTRACT) ? request.getEntityId() : request.getRelatedEntityId(),
                        objectType.equals(SERVICE_CONTRACT) ? request.getRelatedEntityId() : request.getEntityId(),
                        List.of(EntityStatus.ACTIVE)
                );

        if (persistedRelationOptional.isPresent()) {
            log.error("relatedEntities[%s]-Relation already exists;".formatted(index));
            errorMessages.add("basicParameters.relatedEntities[%s]-Relation already exists;".formatted(index));
            return;
        }

        if (objectType.equals(SERVICE_CONTRACT)) {
            if (!goodsOrderRepository.existsByIdAndStatusIn(request.getRelatedEntityId(), List.of(EntityStatus.ACTIVE))) {
                log.error("relatedEntities[%s]-Object cannot be added;".formatted(index));
                errorMessages.add("basicParameters.relatedEntities[%s]-Object cannot be added;".formatted(index));
                return;
            }
        } else {
            if (!serviceContractsRepository.existsByIdAndStatusIn(request.getRelatedEntityId(), List.of(EntityStatus.ACTIVE))) {
                log.error("relatedEntities[%s]-Object cannot be added;".formatted(index));
                errorMessages.add("basicParameters.relatedEntities[%s]-Object cannot be added;".formatted(index));
                return;
            }
        }

        createServiceContractToGoodsOrderRelation(objectType, request);
    }


    /**
     * Creates new service contract to goods order relation.
     *
     * @param objectType type of the selected object for which the relations are being processed
     * @param request    request with relation data
     */
    private void createServiceContractToGoodsOrderRelation(RelatedEntityType objectType, RelatedEntityRequest request) {
        ServiceContractRelatedGoodsOrder newRelation = new ServiceContractRelatedGoodsOrder();
        if (objectType.equals(SERVICE_CONTRACT)) {
            newRelation.setServiceContractId(request.getEntityId());
            newRelation.setGoodsOrderId(request.getRelatedEntityId());
        } else {
            newRelation.setServiceContractId(request.getRelatedEntityId());
            newRelation.setGoodsOrderId(request.getEntityId());
        }
        newRelation.setStatus(EntityStatus.ACTIVE);
        serviceContractRelatedGoodsOrderRepository.save(newRelation);
    }


    /**
     * Validates against duplicate relations and creates new service order to goods order relation.
     *
     * @param objectId      id of the selected object for which the relations are being processed
     * @param request       request with relation data
     * @param errorMessages list of error messages that will be returned to the user
     * @param index         index of the relation in the list of all related entities
     */
    private void relateServiceOrderToServiceOrder(Long objectId,
                                                  RelatedEntityRequest request,
                                                  List<String> errorMessages,
                                                  int index) {
        log.debug("relating service order to service order, request: {}", request);

        if (request.getEntityId().equals(request.getRelatedEntityId())) {
            log.error("relatedEntities[%s]-Cannot relate order to itself;");
            errorMessages.add("basicParameters.relatedEntities[%s]-Cannot relate order to itself;".formatted(index));
            return;
        }

        Optional<ServiceOrderRelatedServiceOrder> persistedRelationOptional = serviceOrderRelatedServiceOrderRepository
                .findByOrderIdAndRelatedOrderIdAndStatusIn(
                        request.getEntityId(),
                        request.getRelatedEntityId(),
                        List.of(EntityStatus.ACTIVE)
                );

        if (persistedRelationOptional.isPresent()) {
            log.error("relatedEntities[%s]-Relation already exists;".formatted(index));
            errorMessages.add("basicParameters.relatedEntities[%s]-Relation already exists;".formatted(index));
            return;
        }

        if (!serviceOrderRepository.existsByIdAndStatusIn(request.getRelatedEntityId(), List.of(EntityStatus.ACTIVE))) {
            log.error("relatedEntities[%s]-Object cannot be added;".formatted(index));
            errorMessages.add("basicParameters.relatedEntities[%s]-Object cannot be added;".formatted(index));
            return;
        }

        createServiceOrderToServiceOrderRelation(objectId, request);
    }


    /**
     * Creates new service order to service order relation.
     *
     * @param objectId id of the selected object for which the relations are being processed
     * @param request  request with relation data
     */
    private void createServiceOrderToServiceOrderRelation(Long objectId, RelatedEntityRequest request) {
        ServiceOrderRelatedServiceOrder newRelation = new ServiceOrderRelatedServiceOrder();
        if (request.getEntityId().equals(objectId)) {
            newRelation.setServiceOrderId(request.getEntityId());
            newRelation.setRelatedServiceOrderId(request.getRelatedEntityId());
        } else {
            newRelation.setServiceOrderId(request.getRelatedEntityId());
            newRelation.setRelatedServiceOrderId(request.getEntityId());
        }
        newRelation.setStatus(EntityStatus.ACTIVE);
        serviceOrderRelatedServiceOrderRepository.save(newRelation);
    }


    /**
     * Validates against duplicate relations and creates new service order to goods order relation.
     *
     * @param objectType    type of the selected object for which the relations are being processed
     * @param request       request with relation data
     * @param errorMessages list of error messages that will be returned to the user
     * @param index         index of the relation in the list of all related entities
     */
    private void relateServiceOrderToGoodsOrder(RelatedEntityType objectType,
                                                RelatedEntityRequest request,
                                                List<String> errorMessages,
                                                int index) {
        log.debug("relating service order to goods order, request: {}", request);

        Optional<ServiceOrderRelatedGoodsOrder> persistedRelationOptional = serviceOrderRelatedGoodsOrderRepository
                .findByServiceOrderIdAndGoodsOrderIdAndStatusIn(
                        objectType.equals(SERVICE_ORDER) ? request.getEntityId() : request.getRelatedEntityId(),
                        objectType.equals(SERVICE_ORDER) ? request.getRelatedEntityId() : request.getEntityId(),
                        List.of(EntityStatus.ACTIVE)
                );

        if (persistedRelationOptional.isPresent()) {
            log.error("relatedEntities[%s]-Relation already exists;".formatted(index));
            errorMessages.add("basicParameters.relatedEntities[%s]-Relation already exists;".formatted(index));
            return;
        }

        if (objectType.equals(SERVICE_ORDER)) {
            if (!goodsOrderRepository.existsByIdAndStatusIn(request.getRelatedEntityId(), List.of(EntityStatus.ACTIVE))) {
                log.error("relatedEntities[%s]-Object cannot be added;".formatted(index));
                errorMessages.add("basicParameters.relatedEntities[%s]-Object cannot be added;".formatted(index));
                return;
            }
        } else {
            if (!serviceOrderRepository.existsByIdAndStatusIn(request.getRelatedEntityId(), List.of(EntityStatus.ACTIVE))) {
                log.error("relatedEntities[%s]-Object cannot be added;".formatted(index));
                errorMessages.add("basicParameters.relatedEntities[%s]-Object cannot be added;".formatted(index));
                return;
            }
        }

        createServiceOrderToGoodsOrderRelation(objectType, request);
    }


    /**
     * Creates new service order to goods order relation.
     *
     * @param objectType type of the selected object for which the relations are being processed
     * @param request    request with relation data
     */
    private void createServiceOrderToGoodsOrderRelation(RelatedEntityType objectType, RelatedEntityRequest request) {
        ServiceOrderRelatedGoodsOrder newRelation = new ServiceOrderRelatedGoodsOrder();
        if (objectType.equals(SERVICE_ORDER)) {
            newRelation.setServiceOrderId(request.getEntityId());
            newRelation.setGoodsOrderId(request.getRelatedEntityId());
        } else {
            newRelation.setServiceOrderId(request.getRelatedEntityId());
            newRelation.setGoodsOrderId(request.getEntityId());
        }
        newRelation.setStatus(EntityStatus.ACTIVE);
        serviceOrderRelatedGoodsOrderRepository.save(newRelation);
    }


    /**
     * Validates against duplicate relations and creates new goods order to goods order relation.
     *
     * @param objectId      id of the selected object for which the relations are being processed
     * @param request       request with relation data
     * @param errorMessages list of error messages that will be returned to the user
     * @param index         index of the relation in the list of all related entities
     */
    private void relateGoodsOrderToGoodsOrder(Long objectId,
                                              RelatedEntityRequest request,
                                              List<String> errorMessages,
                                              int index) {
        log.debug("relating goods order to goods order, request: {}", request);

        if (request.getEntityId().equals(request.getRelatedEntityId())) {
            log.error("relatedEntities[%s]-Cannot relate order to itself;");
            errorMessages.add("relatedEntities[%s]-Cannot relate order to itself;".formatted(index));
            return;
        }

        Optional<GoodsOrderRelatedGoodsOrder> persistedRelationOptional = goodsOrderRelatedGoodsOrderRepository
                .findByOrderIdAndRelatedOrderIdAndStatusIn(
                        request.getEntityId(),
                        request.getRelatedEntityId(),
                        List.of(EntityStatus.ACTIVE)
                );

        if (persistedRelationOptional.isPresent()) {
            log.error("relatedEntities[%s]-Relation already exists;".formatted(index));
            errorMessages.add("basicParameters.relatedEntities[%s]-Relation already exists;".formatted(index));
            return;
        }

        if (!goodsOrderRepository.existsByIdAndStatusIn(request.getRelatedEntityId(), List.of(EntityStatus.ACTIVE))) {
            log.error("relatedEntities[%s]-Object cannot be added;".formatted(index));
            errorMessages.add("basicParameters.relatedEntities[%s]-Object cannot be added;".formatted(index));
            return;
        }

        createGoodsOrderToGoodsOrderRelation(objectId, request);
    }


    /**
     * Creates new goods order to goods order relation.
     *
     * @param objectId id of the selected object for which the relations are being processed
     * @param request  request with relation data
     */
    private void createGoodsOrderToGoodsOrderRelation(Long objectId, RelatedEntityRequest request) {
        GoodsOrderRelatedGoodsOrder newRelation = new GoodsOrderRelatedGoodsOrder();
        if (request.getEntityId().equals(objectId)) {
            newRelation.setGoodsOrderId(request.getEntityId());
            newRelation.setRelatedGoodsOrderId(request.getRelatedEntityId());
        } else {
            newRelation.setGoodsOrderId(request.getRelatedEntityId());
            newRelation.setRelatedGoodsOrderId(request.getEntityId());
        }
        newRelation.setStatus(EntityStatus.ACTIVE);
        goodsOrderRelatedGoodsOrderRepository.save(newRelation);
    }
}
