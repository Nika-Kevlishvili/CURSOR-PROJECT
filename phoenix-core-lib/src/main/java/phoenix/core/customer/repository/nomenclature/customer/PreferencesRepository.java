package phoenix.core.customer.repository.nomenclature.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import phoenix.core.customer.model.entity.nomenclature.customer.Preferences;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.response.nomenclature.NomenclatureResponse;

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
                    select new phoenix.core.customer.model.response.nomenclature.NomenclatureResponse(
                        c.id, c.name, c.orderingId, c.isDefault, c.status
                    )
                    from Preferences as c
                    where (:prompt is null or lower(c.name) like lower(concat('%',:prompt,'%')))
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

    @Query("""
        select p from Preferences p
        where (:prompt is null or lower(p.name) like lower(concat('%',:prompt,'%')))
        and (p.status in (:statuses))
        and (:excludedItemId is null or p.id <> :excludedItemId)
        order by p.isDefault desc , p.orderingId asc
        """)
    Page<Preferences> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
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
}
