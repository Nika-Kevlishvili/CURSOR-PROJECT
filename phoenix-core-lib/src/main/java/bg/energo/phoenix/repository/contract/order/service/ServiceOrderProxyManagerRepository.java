package bg.energo.phoenix.repository.contract.order.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderProxyManager;
import bg.energo.phoenix.model.response.contract.order.service.proxy.ServiceOrderProxyManagerResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceOrderProxyManagerRepository extends JpaRepository<ServiceOrderProxyManager, Long> {

    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.order.service.proxy.ServiceOrderProxyManagerResponse(
                        sopm,
                        m
                    )
                    from ServiceOrderProxyManager sopm
                    join Manager m on sopm.customerManagerId = m.id
                        where sopm.orderProxyId = :orderProxyId
                        and sopm.status in :statuses
                    """
    )
    List<ServiceOrderProxyManagerResponse> getManagersByOrderProxyIdAndStatusIn(Long orderProxyId, List<EntityStatus> statuses);


    List<ServiceOrderProxyManager> findByOrderProxyIdAndStatusIn(Long orderProxyId, List<EntityStatus> statuses);

}
