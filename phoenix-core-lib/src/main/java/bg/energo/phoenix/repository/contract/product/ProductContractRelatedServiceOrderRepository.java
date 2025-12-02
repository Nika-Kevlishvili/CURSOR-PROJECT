package bg.energo.phoenix.repository.contract.product;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.product.ProductContractRelatedServiceOrder;
import bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductContractRelatedServiceOrderRepository extends JpaRepository<ProductContractRelatedServiceOrder, Long> {

    @Query(
            value = """
                    select pcrso from ProductContractRelatedServiceOrder pcrso
                        where (
                            (:type = 'PRODUCT_CONTRACT' and pcrso.productContractId = :objectId)
                            or (:type = 'SERVICE_ORDER' and pcrso.serviceOrderId = :objectId)
                        )
                        and pcrso.status in :statuses
                    """
    )
    List<ProductContractRelatedServiceOrder> findByProductContractIdOrServiceOrderIdAndStatusIn(
            @Param("type") String type,
            @Param("objectId") Long objectId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select pcrso from ProductContractRelatedServiceOrder pcrso
                        where pcrso.productContractId = :productContractId
                        and pcrso.serviceOrderId = :serviceOrderId
                        and pcrso.status in :statuses
                    """
    )
    Optional<ProductContractRelatedServiceOrder> findByProductContractIdAndServiceOrderIdAndStatusIn(
            @Param("productContractId") Long productContractId,
            @Param("serviceOrderId") Long serviceOrderId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse(
                        pcrso,
                        so.orderNumber,
                        'SERVICE_ORDER',
                        so.id
                    )
                    from ProductContractRelatedServiceOrder pcrso
                    join ServiceOrder so on so.id = pcrso.serviceOrderId
                        where pcrso.productContractId = :productContractId
                        and pcrso.status in :statuses
                    """
    )
    List<RelatedEntityResponse> findByProductContractIdAndStatusIn(
            @Param("productContractId") Long productContractId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse(
                        pcrso,
                        pc.contractNumber,
                        'PRODUCT_CONTRACT',
                        pc.id
                    )
                    from ProductContractRelatedServiceOrder pcrso
                    join ProductContract pc on pc.id = pcrso.productContractId
                        where pcrso.serviceOrderId = :serviceOrderId
                        and pcrso.status in :statuses
                    """
    )
    List<RelatedEntityResponse> findByServiceOrderIdAndStatusIn(
            @Param("serviceOrderId") Long serviceOrderId,
            @Param("statuses") List<EntityStatus> statuses
    );

}
