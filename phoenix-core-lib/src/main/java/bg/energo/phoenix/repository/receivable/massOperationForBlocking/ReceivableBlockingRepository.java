package bg.energo.phoenix.repository.receivable.massOperationForBlocking;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.massOperationForBlocking.ReceivableBlocking;
import bg.energo.phoenix.model.response.receivable.massOperationForBlocking.ReceivableBlockingListingMiddleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReceivableBlockingRepository extends JpaRepository<ReceivableBlocking, Long> {

    @Query(nativeQuery = true,
            value = """
                select
                    mofb.id as blockingId,
                    mofb.name as name,
                    text(mofb.type) as reasonType,
                    mofb.mass_operation_blocking_status as blockingStatus,
                    mofb.status  as entityStatus
                from  receivable.mass_operation_for_blocking mofb
                where (
                    (:blockedForPayment is null or coalesce(mofb.blocked_for_payment,'false') = :blockedForPayment)
                    and (:blockedForReminderLetters is null or coalesce(mofb.blocked_for_reminder_letters,'false') = :blockedForReminderLetters)
                    and (:blockedForCalculationOfInterests is null or coalesce(mofb.blocked_for_calculation_of_late_payment,'false') = :blockedForCalculationOfInterests)
                    and (:blockedForLiabilitiesOffsetting is null or coalesce(mofb.blocked_for_liabilities_offsetting,'false') = :blockedForLiabilitiesOffsetting)
                    and (:blockedForSupplyTermination is null or coalesce(mofb.blocked_for_supply_termination,'false') = :blockedForSupplyTermination)
                    and (:blockingStatus is null or text(mofb.mass_operation_blocking_status) in :blockingStatus)
                    and (:prompt is null
                         or (:searchBy = 'ALL' and (lower(mofb.name) like :prompt
                                                    or text(mofb.id) like :prompt
                                                    or lower(text(mofb.type)) like :prompt
                                                    or lower(text(mofb.mass_operation_blocking_status)) like :prompt))
                         or (:searchBy = 'NAME' and lower(mofb.name) like :prompt)
                         or (:searchBy = 'ID' and text(mofb.id) like :prompt)
                         or (:searchBy = 'TYPE' and lower(text(mofb.type)) like :prompt)
                         or (:searchBy = 'STATUS' and lower(text(mofb.mass_operation_blocking_status)) like :prompt)
                    ))
                and
                (
                    text(mofb.status) = 'ACTIVE'
                    OR
                    (
                    text(mofb.status) in :deletedStatuses
                    and text(mofb.mass_operation_blocking_status) IN :deletedStatuses
                    )
                                    
                )
                                                and (COALESCE(:entityStatuses, NULL) IS NULL OR text(mofb.status) in(:entityStatuses)) 

                 """,
            countQuery = """
            SELECT COUNT(*)
            FROM receivable.mass_operation_for_blocking mofb
            WHERE (
                (:blockedForPayment IS NULL OR COALESCE(mofb.blocked_for_payment, 'false') = :blockedForPayment)
                AND (:blockedForReminderLetters IS NULL OR COALESCE(mofb.blocked_for_reminder_letters, 'false') = :blockedForReminderLetters)
                AND (:blockedForCalculationOfInterests IS NULL OR COALESCE(mofb.blocked_for_calculation_of_late_payment, 'false') = :blockedForCalculationOfInterests)
                AND (:blockedForLiabilitiesOffsetting IS NULL OR COALESCE(mofb.blocked_for_liabilities_offsetting, 'false') = :blockedForLiabilitiesOffsetting)
                AND (:blockedForSupplyTermination IS NULL OR COALESCE(mofb.blocked_for_supply_termination, 'false') = :blockedForSupplyTermination)
                AND (:blockingStatus IS NULL OR text(mofb.mass_operation_blocking_status) IN :blockingStatus)
                and (
                    :prompt IS NULL
                    OR (
                        :searchBy = 'ALL' AND (
                            LOWER(mofb.name) LIKE :prompt
                            OR text(mofb.id) LIKE :prompt
                            OR LOWER(text(mofb.type)) LIKE :prompt
                            OR LOWER(text(mofb.mass_operation_blocking_status)) LIKE :prompt
                        )
                    )
                    OR (:searchBy = 'NAME' AND LOWER(mofb.name) LIKE :prompt)
                    OR (:searchBy = 'ID' AND text(mofb.id) LIKE :prompt)
                    OR (:searchBy = 'TYPE' AND LOWER(text(mofb.type)) LIKE :prompt)
                    OR (:searchBy = 'STATUS' AND LOWER(text(mofb.mass_operation_blocking_status)) LIKE :prompt)
                )
            )
            AND (
                text(mofb.status) = 'ACTIVE'
                OR (
                    text(mofb.status) IN :deletedStatuses
                    AND text(mofb.mass_operation_blocking_status) IN :deletedStatuses
                )

            )
                and (COALESCE(:entityStatuses, NULL) IS NULL OR text(mofb.status) in(:entityStatuses)) 

                """
    )
    Page<ReceivableBlockingListingMiddleResponse> filter(
            @Param("prompt") String prompt,
            @Param("searchBy") String searchBy,
            @Param("blockedForPayment") Boolean blockedForPayment,
            @Param("blockedForReminderLetters") Boolean blockedForLetters,
            @Param("blockedForCalculationOfInterests") Boolean blockedForCalculations,
            @Param("blockedForLiabilitiesOffsetting") Boolean blockedForLiabilities,
            @Param("blockedForSupplyTermination") Boolean blockedForTermination,
            @Param("blockingStatus") List<String> blockingStatus,
            @Param("deletedStatuses") List<String> deletedStatuses,
            @Param("entityStatuses") List<String> entityStatuses,
            Pageable pageable
    );

    boolean existsByName(String name);

    Optional<ReceivableBlocking> findByIdAndStatus(Long id, EntityStatus status);

    @Query(value = """
    Select receivable.receivable_condition_eval(:condition) as id
    """, nativeQuery = true)
    List<Long> getReceivableByCondition(@Param("condition") String  condition);

    @Query(value = """
    Select receivable.liability_condition_eval(:condition) as id
    """, nativeQuery = true)
    List<Long> getLiabilitiesByCondition (@Param("condition") String  condition);

    @Query(value = """
    Select receivable.payments_condition_eval(:condition) as id
    """, nativeQuery = true)
    List<Long> getPaymentsByCondition (@Param("condition") String  condition);


    @Query(value = """
    select mofb.condition_text from receivable.liabilities_condition_replacemets mofb
    where mofb.is_key = true
""", nativeQuery = true)
    List<String> getConditionKeys();

}
