package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.entity.contract.service.ServiceContractPriceComponents;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceContractPriceComponentsRepository extends JpaRepository<ServiceContractPriceComponents, Long> {
    List<ServiceContractPriceComponents> findByContractDetailIdAndStatusIn(Long contractDetailId, List<ContractSubObjectStatus> statuses);
}
