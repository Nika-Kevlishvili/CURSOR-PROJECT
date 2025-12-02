package bg.energo.phoenix.repository.pod.discount;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.pod.discount.Discount;
import bg.energo.phoenix.model.response.pod.discount.DiscountListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {
    Optional<Discount> findByIdAndStatusIn(Long id, List<EntityStatus> statuses);

    @Query("""
            select d from Discount d
            join DiscountPointOfDeliveries dpod on dpod.discountId = d.id
            where dpod.pointOfDeliveryId = :pointOfDeliveryId
            and dpod.status = 'ACTIVE'
            and d.status = 'ACTIVE'
            """)
    List<Discount> findActiveDiscountByPointOfDeliveryId(@Param("pointOfDeliveryId") Long pointOfDeliveryId);

    @Query("""
            select d from Discount d
            join DiscountPointOfDeliveries dpod on dpod.discountId = d.id
            where dpod.pointOfDeliveryId = :pointOfDeliveryId
            and dpod.status = 'ACTIVE'
            and d.status = 'ACTIVE'
            and d.customerId = :customerId
            """)
    List<Discount> findActiveDiscountByPointOfDeliveryIdAndCustomerId(@Param("pointOfDeliveryId") Long pointOfDeliveryId, @Param("customerId") Long customerId);

    @Query("""
            select new bg.energo.phoenix.model.response.pod.discount.DiscountListResponse(
            d.id,
            dp.podIdentifier,
            concat(c.identifier, ' (', coalesce(cd.name, ''), ' ', coalesce(cd.middleName, ''), ' ', coalesce(cd.lastName, ''), ')') ,
            d.dateFrom,
            d.dateTo,
            d.amountInPercent,
            d.amountInMoneyPerKWH,
            case when d.invoiced = true then 'YES' else 'NO' end,
            d.createDate,
            d.status)
            from Discount d
            join VwDiscountPods dp on dp.discountId = d.id
            join Customer c on d.customerId = c.id
            join CustomerDetails cd on cd.id = c.lastCustomerDetailId
            where d.status in :statuses
            and (cast(:dateFromBegin as date) is null or d.dateFrom >= :dateFromBegin)
            and (cast(:dateFromEnd as date) is null or d.dateFrom <= :dateFromEnd)
            and (cast(:dateToBegin as date) is null or d.dateTo >= :dateToBegin)
            and (cast(:dateToEnd as date) is null or d.dateTo <= :dateToEnd)
            and (
                    :invoiced is null
                    or :invoiced = 'ALL'
                    or (:invoiced = 'YES' and d.invoiced = true)
                    or (:invoiced = 'NO' and (d.invoiced is null or d.invoiced = false))
                    )
            and (
                :discountParameterFilterField is null or :discountParameterFilterField = 'ALL'
                and (
                    lower(c.identifier) like :prompt
                    or cast(d.id as string) like :prompt
                    or lower(dp.podIdentifier) like :prompt
                )
                or (
                    :discountParameterFilterField is null
                    or :discountParameterFilterField = 'CUSTOMER_IDENTIFIER'
                    and lower(c.identifier) like :prompt
                )
                or (
                    :discountParameterFilterField is null
                    or :discountParameterFilterField = 'ID'
                    and cast(d.id as string) like :prompt
                )
                or (
                    :discountParameterFilterField is null
                    or :discountParameterFilterField = 'POD_IDENTIFIER'
                    and lower(dp.podIdentifier) like :prompt
                )
            )
            """)
    Page<DiscountListResponse> filter(
            @Param("prompt") String prompt,
            @Param("dateFromBegin") LocalDate dateFromBegin,
            @Param("dateFromEnd") LocalDate dateFromEnd,
            @Param("dateToBegin") LocalDate dateToBegin,
            @Param("dateToEnd") LocalDate dateToEnd,
            @Param("invoiced") String invoiced,
            @Param("discountParameterFilterField") String discountParameterFilterField,
            @Param("statuses") List<EntityStatus> statuses,
            Pageable pageRequest);
    @Modifying
    @Query("""
            update Discount d
            set d.invoiced = true
            where d.id in
                (select detail.discountId
                from InvoiceStandardDetailedData detail
                where detail.detailType = 'DISCOUNT'
                and detail.invoiceId in (:invoiceIds))
            """)
    void markDiscountsAsInvoiced(@Param("invoiceIds") List<Long> invoicesToCheckDiscount);
}
