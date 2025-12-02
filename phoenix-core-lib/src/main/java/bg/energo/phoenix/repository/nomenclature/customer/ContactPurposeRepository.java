package bg.energo.phoenix.repository.nomenclature.customer;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.customer.ContactPurpose;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ContactPurposeRepository extends JpaRepository<ContactPurpose, Long> {

    @Query(
            "select c from ContactPurpose as c" +
                    " where (:prompt is null or lower(c.name) like :prompt)" +
                    " and (((c.status in (:statuses))" +
                    " and (:excludedItemId is null or c.id <> :excludedItemId)) " +
                    " or (c.id in (:includedItemIds)))" +
                    " order by case when c.id in (:includedItemIds) then 1 else 2 end," +
                    " case when c.isHardCoded = true then 0 when c.defaultSelection = true then 1 else 2 END , c.orderingId asc"
    )
    Page<ContactPurpose> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            @Param("includedItemIds") List<Long> includedItemIds,
            Pageable pageable
    );
    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(" +
                    "c.id, " +
                    "c.name, " +
                    "c.orderingId, " +
                    "c.defaultSelection, " +
                    "c.status" +
                    ") " +
                    "from ContactPurpose as c" +
                    " where (:prompt is null or lower(c.name) like :prompt)" +
                    " and (c.status in (:statuses))" +
                    " order by c.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<ContactPurpose> findByDefaultSelectionTrue();

    @Query("select max(c.orderingId) from ContactPurpose c")
    Long findLastOrderingId();

    @Query(
            "select c from ContactPurpose as c" +
                    " where c.id <> :currentId " +
                    " and (c.orderingId >= :start and c.orderingId <= :end) "
    )
    List<ContactPurpose> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select c from ContactPurpose as c" +
                    " where c.orderingId is not null" +
                    " order by c.name"
    )
    List<ContactPurpose> orderByName();

    boolean existsByIdAndStatus(Long id, NomenclatureItemStatus status);
    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> status);

    @Query(
            "select cp from ContactPurpose as cp" +
                    " where cp.id = :id" +
                    " and cp.status in :statuses"
    )
    Optional<ContactPurpose> findByIdAndStatuses(
            @Param("id") Long id,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    @Query(
            """
                    select count(1) from ContactPurpose cp 
                                         where cp.id = :id 
                                         and (exists (select 1 from 
                                             CustomerCommContactPurposes cccp,
                                             CustomerCommunications cc,
                                             CustomerDetails cd,
                                             Customer c 
                                                 where cccp.contactPurposeId = :id 
                                                 and cc.id = cccp.customerCommunicationsId 
                                                 and cd.id = cc.customerDetailsId 
                                                 and cccp.status = 'ACTIVE' and cc.status = 'ACTIVE' and c.status = 'ACTIVE'  )
                                             or exists (
                                                select 1 from Reminder r
                                                where r.contactPurposeId = :id
                                                and cast(r.status as string ) = 'ACTIVE')
                                             or exists (
                                               select 1 from SmsCommunication sm
                                               join SmsCommunicationContactPurpose smcp on smcp.smsCommunicationId=sm.id
                                               where smcp.contactPurposeId = :id
                                               and cast(smcp.status as string ) = 'ACTIVE'
                                               and cast(sm.status as string ) ='ACTIVE')
                                             or exists (
                                               select 1 from EmailCommunication ec
                                               join EmailCommunicationContactPurpose ecp on ecp.emailCommunicationId=ec.id
                                               where ecp.contactPurposeId = :id
                                               and cast(ecp.status as string ) = 'ACTIVE'
                                               and cast(ec.entityStatus as string ) ='ACTIVE')
                                               )"""
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );


    @Query("""
        select new bg.energo.phoenix.model.CacheObject(c.id, c.name)
        from ContactPurpose c
        where c.name = :name
        and c.status =:status
    """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> getByNameAndStatus(@Param("name") String name,
                                             @Param("status") NomenclatureItemStatus status);

    @Query(
            """
            select count(c.id) from ContactPurpose c
                where lower(c.name) = lower(:name)
                and c.status in (:statuses)
            """
    )
    Long countContactPurposeByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            c.id,
                            c.name
                        )
                        from ContactPurpose c
                        where c.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    @Query("""
        select cp from ContactPurpose cp
        where cp.id in :ids
        and cp.status='ACTIVE'
""")
    List<ContactPurpose> findByIdsIn(Set<Long> ids);
}
