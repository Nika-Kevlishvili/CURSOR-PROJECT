package phoenix.core.customer.repository.nomenclature.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import phoenix.core.customer.model.entity.nomenclature.customer.EconomicBranchNCEA;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.response.nomenclature.NomenclatureResponse;

import java.util.List;
import java.util.Optional;


@Repository
public interface EconomicBranchNCEARepository extends JpaRepository<EconomicBranchNCEA,Long> {

    @Query("""
        select e
        from EconomicBranchNCEA e
        where e.id = :id
        and e.status in :statuses
    """)
    Optional<EconomicBranchNCEA> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses")List<NomenclatureItemStatus> statuses
    );

    @Query(
            """
                    select new phoenix.core.customer.model.response.nomenclature.NomenclatureResponse(
                        c.id, c.name, c.orderingId, c.isDefault, c.status
                    )
                    from EconomicBranchNCEA as c
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

    @Query("select p from EconomicBranchNCEA p where p.id=:id and p.status in (:statuses)")
    Optional<EconomicBranchNCEA> findByIdAndStatuses(@Param("id") Long id, @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query("""
            select p from EconomicBranchNCEA p
            where p.id <> :currentId
            and (p.orderingId >= :start and p.orderingId <= :end)""")
    List<EconomicBranchNCEA> findInOrderingIdRange(@Param("start") Long start, @Param("end") Long end, @Param("currentId") Long currentId, Sort sort);

    @Query("""
        select p from EconomicBranchNCEA p
        where p.orderingId is not null
        order by p.name
""")
    List<EconomicBranchNCEA> orderByName();

    @Query("select max(p.orderingId) from EconomicBranchNCEA p")
    Long findLastSortOrder();

    @Query("select p from EconomicBranchNCEA p where p.isDefault=true")
    Optional<EconomicBranchNCEA> findByDefaultSelection();

    @Query("""
        select p from EconomicBranchNCEA p
        where (:prompt is null or lower(p.name) like lower(concat('%',:prompt,'%')))
        and (p.status in (:statuses))
        and (:excludedItemId is null or p.id <> :excludedItemId)
        order by p.isDefault desc, p.orderingId asc
        """)
    Page<EconomicBranchNCEA> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            """
            select count(1) from EconomicBranchNCEA ebncea
                where ebncea.id = :id
                and exists (select 1 from CustomerDetails cd, Customer c
                    where cd.economicBranchNceaId = :id and cd.customerId = c.id
                    and c.status = 'ACTIVE')
            """
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );
}
