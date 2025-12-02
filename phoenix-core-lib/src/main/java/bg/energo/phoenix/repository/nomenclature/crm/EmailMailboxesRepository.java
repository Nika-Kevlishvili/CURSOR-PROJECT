package bg.energo.phoenix.repository.nomenclature.crm;

import bg.energo.phoenix.model.entity.nomenclature.crm.EmailMailboxes;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailMailboxesRepository extends JpaRepository<EmailMailboxes, Long> {

    Optional<EmailMailboxes> findByName(String name);

    Optional<EmailMailboxes> findByIsHardCodedTrue();

    @Query("""
            select COUNT(eb) > 0
            from EmailMailboxes eb
            where lower(eb.name) = lower(:name)
            and eb.status in :statuses
            """)
    boolean existsEmailMailboxesWithNameAndStatus(String name, List<NomenclatureItemStatus> statuses);

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(em.id, em.name, em.orderingId, em.defaultSelection, em.status) " +
                    "from EmailMailboxes as em" +
                    " where (:prompt is null or lower(em.name) like :prompt)" +
                    " and (:excludedItemId is null or em.id <> :excludedItemId) " +
                    " and (em.status in (:statuses))" +
                    " order by em.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );


    @Query("""
                select max(em.orderingId) from EmailMailboxes em
            """)
    Long lastOrderingId();

    Optional<EmailMailboxes> findByDefaultSelectionTrue();

    Optional<EmailMailboxes> findByEmailForSendingInvoicesTrue();

    Optional<EmailMailboxes> findByEmailForGridOperatorTrue();

    Optional<EmailMailboxes> findByCommunicationForContractTrue();


    @Query("""
                    select em from EmailMailboxes em
                    where em.id=:id and em.status in :statuses
            """)
    Optional<EmailMailboxes> findByIdAndStatuses(Long id, List<NomenclatureItemStatus> statuses);


    @Query("""
                    select em from EmailMailboxes em
                    where em.orderingId is not null
                    order by em.name
            """)
    List<EmailMailboxes> orderByName();

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            "select em from EmailMailboxes as em" +
                    " where em.id <> :currentId " +
                    " and (em.orderingId >= :start and em.orderingId <= :end) "
    )
    List<EmailMailboxes> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query("""
            select count(em) > 0
            from EmailMailboxes em
            where em.emailForSendingInvoices = true
            and em.status in :statuses
            """)
    boolean existsByEmailForSendingInvoicesAndStatus(List<NomenclatureItemStatus> statuses);

    boolean existsByEmailForGridOperatorTrue();

    @Query("""
            select count(em) > 0
            from EmailMailboxes em
            where em.emailForGridOperator = true
            and em.status in :statuses
            """)
    boolean existsByEmailForGridOperatorAndStatus(List<NomenclatureItemStatus> statuses);

    @Query("""
            select count(em) > 0
            from EmailMailboxes em
            where em.communicationForContract = true
            and em.status in :statuses
            """)
    boolean existsByCommunicationForContractAndStatus(List<NomenclatureItemStatus> statuses);

    @Query("""
                    select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                                       em.id,
                                        em.name
                                    )
                    from EmailMailboxes em
                    where em.id in :ids
            """)
    List<ActivityNomenclatureResponse> findByIdsIn(List<Long> ids);


    @Query("""
                    select em from EmailMailboxes em
                    where (:prompt is null or lower(em.name) like %:prompt%)
                      and (em.status in :statuses)
                      and (:excludedItemId is null or em.id <> :excludedItemId)
                    order by em.isHardCoded desc, em.defaultSelection desc, em.orderingId asc
            """)
    Page<EmailMailboxes> filter(String prompt, List<NomenclatureItemStatus> statuses, Long excludedItemId, Pageable pageable);

    @Query(value = """
            select count(eb.id) > 0 as is_used
            from nomenclature.email_mailboxes eb
            where eb.id = :id
            and (
                exists(
                    select 1
                    from crm.email_communications ec
                    where ec.email_mailbox_id = eb.id
                    and ec.status = 'ACTIVE'
                )
            )
            """, nativeQuery = true)
    boolean hasActiveConnections(Long id);

    @Query(
            """
                     select max(ra.orderingId) from EmailMailboxes ra
                    """
    )
    Long findLastOrderingId();

    @Query("""
        select em.emailAddress
        from EmailMailboxes em
        where em.defaultSelection = true
        and em.emailForGridOperator = true
        and em.status = 'ACTIVE'
            """)
    Optional<String> findDefaultGridOperatorMail();
}
