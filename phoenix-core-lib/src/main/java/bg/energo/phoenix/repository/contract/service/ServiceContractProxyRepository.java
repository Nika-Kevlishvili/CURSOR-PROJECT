package bg.energo.phoenix.repository.contract.service;

import bg.energo.phoenix.model.entity.contract.service.ServiceContractProxy;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceContractProxyRepository extends JpaRepository<ServiceContractProxy, Long> {
    List<ServiceContractProxy> findByContractDetailIdAndStatusIn(Long id, List<ContractSubObjectStatus> statuses);

    Optional<ServiceContractProxy> findByIdAndStatus(Long proxyId, ContractSubObjectStatus contractSubObjectStatus);

    List<ServiceContractProxy> getProxiesByContractDetailIdAndStatus(Long contractDetailId, ContractSubObjectStatus contractSubObjectStatus);
}
