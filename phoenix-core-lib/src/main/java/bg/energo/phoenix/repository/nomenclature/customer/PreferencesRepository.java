package bg.energo.phoenix.repository.nomenclature.customer;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.entity.nomenclature.customer.Preferences;
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
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreferencesRepository extends JpaRepository<Preferences,Long> {

    @Query("""
        select p
        from Preferences p
        where p.id = :id
        and p.status in :statuses
    """)
    Optional<Preferences> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses")List<NomenclatureItemStatus> statuses
    );

    @Query(
            """
                    select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(
                        c.id, c.name, c.orderingId, c.isDefault, c.status
                    )
                    from Preferences as c
                    where (:prompt is null or lower(c.name) like :prompt)
                    and (c.status in (:statuses))
                    order by c.orderingId asc
                    """
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    @Query("select p from Preferences p where p.id=:id and p.status in (:statuses)")
    Optional<Preferences> findByIdAndStatuses(@Param("id") Long id, @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query("""
            select p from Preferences p
            where p.id <> :currentId
            and (p.orderingId >= :start and p.orderingId <= :end)""")
    List<Preferences> findInOrderingIdRange(@Param("start") Long start, @Param("end") Long end, @Param("currentId") Long currentId, Sort sort);

    @Query("""
        select p from Preferences p
        where p.orderingId is not null
        order by p.name
""")
    List<Preferences> orderByName();

    @Query("select max(p.orderingId) from Preferences p")
    Long findLastSortOrder();

    @Query("select p from Preferences p where p.isDefault=true")
    Optional<Preferences> findByDefaultSelection();



    @Query(
            "select c from Preferences as c" +
                    " where (:prompt is null or lower(c.name) like :prompt)" +
                    " and (((c.status in (:statuses))" +
                    " and (:excludedItemId is null or c.id <> :excludedItemId)) " +
                    " or (c.id in (:includedItemIds)))" +
                    " order by case when c.id in (:includedItemIds) then 1 else 2 end," +
                    " c.isDefault desc, c.orderingId asc"
    )
    Page<Preferences> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            @Param("includedItemIds") List<Long> includedItemIds,
            Pageable pageable
    );

    @Query(
            """
            select count(1) from Preferences p
                where p.id = :id
                and exists (select 1 from CustomerPreference cp, CustomerDetails cd, Customer c
                    where cp.preferences.id = :id
                    and cp.customerDetail.id = cd.id
                    and cd.customerId = c.id
                    and cp.status = 'ACTIVE' and c.status = 'ACTIVE')
            """
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );



    @Query("""
        select new bg.energo.phoenix.model.CacheObject(p.id, p.name)
        from Preferences  p
        where p.name = :name
        and p.status =:status
    """)
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Optional<CacheObject> findByNameAndStatus(@Param("name") String name,
                                              @Param("status") NomenclatureItemStatus status);

    @Query(
            """
            select count(p.id) from Preferences p
                where lower(p.name) = lower(:name)
                and p.status in (:statuses)
            """
    )
    Long countPreferencesByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            p.id,
                            p.name
                        )
                        from Preferences p
                        where p.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
