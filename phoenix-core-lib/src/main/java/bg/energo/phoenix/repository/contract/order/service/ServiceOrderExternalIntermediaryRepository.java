package bg.energo.phoenix.repository.contract.order.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderExternalIntermediary;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderSubObjectShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceOrderExternalIntermediaryRepository extends JpaRepository<ServiceOrderExternalIntermediary, Long> {

    List<ServiceOrderExternalIntermediary> findByOrderIdAndStatusIn(Long orderId, List<EntityStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.order.service.ServiceOrderSubObjectShortResponse(
                        soei.externalIntermediaryId,
                        concat(ei.name, ' (', ei.identifier, ')')
                    )
                    from ServiceOrderExternalIntermediary soei
                    join ExternalIntermediary ei on soei.externalIntermediaryId = ei.id
                        where soei.orderId = :orderId
                        and soei.status in :statuses
                        order by soei.createDate asc
                    """
    )
    List<ServiceOrderSubObjectShortResponse> getShortResponseByOrderIdAndStatusIn(
            Long orderId,
            List<EntityStatus> statuses
    );

}
