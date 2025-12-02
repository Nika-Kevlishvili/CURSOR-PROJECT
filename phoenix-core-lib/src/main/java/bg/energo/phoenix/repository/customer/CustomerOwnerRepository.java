package bg.energo.phoenix.repository.customer;

import bg.energo.phoenix.model.entity.customer.CustomerOwner;
import bg.energo.phoenix.model.enums.customer.Status;
import bg.energo.phoenix.model.response.customer.owner.CustomerOwnerDetailResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerOwnerRepository extends JpaRepository<CustomerOwner,Long> {


    @Query("select c from CustomerOwner c where c.status in :statuses and c.id = :id")
    Optional<CustomerOwner> findByIdAndStatuses(@Param("id") Long id,@Param("statuses") List<Status> statuses);

    @Query("select c from CustomerOwner c where c.status in :statuses and c.customer.id = :customerId order by c.createDate asc")
    List<CustomerOwner> findByCustomerIdAndStatuses(@Param("customerId") Long customerId,@Param("statuses") List<Status> statuses);

    @Query(
        """
        select new bg.energo.phoenix.model.response.customer.owner.CustomerOwnerDetailResponse(
            co,
            cd,
            lf
        )
        from CustomerOwner as co
        left join CustomerDetails as cd on cd.id = co.ownerCustomer.lastCustomerDetailId
        left join LegalForm lf on lf.id = cd.legalFormId
            where co.customer.id = :customerId
            and co.status in :statuses
            order by co.createDate
        """
    )
    List<CustomerOwnerDetailResponse> getOwnersByCustomerId(
            @Param("customerId") Long customerId,
            @Param("statuses") List<Status> statuses
    );
}
