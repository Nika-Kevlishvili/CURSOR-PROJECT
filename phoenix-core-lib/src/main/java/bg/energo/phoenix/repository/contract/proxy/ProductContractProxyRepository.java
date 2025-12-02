package bg.energo.phoenix.repository.contract.proxy;

import bg.energo.phoenix.model.entity.contract.proxy.ProductContractProxy;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductContractProxyRepository extends JpaRepository<ProductContractProxy, Long> {

    Optional<ProductContractProxy> findByIdAndStatus(Long proxyId, ContractSubObjectStatus status);

    List<ProductContractProxy> findByContractDetailIdAndStatusIn(Long contractId, List<ContractSubObjectStatus> status);

    List<ProductContractProxy> getProxiesByContractDetailIdAndStatus(Long id, ContractSubObjectStatus status);

}
