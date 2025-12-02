package bg.energo.phoenix.repository.customer;

import bg.energo.phoenix.model.entity.customer.RelatedCustomer;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.response.customer.relatedCustomer.RelatedCustomerBasicInfo;
import bg.energo.phoenix.model.response.customer.relatedCustomer.RelatedCustomerResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RelatedCustomerRepository extends JpaRepository<RelatedCustomer, Long> {

    @Query(
            """
            select new bg.energo.phoenix.model.response.customer.relatedCustomer.RelatedCustomerBasicInfo(
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
                   select rc
            	   from RelatedCustomer rc
            	   where rc.customerId = :customerId and rc.status = :status
            	   
            """
    )
    List<RelatedCustomer> getRelatedCustomersByCustomerIdAndStatus(
            @Param("customerId") Long customerId,
            @Param("status") Status status
    );

    @Query(
            """
            select rc.id from RelatedCustomer as rc
                where (rc.customerId = :customerId or rc.relatedCustomerId = :customerId)
                and rc.status = :status
            """
    )
    List<Long> findCustomerRelationsRecordIds(
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

    @Query(
            """
            select new bg.energo.phoenix.model.response.customer.relatedCustomer.RelatedCustomerResponse(
                rc.id,
                CONCAT(cd.name, ' ', case when (cd.middleName is null or cd.middleName = '') then '' else concat(cd.middleName, ' ') end, cd.lastName),
                cc.identifier,
                rc.customerId,
                rc.relatedCustomerId,
                rc.ciConnectionType.id,
                rc.ciConnectionType.name,
                rc.status
            ) from RelatedCustomer rc
            inner join Customer cc on cc.id = rc.relatedCustomerId
            inner join CustomerDetails cd on cd.id = cc.lastCustomerDetailId
                where rc.customerId = :customerId
                and rc.status in :statuses
            """
    )
    List<RelatedCustomerResponse> getRelatedCustomersByCustomerId(
            @Param("customerId") Long customerId,
            @Param("statuses") List<Status> statuses
    );

    @Query(
            """
            select new bg.energo.phoenix.model.response.customer.relatedCustomer.RelatedCustomerResponse(
                rc.id,
                CONCAT(cd.name, ' ', case when (cd.middleName is null or cd.middleName = '') then '' else concat(cd.middleName, ' ') end, cd.lastName),
                cc.identifier,
                rc.customerId,
                rc.relatedCustomerId,
                rc.ciConnectionType.id,
                rc.ciConnectionType.name,
                rc.status
            ) from RelatedCustomer rc
            inner join Customer cc on cc.id = rc.customerId
            inner join CustomerDetails cd on cd.id = cc.lastCustomerDetailId
                where rc.relatedCustomerId = :customerId
                and rc.status in :statuses
            """
    )
    List<RelatedCustomerResponse> getCustomersUserIsRelatedTo(
            @Param("customerId") Long customerId,
            @Param("statuses") List<Status> statuses
    );

    @Query(
            value = """
                select count(rc.id) > 0 from RelatedCustomer rc
                    where ((rc.customerId = :customerId and rc.relatedCustomerId = :relatedCustomerId)
                    or (rc.customerId = :relatedCustomerId and rc.relatedCustomerId = :customerId))
                    and rc.status in (:statuses)
            """
    )
    boolean existsRelation(Long customerId, Long relatedCustomerId, List<Status> statuses);

}
