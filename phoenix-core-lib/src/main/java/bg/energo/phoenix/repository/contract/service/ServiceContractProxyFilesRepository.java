package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.entity.contract.service.ServiceContractProxyFiles;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ServiceContractProxyFilesRepository extends JpaRepository<ServiceContractProxyFiles,Long> {
    Optional<ServiceContractProxyFiles> findByIdAndStatus(Long id, ContractSubObjectStatus contractSubObjectStatus);

    List<ServiceContractProxyFiles> findServiceContractProxyFilesByContractProxyIdAndStatusIn(Long id,List<ContractSubObjectStatus> statuses);

    List<ServiceContractProxyFiles> findByContractProxyIdAndStatus(Long id, ContractSubObjectStatus contractSubObjectStatus);

    List<ServiceContractProxyFiles> findByContractProxyIdAndIdNotInAndStatus(Long id, Set<Long> shouldBeUpdated, ContractSubObjectStatus contractSubObjectStatus);
}
