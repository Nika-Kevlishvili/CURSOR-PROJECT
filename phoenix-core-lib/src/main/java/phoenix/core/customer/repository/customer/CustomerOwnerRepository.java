package phoenix.core.customer.repository.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import phoenix.core.customer.model.entity.customer.CustomerOwner;
import phoenix.core.customer.model.enums.customer.Status;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerOwnerRepository extends JpaRepository<CustomerOwner,Long> {


    @Query("select c from CustomerOwner c where c.status in :statuses and c.id = :id")
    Optional<CustomerOwner> findByIdAndStatuses(@Param("id") Long id,@Param("statuses") List<Status> statuses);

    @Query("select c from CustomerOwner c where c.status in :statuses and c.customer.id = :customerId order by c.createDate asc")
    List<CustomerOwner> findByCustomerIdAndStatuses(@Param("customerId") Long customerId,@Param("statuses") List<Status> statuses);
}
