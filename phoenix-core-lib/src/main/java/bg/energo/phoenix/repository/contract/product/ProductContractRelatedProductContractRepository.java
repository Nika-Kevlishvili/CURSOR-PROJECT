package bg.energo.phoenix.repository.contract.product;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.product.ProductContractRelatedProductContract;
import bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductContractRelatedProductContractRepository extends JpaRepository<ProductContractRelatedProductContract,Long> {


    @Query(
            value = """
                    select pcrc from ProductContractRelatedProductContract pcrc
                        where (
                            (pcrc.productContractId = :contractId and pcrc.relatedProductContractId = :relatedContractId)
                            or (pcrc.productContractId = :relatedContractId and pcrc.relatedProductContractId = :contractId)
                        )
                        and pcrc.status in :statuses
                    """
    )
    Optional<ProductContractRelatedProductContract> findByContractIdAndRelatedContractIdAndStatusIn(
            @Param("contractId") Long contractId,
            @Param("relatedContractId") Long relatedContractId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select pcrc from ProductContractRelatedProductContract pcrc
                        where (pcrc.productContractId = :contractId or pcrc.relatedProductContractId = :contractId)
                        and pcrc.status in :statuses
                    """
    )
    List<ProductContractRelatedProductContract> findByContractIdAndStatusIn(
            @Param("contractId") Long contractId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse(
                        pcrpc,
                        pc.contractNumber,
                        'PRODUCT_CONTRACT',
                        case when pcrpc.productContractId = :contractId
                            then pcrpc.relatedProductContractId
                            else pcrpc.productContractId
                            end
                    )
                    from ProductContractRelatedProductContract pcrpc
                    join ProductContract pc on
                        case when pcrpc.productContractId = :contractId
                            then pcrpc.relatedProductContractId
                            else pcrpc.productContractId
                            end = pc.id
                        where (pcrpc.productContractId = :contractId or pcrpc.relatedProductContractId = :contractId)
                        and pcrpc.status in :statuses
                    """
    )
    List<RelatedEntityResponse> findByProductContractIdAndStatusIn(
            @Param("contractId") Long contractId,
            @Param("statuses") List<EntityStatus> statuses
    );

}
