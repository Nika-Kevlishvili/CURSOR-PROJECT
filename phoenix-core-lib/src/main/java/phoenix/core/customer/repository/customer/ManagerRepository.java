package phoenix.core.customer.repository.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import phoenix.core.customer.model.entity.customer.Manager;
import phoenix.core.customer.model.enums.customer.Status;
import phoenix.core.customer.model.response.customer.manager.ManagerBasicInfo;

import java.util.List;
import java.util.Optional;

public interface ManagerRepository extends JpaRepository<Manager, Long> {

    @Query(
            "select new phoenix.core.customer.model.response.customer.manager.ManagerBasicInfo(m) " +
                    " from Manager as m" +
                    " where m.customerDetailId = :customerDetailId " +
                    " and m.status = :status order by m.name "
    )
    List<ManagerBasicInfo> getManagersByCustomerDetailId(
            @Param("customerDetailId") Long customerDetailId,
            @Param("status")Status status
    );

    @Query(
            "select m.id from Manager as m " +
                    "where m.customerDetailId = :customerDetailId " +
                    "and m.status = :status"
    )
    List<Long> getManagerIdsByCustomerDetailId(
            @Param("customerDetailId") Long customerDetailId,
            @Param("status")Status status
    );

    @Query(
            """
            select m from Manager as m
                where m.id = :id
                and m.status in :statuses
            """
    )
    Optional<Manager> findManagerByIdAndStatuses(
            @Param("id") Long id,
            @Param("statuses") List<Status> statuses
    );

}
