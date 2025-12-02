package bg.energo.phoenix.repository.contract.proxy;

import bg.energo.phoenix.model.entity.contract.proxy.ProductContractProxyFile;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ProxyFilesRepository extends JpaRepository<ProductContractProxyFile, Long> {
    Optional<ProductContractProxyFile> findByIdAndStatus(Long id, ContractSubObjectStatus status);
    List<ProductContractProxyFile> findByContractProxyIdAndIdNotInAndStatus(Long id, Set<Long> ids, ContractSubObjectStatus status);
    Optional<ProductContractProxyFile> findByIdAndStatusAndContractProxyIdIsNull(Long id, ContractSubObjectStatus status);
    List<ProductContractProxyFile> findByContractProxyIdAndStatus(Long id, ContractSubObjectStatus status);

}
