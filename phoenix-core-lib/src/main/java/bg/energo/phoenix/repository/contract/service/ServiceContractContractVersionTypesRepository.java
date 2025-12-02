package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.entity.contract.service.ServiceContractContractVersionTypes;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceContractContractVersionTypesRepository extends JpaRepository<ServiceContractContractVersionTypes,Long> {
    List<ServiceContractContractVersionTypes> findByContractDetailIdAndStatusIn(Long id, List<ContractSubObjectStatus> statuses);
}
