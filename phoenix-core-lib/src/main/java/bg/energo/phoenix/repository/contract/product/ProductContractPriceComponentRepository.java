package bg.energo.phoenix.repository.contract.product;

import bg.energo.phoenix.model.entity.contract.product.ProductContractPriceComponents;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductContractPriceComponentRepository extends JpaRepository<ProductContractPriceComponents,Long> {

    List<ProductContractPriceComponents> findByContractDetailIdAndStatusIn(Long contractDetailId, List<ContractSubObjectStatus> statuses);
}
