package bg.energo.phoenix.repository.customer;

import bg.energo.phoenix.model.entity.customer.UnwantedCustomer;
import bg.energo.phoenix.model.enums.customer.unwantedCustomer.UnwantedCustomerStatus;
import bg.energo.phoenix.model.response.customer.UnwantedCustomer.UnwantedCustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UnwantedCustomerRepository extends JpaRepository<UnwantedCustomer, Long> {
    /**
     * <h1>UnwantedCustomerRepository findByIdentifier</h1>
     * selects record by identifier
     *
     * @param identifier identification number of the customer
     * @return UnwantedCustomer object
     */
    Optional<UnwantedCustomer> findByIdentifier(String identifier);

    /**
     * <h1>UnwantedCustomerRepository findByIdentifierAndStatusIn</h1>
     * selects record by identifier and status
     *
     * @param identifier identification number of the customer
     * @param statuses customer status list
     * @return UnwantedCustomer object
     */
    Optional<UnwantedCustomer> findByIdentifierAndStatusIn(String identifier, List<UnwantedCustomerStatus> statuses);

    /**
     * <h1>UnwantedCustomerRepository findByIdAndStatuses</h1>
     * selects record by identifier  and statuses
     *
     * @param id       identification number of the customer
     * @param statuses UnwantedCustomerStatus list
     * @return UnwantedCustomer object
     */
    @Query("select p from UnwantedCustomer p where p.id = :id and p.status in (:statuses)")
    Optional<UnwantedCustomer> findByIdAndStatuses(@Param("id") Long id, @Param("statuses") List<UnwantedCustomerStatus> statuses);

    /**
     * <h1>UnwantedCustomerRepository filter</h1>
     *
     * @param prompt     string key for searching
     * @param columnName in which column should script search prompt value
     * @param reason     list of unwanted customer reason ids
     * @param statuses   unwanted customer statuses
     * @param pageable   pagination object for ordering and sorting selected list
     * @return paginated list of UnwantedCustomerResponse
     */
    @Query(value = """
            select new bg.energo.phoenix.model.response.customer.UnwantedCustomer.UnwantedCustomerResponse(
                uc.id,
                uc.identifier,
                uc.name,
                ucr.id,
                ucr.name,
                uc.additionalInfo,
                uc.createContractRestriction,
                uc.createOrderRestriction,
                uc.systemUserId,
                uc.status,
                uc.createDate,
                uc.modifyDate,
                uc.modifySystemUserId
            )
            from UnwantedCustomer as uc
            join UnwantedCustomerReason ucr on uc.unwantedCustomerReasonId  = ucr.id
                where (:prompt is null or
                        (:column_name = 'ALL' and (lower(uc.identifier) like %:prompt% or lower(uc.name) like %:prompt% or lower(ucr.name) like %:prompt%))
                        or (:column_name = 'IDENTIFIER' and lower(uc.identifier) like %:prompt%)
                        or (:column_name = 'NAME' and lower(uc.name) like %:prompt%)
                        or (:column_name = 'REASON' and lower(ucr.name) like %:prompt%))
                and ((:reason) is null or ucr.id in :reason)
                and  uc.status in :statuses
            """
    )
    Page<UnwantedCustomerResponse> filter(
            @Param("prompt") String prompt,
            @Param("column_name") String columnName,
            @Param("reason") Collection<Long> reason,
            @Param("statuses") List<UnwantedCustomerStatus> statuses,
            Pageable pageable
    );
}
