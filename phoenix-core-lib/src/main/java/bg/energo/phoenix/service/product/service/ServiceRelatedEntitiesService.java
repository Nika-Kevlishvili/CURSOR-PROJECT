package bg.energo.phoenix.service.product.service;

import bg.energo.phoenix.model.entity.product.product.Product;
import bg.energo.phoenix.model.entity.product.service.EPService;
import bg.energo.phoenix.model.entity.product.service.ServiceDetails;
import bg.energo.phoenix.model.entity.product.service.ServiceLinkedProduct;
import bg.energo.phoenix.model.entity.product.service.ServiceLinkedService;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceSubobjectStatus;
import bg.energo.phoenix.model.request.product.service.ServiceRelatedEntityRequest;
import bg.energo.phoenix.repository.product.product.ProductRepository;
import bg.energo.phoenix.repository.product.service.ServiceDetailsRepository;
import bg.energo.phoenix.repository.product.service.ServiceRepository;
import bg.energo.phoenix.repository.product.service.subObject.ServiceLinkedProductRepository;
import bg.energo.phoenix.repository.product.service.subObject.ServiceLinkedServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceRelatedEntitiesService {
    private final ProductRepository productRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceDetailsRepository serviceDetailsRepository;
    private final ServiceLinkedProductRepository serviceLinkedProductRepository;
    private final ServiceLinkedServiceRepository serviceLinkedServiceRepository;

    @Transactional
    public void addRelatedProductsAndServicesToService(List<ServiceRelatedEntityRequest> relatedEntities, ServiceDetails serviceDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isEmpty(relatedEntities)) {
            relatedEntities = new ArrayList<>();
        }

        List<Long> availableProductIds = null;
        List<Long> availableServiceIds = null;

        for (int i = 0; i < relatedEntities.size(); i++) {
            ServiceRelatedEntityRequest relatedEntity = relatedEntities.get(i);

            switch (relatedEntity.getType()) {
                case PRODUCT -> {
                    Product product = productRepository
                            .findByIdAndProductStatusIn(relatedEntity.getId(), List.of(ProductStatus.ACTIVE))
                            .orElse(null);

                    if (product == null) {
                        exceptionMessages.add("basicSettings.relatedEntities[%s].id-Service with presented id [%s] not found;".formatted(i, relatedEntity.getId()));
                        continue;
                    }

                    if (availableProductIds == null) {
                        availableProductIds = fetchAvailableProducts();
                    }

                    if (!availableProductIds.contains(product.getId())) {
                        exceptionMessages.add(String.format("basicSettings.relatedEntities[%s].id-Service is not available;", i));
                        continue;
                    }

                    serviceLinkedProductRepository.save(
                            new ServiceLinkedProduct(
                                    null,
                                    relatedEntity.getObligatory(),
                                    serviceDetails,
                                    product,
                                    relatedEntity.getAllowSalesUnder(),
                                    ServiceSubobjectStatus.ACTIVE
                            )
                    );
                }
                case SERVICE -> {
                    EPService service = serviceRepository
                            .findByIdAndStatusIn(relatedEntity.getId(), List.of(ServiceStatus.ACTIVE))
                            .orElse(null);

                    if (serviceDetails.getService() != null && relatedEntity.getId().equals(serviceDetails.getService().getId())) {
                        exceptionMessages.add("basicSettings.relatedEntities[%s].id-Service cannot be it's own related service;".formatted(i));
                        continue;
                    }

                    if (service == null) {
                        exceptionMessages.add("basicSettings.relatedEntities[%s].id-Service with presented id [%s] not found;".formatted(i, relatedEntity.getId()));
                        continue;
                    }

                    if (availableServiceIds == null) {
                        availableServiceIds = fetchAvailableServices();
                    }

                    if (!availableServiceIds.contains(service.getId())) {
                        exceptionMessages.add("basicSettings.relatedEntities[%s].id-Service is not available;".formatted(i));
                        continue;
                    }

                    serviceLinkedServiceRepository.save(
                            new ServiceLinkedService(
                                    null,
                                    relatedEntity.getObligatory(),
                                    serviceDetails,
                                    service,
                                    relatedEntity.getAllowSalesUnder(),
                                    ServiceSubobjectStatus.ACTIVE
                            )
                    );
                }
            }
        }
    }

    @Transactional
    public void updateRelatedProductsAndServicesToProduct(List<ServiceRelatedEntityRequest> relatedEntities, ServiceDetails serviceDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isEmpty(relatedEntities)) {
            relatedEntities = new ArrayList<>();
        }

        Long currentServiceId = serviceDetails.getService().getId();

        List<Long> availableProductIds = productRepository
                .findAvailableProductIdsForProduct();
        List<Long> availableServiceIds = serviceRepository
                .findAvailableServiceIdsForProduct();

        List<ServiceLinkedProduct> activeRelatedProducts = serviceDetails
                .getLinkedProducts()
                .stream().filter(service -> service.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                .toList();

        List<ServiceLinkedService> activeRelatedServices = serviceDetails
                .getLinkedServices()
                .stream().filter(service -> service.getStatus().equals(ServiceSubobjectStatus.ACTIVE))
                .toList();

        List<Long> activeRelatedProductIds = activeRelatedProducts
                .stream()
                .map(productLinkToProduct -> productLinkToProduct.getProduct().getId())
                .toList();

        List<Long> activeRelatedServiceIds = activeRelatedServices
                .stream()
                .map(productLinkToService -> productLinkToService.getService().getId())
                .toList();

        validateEntitiesAvailability(relatedEntities, exceptionMessages, currentServiceId, availableProductIds, availableServiceIds, activeRelatedProductIds, activeRelatedServiceIds);

        if (CollectionUtils.isNotEmpty(exceptionMessages)) {
            return;
        }

        deleteOutdatedRelatedProducts(relatedEntities, activeRelatedProducts);
        deleteOutdatedRelatedServices(relatedEntities, activeRelatedServices);

        for (int i = 0; i < relatedEntities.size(); i++) {
            ServiceRelatedEntityRequest productRelatedEntityRequest = relatedEntities.get(i);

            switch (productRelatedEntityRequest.getType()) {
                case PRODUCT -> {
                    if (!activeRelatedProductIds.contains(productRelatedEntityRequest.getId())) {
                        Optional<Product> productOptional = productRepository
                                .findByIdAndProductStatusIn(productRelatedEntityRequest.getId(), List.of(ProductStatus.ACTIVE));

                        if (productOptional.isPresent()) {
                            serviceLinkedProductRepository.save(
                                    new ServiceLinkedProduct(null,
                                                             productRelatedEntityRequest.getObligatory(),
                                                             serviceDetails,
                                                             productOptional.get(),
                                                             productRelatedEntityRequest.getAllowSalesUnder(),
                                                             ServiceSubobjectStatus.ACTIVE));
                        } else {
                            exceptionMessages.add("basicSettings.relatedEntities[%s].id-Product with presented id: [%s] not found;".formatted(i, productRelatedEntityRequest.getId()));
                        }
                    } else {
                        Optional<ServiceLinkedProduct> relatedProductOptional = activeRelatedProducts
                                .stream()
                                .filter(activeLinkedProduct -> activeLinkedProduct.getProduct().getId().equals(productRelatedEntityRequest.getId()))
                                .findFirst();

                        if (relatedProductOptional.isPresent()) {
                            ServiceLinkedProduct relatedProduct = relatedProductOptional.get();
                            relatedProduct.setAllowsSalesUnder(productRelatedEntityRequest.getAllowSalesUnder());
                            relatedProduct.setServiceObligationCondition(productRelatedEntityRequest.getObligatory());

                            serviceLinkedProductRepository.save(relatedProduct);
                        } else {
                            exceptionMessages.add("basicSettings.relatedEntities[%s].id-Exception handled while trying to modify service related product with id: [%s];".formatted(i, productRelatedEntityRequest.getId()));
                        }
                    }
                }
                case SERVICE -> {
                    if (!activeRelatedServiceIds.contains(productRelatedEntityRequest.getId())) {
                        Optional<EPService> serviceOptional = serviceRepository
                                .findByIdAndStatusIn(productRelatedEntityRequest.getId(), List.of(ServiceStatus.ACTIVE));

                        if (serviceOptional.isPresent()) {
                            serviceLinkedServiceRepository
                                    .save(
                                            new ServiceLinkedService(
                                                    null,
                                                    productRelatedEntityRequest.getObligatory(),
                                                    serviceDetails,
                                                    serviceOptional.get(),
                                                    productRelatedEntityRequest.getAllowSalesUnder(),
                                                    ServiceSubobjectStatus.ACTIVE
                                            )
                                    );
                        } else {
                            exceptionMessages.add("basicSettings.relatedEntities[%s].id-Service with presented id: [%s] not found;".formatted(i, productRelatedEntityRequest.getId()));
                        }
                    } else {
                        Optional<ServiceLinkedService> relatedServiceOptional = activeRelatedServices
                                .stream()
                                .filter(activeLinkedService -> activeLinkedService.getService().getId().equals(productRelatedEntityRequest.getId()))
                                .findFirst();

                        if (relatedServiceOptional.isPresent()) {
                            ServiceLinkedService relatedService = relatedServiceOptional.get();
                            relatedService.setServiceObligationCondition(productRelatedEntityRequest.getObligatory());
                            relatedService.setAllowsSalesUnder(productRelatedEntityRequest.getAllowSalesUnder());

                            serviceLinkedServiceRepository.save(relatedService);
                        } else {
                            exceptionMessages.add("basicSettings.relatedEntities[%s].id-Exception handled while trying to modify service related service with id: [%s];".formatted(i, productRelatedEntityRequest.getId()));
                        }
                    }
                }
            }
        }
    }

    private List<Long> fetchAvailableProducts() {
        return productRepository.findAvailableProductIdsForProduct();
    }

    private List<Long> fetchAvailableServices() {
        return serviceRepository.findAvailableServiceIdsForProduct();
    }

    private void validateEntitiesAvailability(List<ServiceRelatedEntityRequest> relatedEntities, List<String> exceptionMessages, Long currentServiceId, List<Long> availableProductIds, List<Long> availableServiceIds, List<Long> activeRelatedProductIds, List<Long> activeRelatedServiceIds) {
        for (int i = 0; i < relatedEntities.size(); i++) {
            ServiceRelatedEntityRequest relatedEntityRequest = relatedEntities.get(i);
            final Long id = relatedEntityRequest.getId();

            switch (relatedEntityRequest.getType()) {
                case PRODUCT -> {
                    if (!availableProductIds.contains(id) && !activeRelatedProductIds.contains(id)) {
                        exceptionMessages.add("basicSettings.relatedEntities[%s].id-Service with presented id: [%s] not found or is not available;".formatted(i, id));
                    }
                }
                case SERVICE -> {
                    if (id.equals(currentServiceId)) {
                        exceptionMessages.add("basicSettings.relatedEntities[%s].id-Service cannot be it's own related service;".formatted(i));
                    }

                    if (!availableServiceIds.contains(id) && !activeRelatedServiceIds.contains(id)) {
                        exceptionMessages.add("basicSettings.relatedEntities[%s].id-Service with presented id: [%s] not found or is not available;".formatted(i, id));
                    }
                }
            }
        }
    }

    private void deleteOutdatedRelatedProducts(List<ServiceRelatedEntityRequest> relatedEntities, List<ServiceLinkedProduct> currentActiveRelatedProducts) {
        List<Long> relatedEntityIds =
                relatedEntities
                        .stream()
                        .map(ServiceRelatedEntityRequest::getId)
                        .toList();

        List<ServiceLinkedProduct> outdatedRelatedProducts =
                currentActiveRelatedProducts
                        .stream()
                        .filter(relatedProduct -> !relatedEntityIds.contains(relatedProduct.getProduct().getId()))
                        .toList();
        outdatedRelatedProducts
                .forEach(relatedProduct -> relatedProduct.setStatus(ServiceSubobjectStatus.DELETED));

        serviceLinkedProductRepository.saveAll(outdatedRelatedProducts);
    }

    private void deleteOutdatedRelatedServices(List<ServiceRelatedEntityRequest> relatedEntities, List<ServiceLinkedService> currentActiveRelatedServices) {
        List<Long> relatedEntityIds = relatedEntities
                .stream()
                .map(ServiceRelatedEntityRequest::getId)
                .toList();

        List<ServiceLinkedService> outdatedRelatedServices = currentActiveRelatedServices
                .stream()
                .filter(relatedServices -> !relatedEntityIds.contains(relatedServices.getService().getId()))
                .toList();
        outdatedRelatedServices
                .forEach(relatedServices -> relatedServices.setStatus(ServiceSubobjectStatus.DELETED));

        serviceLinkedServiceRepository.saveAll(outdatedRelatedServices);
    }


    /**
     * Validates whether the user can create a service contract with the service detail id and customer id
     * according to the service detail's related products and services.
     *
     * @param serviceId      ID of the service the detail of which should be validated
     * @param serviceVersion Version of the service the detail of which should be validated
     * @param customerId     ID of the customer the contract should be created for
     * @param errorMessages  List of error messages to be populated in case of validation failure
     * @return true if the user can create a service contract with the service detail id and customer id, false otherwise
     */
    public boolean canCreateServiceContractWithServiceAndCustomer(Long serviceId, Long serviceVersion, Long customerId, List<String> errorMessages) {
        log.debug("Checking if service contract can be created with service detail id: {} and customer id: {}", serviceId, customerId);

        Optional<ServiceDetails> serviceDetailOptional = serviceDetailsRepository.findByServiceIdAndVersion(serviceId, serviceVersion);
        if (serviceDetailOptional.isEmpty()) {
            log.error("Unable to validate related products/services before contract creation as service detail with id: {} and version: {} was not found", serviceId, serviceVersion);
            errorMessages.add("Unable to validate related products/services before contract creation as service detail with id: %s and version: %s was not found".formatted(serviceId, serviceVersion));
            return false;
        }

        return serviceLinkedProductRepository.canCreateContractWithServiceAndCustomer(serviceDetailOptional.get().getId(), customerId);
    }
}
