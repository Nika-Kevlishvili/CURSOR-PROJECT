package bg.energo.phoenix.repository.contract.order.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderRelatedServiceOrder;
import bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceOrderRelatedServiceOrderRepository extends JpaRepository<ServiceOrderRelatedServiceOrder, Long> {

    @Query(
            value = """
                    select sorso from ServiceOrderRelatedServiceOrder sorso
                        where (sorso.serviceOrderId = :orderId or sorso.relatedServiceOrderId = :orderId)
                        and sorso.status in :statuses
                    """
    )
    List<ServiceOrderRelatedServiceOrder> findByOrderIdAndStatusIn(
            @Param("orderId") Long orderId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select sorso from ServiceOrderRelatedServiceOrder sorso
                        where (
                            (sorso.serviceOrderId = :serviceOrderId and sorso.relatedServiceOrderId = :relatedServiceOrderId)
                            or (sorso.serviceOrderId = :relatedServiceOrderId and sorso.relatedServiceOrderId = :serviceOrderId)
                        )
                        and sorso.status in :statuses
                    """
    )
    Optional<ServiceOrderRelatedServiceOrder> findByOrderIdAndRelatedOrderIdAndStatusIn(
            @Param("serviceOrderId") Long serviceOrderId,
            @Param("relatedServiceOrderId") Long relatedServiceOrderId,
            @Param("statuses") List<EntityStatus> statuses
    );


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.relatedEntities.RelatedEntityResponse(
                        sorso,
                        so.orderNumber,
                        'SERVICE_ORDER',
                        case when sorso.serviceOrderId = :serviceOrderId
                            then sorso.relatedServiceOrderId
                            else sorso.serviceOrderId
                            end
                    )
                    from ServiceOrderRelatedServiceOrder sorso
                    join ServiceOrder so on
                        case when sorso.serviceOrderId = :serviceOrderId
                            then sorso.relatedServiceOrderId
                            else sorso.serviceOrderId
                            end = so.id
                        where (sorso.serviceOrderId = :serviceOrderId or sorso.relatedServiceOrderId = :serviceOrderId)
                        and sorso.status in :statuses
                    """
    )
    List<RelatedEntityResponse> findByServiceOrderIdAndStatusIn(
            @Param("serviceOrderId") Long serviceOrderId,
            @Param("statuses") List<EntityStatus> statuses
    );
}
