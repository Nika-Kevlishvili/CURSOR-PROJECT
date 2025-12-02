package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.entity.contract.service.ContractLinkedProductContract;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractLinkedProductContractRepository extends JpaRepository<ContractLinkedProductContract,Long> {
    List<ContractLinkedProductContract> findAllByContractIdAndStatus(Long id, ContractSubObjectStatus status);
    List<ContractLinkedProductContract> findAllByContractIdAndStatusAndIdNotIn(Long id, ContractSubObjectStatus status,List<Long> ids);
    Optional<ContractLinkedProductContract> findByLinkedProductContractIdAndStatus(Long id,ContractSubObjectStatus status);
    Optional<ContractLinkedProductContract> findByLinkedProductContractIdAndContractIdAndStatus(Long id,Long contractId,ContractSubObjectStatus status);


}
