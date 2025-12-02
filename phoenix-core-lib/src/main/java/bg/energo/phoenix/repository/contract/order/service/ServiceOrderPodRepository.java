package bg.energo.phoenix.repository.contract.order.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderPod;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderSubObjectShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceOrderPodRepository extends JpaRepository<ServiceOrderPod, Long> {

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.order.service.ServiceOrderSubObjectShortResponse(
                        pod.id,
                        pod.identifier
                    )
                    from ServiceOrderPod sop
                    join PointOfDelivery pod on sop.podId = pod.id
                        where sop.orderId = :id
                        and sop.status in :statuses
                        order by sop.createDate asc
                    """
    )
    List<ServiceOrderSubObjectShortResponse> findByServiceOrderIdAndStatusIn(
            @Param("id") Long id,
            @Param("statuses") List<EntityStatus> statuses
    );


    List<ServiceOrderPod> findByOrderIdAndStatusIn(Long orderId, List<EntityStatus> statuses);

}
