package bg.energo.phoenix.repository.nomenclature.crm;

import bg.energo.phoenix.model.entity.nomenclature.crm.SmsSendingNumber;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SmsSendingNumberRepository extends JpaRepository<SmsSendingNumber, Long> {

    @Query(
            """
                    select ra from SmsSendingNumber as ra
                        where ra.id<> :currentId
                        and (ra.orderingId >= :start and ra.orderingId <= :end)
                    """
    )
    List<SmsSendingNumber> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            """
                    select ra from SmsSendingNumber as ra
                        where ra.orderingId is not null
                        order by ra.name
                    """
    )
    List<SmsSendingNumber> orderByName();

    @Query(
            """
                    select count(1) from SmsSendingNumber ra
                        where lower(ra.name) = lower(:name)
                        and ra.status in :statuses
                    """
    )
    Long countSmsSendingNumbersByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    Optional<SmsSendingNumber> findByDefaultSelectionTrue();

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            s.id,
                            s.name
                        )
                        from SmsSendingNumber s
                        where s.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    @Query(
            """
                    select sa from SmsSendingNumber as sa
                        where (:prompt is null or (
                            lower(sa.name) like :prompt
                        ))
                        and sa.status in (:statuses)
                        and :excludedItemId is null or sa.id <> :excludedItemId
                        order by sa.orderingId asc
                    """
    )
    Page<SmsSendingNumber> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            """
                    select max(ra.orderingId) from SmsSendingNumber ra
                    """
    )
    Long findLastOrderingId();

    @Query(
            value = """
                    select sa from SmsSendingNumber as sa
                        where (:prompt is null or (
                            lower(sa.name) like :prompt
                        ))
                        and ((sa.status in (:statuses))
                        and (:excludedItemId is null or sa.id <> :excludedItemId)
                        or (sa.id in (:includedItemIds)))
                        order by
                            case when sa.id in (:includedItemIds) then 1 else 2 end,
                            case when sa.isHardCoded = true then 0 when sa.defaultSelection = true then 1 else 2 END,
                        sa.defaultSelection desc, sa.orderingId asc
                    """, countQuery = """
            select count(1) from SmsSendingNumber as sa
                where (:prompt is null or (
                    lower(sa.name) like :prompt
                ))
                and ((sa.status in (:statuses))
                and (:excludedItemId is null or sa.id <> :excludedItemId)
                or (sa.id in (:includedItemIds)))
                """
    )
    Page<SmsSendingNumber> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            @Param("includedItemIds") List<Long> includedItemIds,
            Pageable pageable
    );

    @Query(value = """
            select count(ssn.id) > 0 as is_used
            from nomenclature.sms_sending_numbers ssn
            where ssn.id = :id
            and (
                exists(select 1
                        from crm.sms_communications smsc
                        where smsc.sms_sending_number_id = ssn.id
                        and smsc.status = 'ACTIVE'
                )
            )
            """, nativeQuery = true)
    boolean hasActiveConnections(Long id);

    Optional<SmsSendingNumber> findByIsHardCodedTrue();
}
