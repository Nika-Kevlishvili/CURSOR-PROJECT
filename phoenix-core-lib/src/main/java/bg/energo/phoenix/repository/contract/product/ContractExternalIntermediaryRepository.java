package bg.energo.phoenix.repository.contract.product;

import bg.energo.phoenix.model.entity.contract.product.ContractExternalIntermediary;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import bg.energo.phoenix.model.response.contract.productContract.ProductContractSubObjectShortResponse;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractExternalIntermediaryRepository extends JpaRepository<ContractExternalIntermediary, Long> {

    List<ContractExternalIntermediary> findByContractDetailIdAndStatusIn(Long contractDetailId, List<ContractSubObjectStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.productContract.ProductContractSubObjectShortResponse(
                        cei.externalIntermediaryId,
                        concat(ei.name, ' (', ei.identifier, ')')
                    )
                    from ContractExternalIntermediary cei
                    join ExternalIntermediary ei on cei.externalIntermediaryId = ei.id
                        where cei.contractDetailId = :contractDetailId
                        and cei.status in :statuses
                    """
    )
    List<ProductContractSubObjectShortResponse> getShortResponseByContractDetailIdAndStatusIn(
            @Param("contractDetailId") Long contractDetailId,
            @Param("statuses") List<ContractSubObjectStatus> statuses
    );

}
