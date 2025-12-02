package bg.energo.phoenix.repository.contract.order.service;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderAssistingEmployee;
import bg.energo.phoenix.model.response.contract.order.service.ServiceOrderSubObjectShortResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceOrderAssistingEmployeeRepository extends JpaRepository<ServiceOrderAssistingEmployee, Long> {

    List<ServiceOrderAssistingEmployee> findByOrderIdAndStatusIn(Long orderId, List<EntityStatus> statuses);


    @Query(
            value = """
                    select new bg.energo.phoenix.model.response.contract.order.service.ServiceOrderSubObjectShortResponse(
                        soae.accountManagerId,
                        concat(ac.displayName, ' (', ac.userName, ')')
                    )
                    from ServiceOrderAssistingEmployee soae
                    join AccountManager ac on soae.accountManagerId = ac.id
                        where soae.orderId = :orderId
                        and soae.status in :statuses
                        order by soae.createDate asc
                    """
    )
    List<ServiceOrderSubObjectShortResponse> getShortResponseByOrderIdAndStatusIn(
            @Param("orderId") Long orderId,
            @Param("statuses") List<EntityStatus> statuses
    );

}
