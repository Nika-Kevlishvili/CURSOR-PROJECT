package bg.energo.phoenix.service.product.product;

import bg.energo.phoenix.model.entity.product.product.Product;
import bg.energo.phoenix.model.entity.product.product.ProductDetails;
import bg.energo.phoenix.model.entity.product.product.ProductLinkToProduct;
import bg.energo.phoenix.model.entity.product.product.ProductLinkToService;
import bg.energo.phoenix.model.entity.product.service.EPService;
import bg.energo.phoenix.model.enums.product.product.ProductStatus;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.service.ServiceStatus;
import bg.energo.phoenix.model.request.product.product.ProductRelatedEntityRequest;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.repository.product.product.ProductLinkToProductRepository;
import bg.energo.phoenix.repository.product.product.ProductLinkToServiceRepository;
import bg.energo.phoenix.repository.product.product.ProductRepository;
import bg.energo.phoenix.repository.product.service.ServiceRepository;
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
public class ProductRelatedEntitiesService {
    private final ProductRepository productRepository;
    private final ProductDetailsRepository productDetailsRepository;
    private final ServiceRepository serviceRepository;
    private final ProductLinkToProductRepository productLinkToProductRepository;
    private final ProductLinkToServiceRepository productLinkToServiceRepository;

    @Transactional
    public void addRelatedProductsAndServicesToProduct(List<ProductRelatedEntityRequest> relatedEntities, ProductDetails productDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isEmpty(relatedEntities)) {
            relatedEntities = new ArrayList<>();
        }

        List<Long> availableProductIds = null;
        List<Long> availableServiceIds = null;

        for (int i = 0; i < relatedEntities.size(); i++) {
            ProductRelatedEntityRequest relatedEntity = relatedEntities.get(i);

            switch (relatedEntity.getType()) {
                case PRODUCT -> {
                    Product product = productRepository
                            .findByIdAndProductStatusIn(relatedEntity.getId(), List.of(ProductStatus.ACTIVE))
                            .orElse(null);

                    if (productDetails.getProduct() != null && relatedEntity.getId().equals(productDetails.getProduct().getId())) {
                        exceptionMessages.add("basicSettings.relatedEntities[%s].id-Product cannot be it's own related product;".formatted(i));
                        continue;
                    }

                    if (product == null) {
                        exceptionMessages.add("basicSettings.relatedEntities[%s].id-Product with presented id [%s] not found;".formatted(i, relatedEntity.getId()));
                        continue;
                    }

                    if (availableProductIds == null) {
                        availableProductIds = fetchAvailableProducts();
                    }

                    if (!availableProductIds.contains(product.getId())) {
                        exceptionMessages.add(String.format("basicSettings.relatedEntities[%s].id-Product is not available;", i));
                        continue;
                    }

                    productLinkToProductRepository.save(
                            new ProductLinkToProduct(
                                    null,
                                    productDetails,
                                    product,
                                    relatedEntity.getObligatory(),
                                    relatedEntity.getAllowSalesUnder(),
                                    ProductSubObjectStatus.ACTIVE
                            )
                    );
                }
                case SERVICE -> {
                    EPService service = serviceRepository
                            .findByIdAndStatusIn(relatedEntity.getId(), List.of(ServiceStatus.ACTIVE))
                            .orElse(null);

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

                    productLinkToServiceRepository.save(
                            new ProductLinkToService(
                                    null,
                                    productDetails,
                                    service,
                                    relatedEntity.getObligatory(),
                                    relatedEntity.getAllowSalesUnder(),
                                    ProductSubObjectStatus.ACTIVE
                            )
                    );
                }
            }
        }
    }

    @Transactional
    public void updateRelatedProductsAndServicesToProduct(List<ProductRelatedEntityRequest> relatedEntities, ProductDetails productDetails, List<String> exceptionMessages) {
        if (CollectionUtils.isEmpty(relatedEntities)) {
            relatedEntities = new ArrayList<>();
        }

        Long currentProductId = productDetails.getProduct().getId();

        List<Long> availableProductIds = productRepository
                .findAvailableProductIdsForProduct();
        List<Long> availableServiceIds = serviceRepository
                .findAvailableServiceIdsForProduct();

        List<ProductLinkToProduct> activeRelatedProducts = productDetails
                .getLinkedProducts()
                .stream().filter(product -> product.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                .toList();

        List<ProductLinkToService> activeRelatedServices = productDetails
                .getLinkedServices()
                .stream().filter(service -> service.getProductSubObjectStatus().equals(ProductSubObjectStatus.ACTIVE))
                .toList();

        List<Long> activeRelatedProductIds = activeRelatedProducts
                .stream()
                .map(productLinkToProduct -> productLinkToProduct.getLinkedProduct().getId())
                .toList();

        List<Long> activeRelatedServiceIds = activeRelatedServices
                .stream()
                .map(productLinkToService -> productLinkToService.getLinkedService().getId())
                .toList();

        validateEntitiesAvailability(relatedEntities, exceptionMessages, currentProductId, availableProductIds, availableServiceIds, activeRelatedProductIds, activeRelatedServiceIds);

        if (CollectionUtils.isNotEmpty(exceptionMessages)) {
            return;
        }

        deleteOutdatedRelatedProducts(relatedEntities, activeRelatedProducts);
        deleteOutdatedRelatedServices(relatedEntities, activeRelatedServices);

        for (int i = 0; i < relatedEntities.size(); i++) {
            ProductRelatedEntityRequest productRelatedEntityRequest = relatedEntities.get(i);

            switch (productRelatedEntityRequest.getType()) {
                case PRODUCT -> {
                    if (!activeRelatedProductIds.contains(productRelatedEntityRequest.getId())) {
                        Optional<Product> productOptional = productRepository
                                .findByIdAndProductStatusIn(productRelatedEntityRequest.getId(), List.of(ProductStatus.ACTIVE));

                        if (productOptional.isPresent()) {
                            productLinkToProductRepository.save(
                                    new ProductLinkToProduct(null,
                                                             productDetails,
                                                             productOptional.get(),
                                                             productRelatedEntityRequest.getObligatory(),
                                                             productRelatedEntityRequest.getAllowSalesUnder(),
                                                             ProductSubObjectStatus.ACTIVE));
                        } else {
                            exceptionMessages.add("basicSettings.relatedEntities[%s].id-Product with presented id: [%s] not found;".formatted(i, productRelatedEntityRequest.getId()));
                        }
                    } else {
                        Optional<ProductLinkToProduct> relatedProductOptional = activeRelatedProducts
                                .stream()
                                .filter(activeLinkedProduct -> activeLinkedProduct.getLinkedProduct().getId().equals(productRelatedEntityRequest.getId()))
                                .findFirst();

                        if (relatedProductOptional.isPresent()) {
                            ProductLinkToProduct relatedProduct = relatedProductOptional.get();
                            relatedProduct.setAllowSalesUnder(productRelatedEntityRequest.getAllowSalesUnder());
                            relatedProduct.setObligatory(productRelatedEntityRequest.getObligatory());

                            productLinkToProductRepository.save(relatedProduct);
                        } else {
                            exceptionMessages.add("basicSettings.relatedEntities[%s].id-Exception handled while trying to modify product related product with id: [%s];".formatted(i, productRelatedEntityRequest.getId()));
                        }
                    }
                }
                case SERVICE -> {
                    if (!activeRelatedServiceIds.contains(productRelatedEntityRequest.getId())) {
                        Optional<EPService> serviceOptional = serviceRepository
                                .findByIdAndStatusIn(productRelatedEntityRequest.getId(), List.of(ServiceStatus.ACTIVE));

                        if (serviceOptional.isPresent()) {
                            productLinkToServiceRepository
                                    .save(
                                            new ProductLinkToService(
                                                    null,
                                                    productDetails,
                                                    serviceOptional.get(),
                                                    productRelatedEntityRequest.getObligatory(),
                                                    productRelatedEntityRequest.getAllowSalesUnder(),
                                                    ProductSubObjectStatus.ACTIVE
                                            )
                                    );
                        } else {
                            exceptionMessages.add("basicSettings.relatedEntities[%s].id-Service with presented id: [%s] not found;".formatted(i, productRelatedEntityRequest.getId()));
                        }
                    } else {
                        Optional<ProductLinkToService> relatedServiceOptional = activeRelatedServices
                                .stream()
                                .filter(activeLinkedService -> activeLinkedService.getLinkedService().getId().equals(productRelatedEntityRequest.getId()))
                                .findFirst();

                        if (relatedServiceOptional.isPresent()) {
                            ProductLinkToService relatedService = relatedServiceOptional.get();
                            relatedService.setObligatory(productRelatedEntityRequest.getObligatory());
                            relatedService.setAllowSalesUnder(productRelatedEntityRequest.getAllowSalesUnder());

                            productLinkToServiceRepository.save(relatedService);
                        } else {
                            exceptionMessages.add("basicSettings.relatedEntities[%s].id-Exception handled while trying to modify product related service with id: [%s];".formatted(i, productRelatedEntityRequest.getId()));
                        }
                    }
                }
            }
        }
    }

    private void validateEntitiesAvailability(List<ProductRelatedEntityRequest> relatedEntities, List<String> exceptionMessages, Long currentProductId, List<Long> availableProductIds, List<Long> availableServiceIds, List<Long> activeRelatedProductIds, List<Long> activeRelatedServiceIds) {
        for (int i = 0; i < relatedEntities.size(); i++) {
            ProductRelatedEntityRequest relatedEntityRequest = relatedEntities.get(i);
            final Long id = relatedEntityRequest.getId();

            switch (relatedEntityRequest.getType()) {
                case PRODUCT -> {
                    if (id.equals(currentProductId)) {
                        exceptionMessages.add("basicSettings.relatedEntities[%s].id-Product cannot be it's own related product;".formatted(i));
                    }

                    if (!availableProductIds.contains(id) && !activeRelatedProductIds.contains(id)) {
                        exceptionMessages.add("basicSettings.relatedEntities[%s].id-Product with presented id: [%s] not found or is not available;".formatted(i, id));
                    }
                }
                case SERVICE -> {
                    if (!availableServiceIds.contains(id) && !activeRelatedServiceIds.contains(id)) {
                        exceptionMessages.add("basicSettings.relatedEntities[%s].id-Service with presented id: [%s] not found or is not available;".formatted(i, id));
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

    private void deleteOutdatedRelatedProducts(List<ProductRelatedEntityRequest> relatedEntities, List<ProductLinkToProduct> currentActiveRelatedProducts) {
        List<Long> relatedEntityIds =
                relatedEntities
                        .stream()
                        .map(ProductRelatedEntityRequest::getId)
                        .toList();

        List<ProductLinkToProduct> outdatedRelatedProducts =
                currentActiveRelatedProducts
                        .stream()
                        .filter(relatedProduct -> !relatedEntityIds.contains(relatedProduct.getLinkedProduct().getId()))
                        .toList();
        outdatedRelatedProducts
                .forEach(relatedProduct -> relatedProduct.setProductSubObjectStatus(ProductSubObjectStatus.DELETED));

        productLinkToProductRepository.saveAll(outdatedRelatedProducts);
    }

    private void deleteOutdatedRelatedServices(List<ProductRelatedEntityRequest> relatedEntities, List<ProductLinkToService> currentActiveRelatedServices) {
        List<Long> relatedEntityIds = relatedEntities
                .stream()
                .map(ProductRelatedEntityRequest::getId)
                .toList();

        List<ProductLinkToService> outdatedRelatedServices = currentActiveRelatedServices
                .stream()
                .filter(relatedServices -> !relatedEntityIds.contains(relatedServices.getLinkedService().getId()))
                .toList();
        outdatedRelatedServices
                .forEach(relatedServices -> relatedServices.setProductSubObjectStatus(ProductSubObjectStatus.DELETED));

        productLinkToServiceRepository.saveAll(outdatedRelatedServices);
    }


    /**
     * Validates whether the user can create a product contract with the product detail id and customer id
     * according to the product detail's related products and services.
     *
     * @param productId        ID of the product the detail of which should to be validated
     * @param productVersionId Version of the product the detail of which should to be validated
     * @param customerId       ID of the customer the contract should be created for
     * @param errorMessages    List of error messages to be filled in case of validation failure
     * @return {@code true} if the user can create a product contract with the product detail id and customer id
     */
    public boolean canCreateProductContractWithProductVersionAndCustomer(Long productId, Long productVersionId, Long customerId, List<String> errorMessages) {
        log.debug("Validating whether the user can create a product contract with the product detail id: {} and customer id: {}", productVersionId, customerId);

        Optional<ProductDetails> productDetailOptional = productDetailsRepository.findByProductIdAndVersion(productId, productVersionId);
        if (productDetailOptional.isEmpty()) {
            log.error("Unable to validate related products/services before contract creation as product detail with id: {} and version: {} not found", productId, productVersionId);
            errorMessages.add("Unable to validate related products/services before contract creation as product detail with id: %s and version: %s not found".formatted(productId, productVersionId));
            return false;
        }

        return productLinkToProductRepository.canCreateContractWithProductAndCustomer(productDetailOptional.get().getId(), customerId);
    }
}
