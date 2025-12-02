package bg.energo.phoenix.repository.contract.product;

import bg.energo.phoenix.model.entity.contract.product.ProductContractAdditionalParams;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductContractAdditionalParamsRepository extends JpaRepository<ProductContractAdditionalParams, Long> {

    List<ProductContractAdditionalParams> findAllByContractDetailId(Long contractDetailId);

    void deleteAllByContractDetailId(Long contractDetailId);

    Optional<ProductContractAdditionalParams> findByContractDetailIdAndProductAdditionalParamId(Long contractDetailId, Long productAdditionalParamId);
}
