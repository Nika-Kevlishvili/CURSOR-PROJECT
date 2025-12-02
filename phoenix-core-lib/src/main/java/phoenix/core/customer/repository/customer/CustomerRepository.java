package phoenix.core.customer.repository.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import phoenix.core.customer.model.entity.customer.Customer;
import phoenix.core.customer.model.enums.customer.CustomerStatus;
import phoenix.core.customer.model.request.CustomerListingResponse;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {


    Optional<Customer> findFirstByIdentifierAndStatusIn(@Param("identifier") String identifier,
                                                        @Param("status") List<CustomerStatus> status);

    @Query(
            "select new phoenix.core.customer.model.request.CustomerListingResponse(" +
                    "c.identifier, " +
                    "c.status, " +
                    "c.customerType , " +
                    "cd.name, " +
                    "cd.economicBranchCiId, " +
                    "ci.name, " +
                    "cd.populatedPlaceId, " +
                    "pp.name, " +
                    "cd.createDate ) " +
                    "from Customer c " +
                    "join CustomerDetails cd on cd.customerId = c.id " +
                    "left join EconomicBranchCI ci on cd.economicBranchCiId = ci.id " +
                    "left join PopulatedPlace pp on cd.populatedPlaceId = pp.id " +
                    "where 1=1 and " +
                    "(:prompt is null or " +
                    "  (:search_field = 'ALL' and c.identifier like %:prompt% or " +
                    "   lower(cd.name) like %:prompt% or " +
                 /*    "lower(cd.status) like %:prompt% or " +*/ //enum to string ?
                    /* "lower(c.customerType) like %:prompt%  or " +*/
                       "ci.name like %:prompt% or " +
                       "pp.name like %:prompt% " +
                       ")" +
                    ")"
    )
    Page<CustomerListingResponse> getCustomersList(@Param("prompt")String prompt,@Param("search_field")String searchField, Pageable pageable);
}
