package phoenix.core.customer.repository.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import phoenix.core.customer.model.entity.customer.RelatedCustomer;
import phoenix.core.customer.model.enums.customer.Status;
import phoenix.core.customer.model.response.customer.relatedCustomer.RelatedCustomerBasicInfo;

import java.util.List;
import java.util.Optional;

public interface RelatedCustomerRepository extends JpaRepository<RelatedCustomer, Long> {

    @Query(
            """
            select new phoenix.core.customer.model.response.customer.relatedCustomer.RelatedCustomerBasicInfo(
                       rc.id,
                       CONCAT(c.identifier, ' (', rc.ciConnectionType.name, ')')
                   )
            	   from RelatedCustomer rc
            	   join Customer c on c.id = rc.relatedCustomerId
            	   where rc.customerId = :customerId and rc.status = :status
            	   order by rc.createDate asc
            """
    )
    List<RelatedCustomerBasicInfo> getRelatedCustomersByCustomerId(
            @Param("customerId") Long customerId,
            @Param("status") Status status
    );

    @Query(
            """
            select rc.id from RelatedCustomer as rc
                where rc.customerId = :customerId
                and rc.status = :status
            """
    )
    List<Long> getRelatedCustomerRecordIdsByCustomerId(
            @Param("customerId") Long customerId,
            @Param("status") Status status
    );

    @Query(
            """
            select rc.relatedCustomerId from RelatedCustomer as rc
                where rc.customerId = :customerId
                and rc.status = :status
            """
    )
    List<Long> findRelatedCustomerIdsByCustomerIdAndStatus(
            @Param("customerId") Long customerId,
            @Param("status") Status status
    );

    @Query(
            """
            select rc from RelatedCustomer as rc
                where rc.id = :id
                and rc.status in :statuses
            """
    )
    Optional<RelatedCustomer> findRelatedCustomerByIdAndStatuses(
            @Param("id") Long id,
            @Param("statuses") List<Status> statuses
    );
}
