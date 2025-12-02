package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.entity.contract.service.ContractLinkedServiceContract;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractLinkedServiceContractRepository extends JpaRepository<ContractLinkedServiceContract, Long> {
    List<ContractLinkedServiceContract> findAllByContractIdAndStatus(Long id, ContractSubObjectStatus status);

    Optional<ContractLinkedServiceContract> findByLinkedServiceContractIdAndStatus(Long id, ContractSubObjectStatus contractSubObjectStatus);
    Optional<ContractLinkedServiceContract> findByLinkedServiceContractIdAndContractIdAndStatus(Long id, Long contractId,ContractSubObjectStatus contractSubObjectStatus);

    List<ContractLinkedServiceContract> findByContractIdAndStatusAndIdNotIn(Long id, ContractSubObjectStatus contractSubObjectStatus, List<Long> list);
}
