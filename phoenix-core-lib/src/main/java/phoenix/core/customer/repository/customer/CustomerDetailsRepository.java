package phoenix.core.customer.repository.customer;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import phoenix.core.customer.model.entity.customer.CustomerDetails;
import phoenix.core.customer.model.enums.customer.CustomerDetailStatus;
import phoenix.core.customer.model.request.CustomerVersionsResponse;

import java.util.List;
import java.util.Optional;

public interface CustomerDetailsRepository extends JpaRepository<CustomerDetails, Long> {


    Optional<CustomerDetails> findFirstByCustomerId(@Param("customerId") Long customerId, Sort sort);
    Optional<CustomerDetails> findByCustomerId(Long customerId);
    Optional<CustomerDetails> findByCustomerIdAndVersionId(@Param("customerId") Long customerId,@Param("versionId") Long versionId);

    @Query("select new phoenix.core.customer.model.request.CustomerVersionsResponse(cd.versionId,cd.createDate) from CustomerDetails cd where cd.customerId = :customerId and cd.status in (:statuses)")
    List<CustomerVersionsResponse> getVersions(@Param("customerId")Long customerId, @Param("statuses") List<CustomerDetailStatus> statuses);
}
