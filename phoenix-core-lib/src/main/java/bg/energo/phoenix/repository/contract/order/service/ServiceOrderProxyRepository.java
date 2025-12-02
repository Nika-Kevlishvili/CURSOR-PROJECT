package bg.energo.phoenix.repository.contract.order.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderProxy;
import bg.energo.phoenix.model.response.contract.order.service.proxy.ServiceOrderProxyBaseResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceOrderProxyRepository extends JpaRepository<ServiceOrderProxy, Long> {

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.order.service.proxy.ServiceOrderProxyBaseResponse(sop)
                    from ServiceOrderProxy sop
                        where sop.orderId = :orderId
                        and sop.status in :statuses
                    """
    )
    List<ServiceOrderProxyBaseResponse> getByOrderIdAndStatusIn(Long orderId, List<EntityStatus> statuses);


    List<ServiceOrderProxy> findByOrderIdAndStatusIn(Long orderId, List<EntityStatus> statuses);

}
