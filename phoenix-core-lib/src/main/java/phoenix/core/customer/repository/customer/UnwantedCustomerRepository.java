package phoenix.core.customer.repository.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import phoenix.core.customer.model.entity.customer.UnwantedCustomer;
import phoenix.core.customer.model.response.customer.UnwantedCustomer.UnwantedCustomerResponse;

import java.util.Collection;
import java.util.Optional;

public interface UnwantedCustomerRepository extends JpaRepository<UnwantedCustomer, Long> {
    Optional<UnwantedCustomer> findByIdentifier(String identifier);

    @Query("select new phoenix.core.customer.model.response.customer.UnwantedCustomer.UnwantedCustomerResponse(" +
            "uc.id, " +
            "uc.identifier," +
            "uc.name," +
            "ucr.id," +
            "ucr.name," +
            "uc.additionalInfo," +
            "uc.createContractRestriction," +
            "uc.createOrderRestriction," +
            "uc.systemUserid," +
            "uc.status," +
            "uc.createDate," +
            "uc.modifyDate," +
            "uc.modifySystemUserId" +
            ") from UnwantedCustomer as uc " +
            "                    join UnwantedCustomerReason ucr on uc.unwantedCustomerReasonId  = ucr.id " +
            "                    where  " +
            "                    (:prompt is null or " +
            "                     (:column_name =  'ALL' and (uc.identifier like %:prompt% or" +
            "                                                lower(uc.name) like %:prompt% or " +
            "                                                lower(ucr.name) like %:prompt% ))" +
            "                       or    (:column_name = 'IDENTIFIER' and lower(uc.identifier) like %:prompt%) " +
            "                       or    (:column_name = 'NAME' and lower(uc.name)  like %:prompt%) " +
            "                       or    (:column_name = 'REASON' and lower(ucr.name)  like %:prompt%)) " +
            "                  and ( COALESCE(:reason,0) = 0 or  ucr.id in :reason )")
    Page<UnwantedCustomerResponse> filter(@Param("prompt") String prompt, @Param("column_name") String columnName,
                                          @Param("reason") Collection<Long> reason, Pageable pageable);
}
