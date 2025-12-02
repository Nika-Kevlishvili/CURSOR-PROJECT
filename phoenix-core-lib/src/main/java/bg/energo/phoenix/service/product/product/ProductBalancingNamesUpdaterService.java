package bg.energo.phoenix.service.product.product;

import bg.energo.phoenix.model.customAnotations.aspects.ExecutionTimeLogger;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.product.price.priceComponent.ProfileForBalancing;
import bg.energo.phoenix.model.entity.product.product.ProductForBalancing;
import bg.energo.phoenix.model.response.communication.xEnergie.BalancingSystemsProducts;
import bg.energo.phoenix.model.response.communication.xEnergie.BalancingSystemsProfiles;
import bg.energo.phoenix.repository.product.price.priceComponent.ProfileForBalancingRepository;
import bg.energo.phoenix.repository.product.product.ProductForBalancingRepository;
import bg.energo.phoenix.service.xEnergie.XEnergieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductBalancingNamesUpdaterService {
    private final ProductForBalancingRepository productForBalancingRepository;
    private final ProfileForBalancingRepository profileForBalancingRepository;
    private final XEnergieRepository xEnergieRepository;

    @ExecutionTimeLogger
    public void updateBalancingProducts() {
        try {
            List<ProductForBalancing> activeBalancingProducts = productForBalancingRepository
                    .findAllByStatusIn(List.of(EntityStatus.ACTIVE));

            List<BalancingSystemsProducts> balancingSystemsProducts = xEnergieRepository
                    .retrieveBalancingSystemProducts();

            List<ProductForBalancing> outDatedProductForBalancing = activeBalancingProducts
                    .stream()
                    .filter(productForBalancing ->
                            CollectionUtils.select(balancingSystemsProducts,
                                            x -> x.name().equals(productForBalancing.getName())
                                    )
                                    .isEmpty())
                    .toList();

            List<BalancingSystemsProducts> newProductsForBalancing = balancingSystemsProducts
                    .stream()
                    .filter(informationForBalancingSystem ->
                            CollectionUtils.select(activeBalancingProducts,
                                            x -> x.getName().equals(informationForBalancingSystem.name())
                                    )
                                    .isEmpty()
                    )
                    .toList();

            outDatedProductForBalancing.forEach(productForBalancing -> productForBalancing.setStatus(EntityStatus.DELETED));

            productForBalancingRepository.saveAll(outDatedProductForBalancing);
            productForBalancingRepository.saveAll(newProductsForBalancing.stream().map(ProductForBalancing::new).toList());
        } catch (Exception e) {
            log.error("Product Balancing Name update failed", e);
        }
    }

    @ExecutionTimeLogger
    public void updateBalancingProfiles() {
        try {
            List<ProfileForBalancing> activeBalancingProfiles = profileForBalancingRepository
                    .findAllByStatusIn(List.of(EntityStatus.ACTIVE));

            List<BalancingSystemsProfiles> balancingSystemsProfiles = xEnergieRepository
                    .retrieveBalancingSystemProfiles();

            List<ProfileForBalancing> outDatedProfilesForBalancing = activeBalancingProfiles
                    .stream()
                    .filter(productForBalancing ->
                            CollectionUtils.select(balancingSystemsProfiles,
                                            x -> x.name().equals(productForBalancing.getName())
                                    )
                                    .isEmpty())
                    .toList();

            List<BalancingSystemsProfiles> newProfilesForBalancing = balancingSystemsProfiles
                    .stream()
                    .filter(informationForBalancingSystem ->
                            CollectionUtils.select(activeBalancingProfiles,
                                            x -> x.getName().equals(informationForBalancingSystem.name())
                                    )
                                    .isEmpty()
                    )
                    .toList();

            outDatedProfilesForBalancing.forEach(productForBalancing -> productForBalancing.setStatus(EntityStatus.DELETED));

            profileForBalancingRepository.saveAll(outDatedProfilesForBalancing);
            profileForBalancingRepository.saveAll(newProfilesForBalancing.stream().map(ProfileForBalancing::new).toList());
        } catch (Exception e) {
            log.error("Profile Balancing Name update failed", e);
        }
    }
}
