package bg.energo.phoenix.repository.contract.order.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderUnrecognizedPod;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderSubObjectShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceOrderUnrecognizedPodRepository extends JpaRepository<ServiceOrderUnrecognizedPod, Long> {

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.order.service.ServiceOrderSubObjectShortResponse(
                        soup.id,
                        soup.podIdentifier
                    )
                    from ServiceOrderUnrecognizedPod soup
                        where soup.orderId = :id
                        and soup.status in :statuses
                        order by soup.createDate asc
                    """
    )
    List<ServiceOrderSubObjectShortResponse> findByServiceOrderIdAndStatusIn(
            @Param("id") Long id,
            @Param("statuses") List<EntityStatus> statuses
    );


    List<ServiceOrderUnrecognizedPod> findByOrderIdAndStatusIn(Long orderId, List<EntityStatus> statuses);

}
