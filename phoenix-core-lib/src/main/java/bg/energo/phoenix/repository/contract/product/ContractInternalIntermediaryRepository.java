package bg.energo.phoenix.repository.contract.product;

import bg.energo.phoenix.model.entity.contract.product.ContractInternalIntermediary;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractSubObjectShortResponse;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractInternalIntermediaryRepository extends JpaRepository<ContractInternalIntermediary, Long> {

    List<ContractInternalIntermediary> findByContractDetailIdAndStatusIn(Long contractDetailId, List<ContractSubObjectStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.productContract.ProductContractSubObjectShortResponse(
                        cii.accountManagerId,
                        concat(ac.displayName, ' (', ac.userName, ')')
                    )
                    from ContractInternalIntermediary cii
                    join AccountManager ac on cii.accountManagerId = ac.id
                        where cii.contractDetailId = :contractDetailId
                        and cii.status in :statuses
                    """
    )
    List<ProductContractSubObjectShortResponse> getShortResponseByContractDetailIdAndStatusIn(
            @Param("contractDetailId") Long contractDetailId,
            @Param("statuses") List<ContractSubObjectStatus> statuses
    );

}
