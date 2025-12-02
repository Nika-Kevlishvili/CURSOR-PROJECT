package bg.energo.phoenix.repository.contract.product;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.product.ProductContractRelatedServiceContract;
import bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductContractRelatedServiceContractRepository extends JpaRepository<ProductContractRelatedServiceContract, Long> {

    @Query(
            value = """
                    select pcrsc from ProductContractRelatedServiceContract pcrsc
                        where (
                            (:type = 'PRODUCT_CONTRACT' and pcrsc.productContractId = :objectId)
                            or (:type = 'SERVICE_CONTRACT' and pcrsc.serviceContractId = :objectId)
                        )
                        and pcrsc.status in :statuses
                    """
    )
    List<ProductContractRelatedServiceContract> findByProductContractIdOrServiceContractIdAndStatusIn(
            @Param("type") String type,
            @Param("objectId") Long objectId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select pcrsc from ProductContractRelatedServiceContract pcrsc
                        where pcrsc.productContractId = :productContractId
                        and pcrsc.serviceContractId = :serviceContractId
                        and pcrsc.status in :statuses
                    """
    )
    Optional<ProductContractRelatedServiceContract> findByProductContractIdAndServiceContractIdAndStatusIn(
            @Param("productContractId") Long productContractId,
            @Param("serviceContractId") Long serviceContractId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse(
                        pcrsc,
                        sc.contractNumber,
                        'SERVICE_CONTRACT',
                        sc.id
                    )
                    from ProductContractRelatedServiceContract pcrsc
                    join ServiceContracts sc on sc.id = pcrsc.serviceContractId
                        where pcrsc.productContractId = :productContractId
                        and pcrsc.status in :statuses
                    """
    )
    List<RelatedEntityResponse> findByProductContractIdAndStatusIn(
            @Param("productContractId") Long productContractId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse(
                        pcrsc,
                        pc.contractNumber,
                        'PRODUCT_CONTRACT',
                        pc.id
                    )
                    from ProductContractRelatedServiceContract pcrsc
                    join ProductContract pc on pc.id = pcrsc.productContractId
                        where pcrsc.serviceContractId = :serviceContractId
                        and pcrsc.status in :statuses
                    """
    )
    List<RelatedEntityResponse> findByServiceContractIdAndStatusIn(
            @Param("serviceContractId") Long serviceContractId,
            @Param("statuses") List<EntityStatus> statuses
    );

}
