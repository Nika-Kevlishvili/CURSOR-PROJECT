package bg.energo.phoenix.service.contract.product;

import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.entity.contract.billing.ContractPods;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.pod.pod.PointOfDeliveryDetails;
import bg.energo.phoenix.repository.contract.billing.ContractPodRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractDetailsRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryDetailsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static bg.energo.phoenix.model.entity.EntityStatus.ACTIVE;
import static bg.energo.phoenix.model.entity.EntityStatus.DELETED;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductContractPodContinuityValidationService {
    private final ContractPodRepository contractPodRepository;
    private final ProductContractDetailsRepository productContractDetailsRepository;
    private final PointOfDeliveryDetailsRepository pointOfDeliveryDetailsRepository;
    private final PointOfDeliveryDetailsRepository podDetailsRepository;


    public ProductContractDetails findNextVersion(Long productContractId,
                                                  LocalDate versionDate
    ) {
        ProductContractDetails nextVersion = null;
        Optional<ProductContractDetails> nextVersionOptional = productContractDetailsRepository
                .findSignedProductContractNextVersion(
                        productContractId,
                        versionDate,
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst();
        if (nextVersionOptional.isPresent()) {
            nextVersion = nextVersionOptional.get();
        }
        return nextVersion;
    }

    public ProductContractDetails findPreviousVersion(Long productContractId,
                                                      LocalDate versionDate
    ) {
        ProductContractDetails previousVersion = null;
        Optional<ProductContractDetails> previousVersionOptional = productContractDetailsRepository
                .findSignedProductContractPreviousVersion(
                        productContractId,
                        versionDate,
                        PageRequest.of(0, 1))
                .stream()
                .findFirst();
        if (previousVersionOptional.isPresent()) {
            previousVersion = previousVersionOptional.get();
        }
        return previousVersion;
    }

    public void validatePodContinuity(Long contractId,
                                      Long beforeProductDetailId,
                                      Long middleProductDetailId,
                                      Long afterProductDetailId,
                                      LocalDate versionDate) {

        List<ContractPods> previousContractPods = fetchContractPods(beforeProductDetailId);
        List<ContractPods> middleContractPods = fetchContractPods(middleProductDetailId);
        List<ContractPods> nextContractPods = fetchContractPods(afterProductDetailId);

        Set<Long> uniquePreviousPodIds = extractUniquePodIds(previousContractPods);
        Set<Long> uniqueMiddlePodIds = extractUniquePodIds(middleContractPods);
        Set<Long> uniqueNextPodIds = extractUniquePodIds(nextContractPods);


        Map<Long, List<Long>> podIdToPodDetailIdsMap = createPodIdToDetailIdsMap(middleContractPods);

        List<Long> podsToRemove = findPodsToRemove(
                uniquePreviousPodIds,
                uniqueMiddlePodIds,
                uniqueNextPodIds);

        List<Long> podsToAdd = findPodsToAdd(
                uniquePreviousPodIds,
                uniqueMiddlePodIds,
                uniqueNextPodIds);


        List<ProductContractDetails> allFutureVersions = productContractDetailsRepository
                .findAllSignedProductContractNextVersion(
                        contractId,
                        versionDate
                );

        ProductContractDetails middleDetail = productContractDetailsRepository.findById(middleProductDetailId).get();


        processFutureVersions(
                allFutureVersions,
                podIdToPodDetailIdsMap,
                podsToRemove,
                podsToAdd,
                middleContractPods
        );

        processPreviousVersionDatesAndReason(
                previousContractPods,
                allFutureVersions,
                podIdToPodDetailIdsMap,
                podsToRemove,
                podsToAdd,
                versionDate
        );

    }

    private List<ContractPods> fetchContractPods(Long productDetailId) {
        return contractPodRepository.findAllByContractDetailIdAndStatusIn(
                productDetailId,
                List.of(ACTIVE));
    }

    private Set<Long> extractUniquePodIds(List<ContractPods> contractPods) {
        List<PointOfDeliveryDetails> podDetails = new ArrayList<>();
        for (ContractPods contractPod : contractPods) {
            podDetails.add(pointOfDeliveryDetailsRepository.findById(contractPod.getPodDetailId()).get());
        }
        return podDetails.stream().map(PointOfDeliveryDetails::getPodId)
                .collect(Collectors.toSet());
    }

    private Map<Long, List<Long>> createPodIdToDetailIdsMap(List<ContractPods> contractPods) {
        Map<Long, List<Long>> result = new HashMap<>();
        List<PointOfDeliveryDetails> podDetailsList = new ArrayList<>();

        for (ContractPods contractPod : contractPods) {
            Optional<PointOfDeliveryDetails> optionalPodDetails = pointOfDeliveryDetailsRepository
                    .findById(contractPod.getPodDetailId());
            optionalPodDetails.ifPresent(podDetailsList::add);
        }

        for (PointOfDeliveryDetails podDetails : podDetailsList) {
            if (result.containsKey(podDetails.getPodId())) {
                result.get(podDetails.getPodId()).add(podDetails.getId());
            } else {
                result.put(podDetails.getPodId(), new ArrayList<>(Collections.singleton(podDetails.getId())));
            }
        }
//
//        return podDetailsList.stream()
//                .collect(Collectors.groupingBy(
//                        PointOfDeliveryDetails::getPodId,
//                        Collectors.mapping(PointOfDeliveryDetails::getId, Collectors.toList())
//                ));
        return result;
    }

    private List<Long> findPodsToRemove(Set<Long> uniquePreviousPodIds,
                                        Set<Long> uniqueMiddlePodIds,
                                        Set<Long> uniqueNextPodIds) {

        return uniquePreviousPodIds.stream()
                .filter(podId ->
                        !uniqueMiddlePodIds.contains(podId) && uniqueNextPodIds.contains(podId))
                .toList();
    }

    private List<Long> findPodsToAdd(Set<Long> uniquePreviousPodIds,
                                     Set<Long> uniqueMiddlePodIds,
                                     Set<Long> uniqueNextPodIds) {

        return uniqueMiddlePodIds.stream()
                .filter(podId ->
                        !uniquePreviousPodIds.contains(podId) && !uniqueNextPodIds.contains(podId))
                .toList();
    }

    private void processPreviousVersionDatesAndReason(List<ContractPods> previousContractPods,
                                                      List<ProductContractDetails> futureVersions,
                                                      Map<Long, List<Long>> podIdToPodDetailIdsMap,
                                                      List<Long> podsToRemove,
                                                      List<Long> podsToAdd,
                                                      LocalDate middleVersionDate) {
        handlePodsChangeDatesAndReason(
                podsToRemove,
                previousContractPods,
                middleVersionDate
        );
    }

    //for pods that are removing from future
    //change date and reason in previous version
    private void handlePodsChangeDatesAndReason(
            List<Long> podsToRemove,
            List<ContractPods> previousPods,
            LocalDate middleVersionDate
    ) {

        previousPods.forEach(contractPod -> {
            PointOfDeliveryDetails pointOfDeliveryDetails = pointOfDeliveryDetailsRepository
                    .findById(contractPod.getPodDetailId())
                    .orElse(null);

            if (pointOfDeliveryDetails != null && podsToRemove.contains(pointOfDeliveryDetails.getPodId())) {
                if (contractPod.getDeactivationDate() != null
                        && !middleVersionDate.isAfter(contractPod.getDeactivationDate())) {
                    contractPod.setDeactivationDate(middleVersionDate.minusDays(1));
                    contractPod.setDeactivationPurposeId(70L);

                }
                contractPodRepository.save(contractPod);
            }
        });
    }

    private void processFutureVersions(List<ProductContractDetails> futureVersions,
                                       Map<Long, List<Long>> podIdToPodDetailIdsMap,
                                       List<Long> podsToRemove,
                                       List<Long> podsToAdd,
                                       List<ContractPods> middleContractPods) {

        futureVersions.forEach(futureVersion -> {
            List<ContractPods> contractPods = contractPodRepository.findAllByContractDetailIdAndStatusIn(
                    futureVersion.getId(),
                    List.of(ACTIVE)
            );

            handlePodsToAdd(podsToAdd, podIdToPodDetailIdsMap, futureVersion, middleContractPods,contractPods);
            handlePodsToRemove(podsToRemove, contractPods, futureVersion);

            List<ContractPods> contractPodsNew = contractPodRepository.findAllByContractDetailIdAndStatusIn(
                    futureVersion.getId(),
                    List.of(ACTIVE)
            );

            processEstimatedTotalConsumption(
                    contractPodsNew.stream().map(
                            ContractPods::getPodDetailId).toList(),
                    productContractDetailsRepository.findById(futureVersion.getId()).get());
        });
    }

    private void handlePodsToAdd(List<Long> podsToAdd,
                                 Map<Long, List<Long>> podIdToPodDetailIdsMap,
                                 ProductContractDetails futureVersion,
                                 List<ContractPods> middleContractPods,
                                 List<ContractPods> futureContractPods) {
        podsToAdd.forEach(podId -> {
            if (podIdToPodDetailIdsMap.containsKey(podId)) {
                List<Long> podDetailIds = podIdToPodDetailIdsMap.get(podId);
                if (!podDetailIds.isEmpty()) {
                    Long firstPodDetailId = podDetailIds.get(0);
                    if (futureContractPods.stream().noneMatch(futurePod ->
                            futurePod.getPodDetailId().equals(firstPodDetailId))) {
                        ContractPods newContractPod = new ContractPods();
                        newContractPod.setBillingGroupId(middleContractPods
                                .stream()
                                .filter(contractPod -> contractPod.getPodDetailId().equals(firstPodDetailId))
                                .findFirst()
                                .get()
                                .getBillingGroupId()
                        );
                        newContractPod.setContractDetailId(futureVersion.getId());
                        newContractPod.setPodDetailId(firstPodDetailId);
                        newContractPod.setStatus(ACTIVE);

                        contractPodRepository.saveAndFlush(newContractPod);
                        log.info("Added new Contract Pod for Pod ID {} in Future Version ID {}",
                                podId,
                                futureVersion.getContractId());
                    }
                }
            }
        });
    }

    private void handlePodsToRemove(
            List<Long> podsToRemove,
            List<ContractPods> existingPods,
            ProductContractDetails futureVersion) {

        boolean isOnlyOneMzfq = existingPods.size() == 1;

        existingPods.forEach(contractPod -> {
            PointOfDeliveryDetails pointOfDeliveryDetails = pointOfDeliveryDetailsRepository
                    .findById(contractPod.getPodDetailId())
                    .orElse(null);

            if (pointOfDeliveryDetails != null && podsToRemove.contains(pointOfDeliveryDetails.getPodId())) {
                if (isOnlyOneMzfq) {
                    throw new IllegalArgumentsProvidedException("All contract version should have at least one pod");
                }
                contractPod.setStatus(DELETED);
                contractPodRepository.save(contractPod);

                log.info("Updated Contract Pod ID {} to DELETED for Future Version ID {}",
                        contractPod.getId(),
                        futureVersion.getContractId());
            }
        });
    }

    private void processEstimatedTotalConsumption(List<Long> podDetailIds,
                                                  ProductContractDetails productContractDetails) {
        BigDecimal summedConsumption = BigDecimal.ZERO;
        if (CollectionUtils.isNotEmpty(podDetailIds)) {
            List<Integer> podConsumptions = podDetailsRepository.findEstimatedMonthlyAvgConsumptionByIdIn(podDetailIds);
            int totalConsumption = podConsumptions.stream().mapToInt(Integer::intValue).sum();
            summedConsumption = BigDecimal.valueOf(totalConsumption * 12L).divide(BigDecimal.valueOf(1000), MathContext.UNLIMITED);
        }
        productContractDetails.setEstimatedTotalConsumptionUnderContractKwh(summedConsumption);
        productContractDetailsRepository.saveAndFlush(productContractDetails);
    }

}
