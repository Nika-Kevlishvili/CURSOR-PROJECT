package phoenix.core.customer.repository.nomenclature.address;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import phoenix.core.customer.model.entity.nomenclature.address.ResidentialArea;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.enums.nomenclature.ResidentialAreaType;
import phoenix.core.customer.model.response.nomenclature.NomenclatureResponse;
import phoenix.core.customer.model.response.nomenclature.ResidentialAreaTreeResponse;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResidentialAreaRepository extends JpaRepository<ResidentialArea, Long> {

    @Query("""
            select p from ResidentialArea p
            where p.status in :statuses
            and p.id = :id
            """)
    Optional<ResidentialArea> findByIdAndStatuses(@Param("id") Long id, @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query("""
        select r
        from ResidentialArea r
        where r.id = :id
        and r.populatedPlace.id = :populatedPlaceId
        and r.status in :statuses
    """)
    Optional<ResidentialArea> findByIdAndPopulatedPlaceIdAndStatus(
            @Param("id") Long id,
            @Param("populatedPlaceId") Long populatedPlaceId,
            @Param("statuses")List<NomenclatureItemStatus> statuses
    );

    @Query("""
            select p from ResidentialArea p
            where p.id <> :currentId
            and (p.orderingId >= :start and p.orderingId <= :end)
            """)
    List<ResidentialArea> findInOrderingIdRange(@Param("start") Long start, @Param("end") Long end, @Param("currentId") Long currentId, Sort sort);

    @Query(
            "select c from ResidentialArea as c" +
                    " where c.orderingId is not null" +
                    " order by c.name"
    )
    List<ResidentialArea> orderByName();

    @Query("select max(c.orderingId) from ResidentialArea c")
    Long findLastOrderingId();

    Optional<ResidentialArea> findByDefaultSelectionTrue();

    @Query(
            """
                    select new phoenix.core.customer.model.response.nomenclature.NomenclatureResponse(
                            p.id,
                            concat(p.name, ' - ', p.type, ' - ', pp.name,' - ', m.name, ' - ', rg.name, ' - ', ca.name) ,
                            p.orderingId,
                            p.defaultSelection,
                            p.status
                    )
                    from ResidentialArea p
                        left join p.populatedPlace pp
                        left join pp.municipality m
                        left join m.region rg
                        left join rg.country ca
                    where (p.status in (:statuses))
                    and (:prompt is null or
                        (lower(p.name) like lower(concat('%',:prompt,'%')) or
                        lower(pp.name) like lower(concat('%',:prompt,'%')) or
                        lower(m.name) like lower(concat('%',:prompt,'%')) or
                        lower(rg.name) like lower(concat('%',:prompt,'%')) or
                        lower(ca.name) like lower(concat('%',:prompt,'%'))))
                    and (:excludedItemId is null or p.id <> :excludedItemId)
                    order by p.orderingId asc
                    """
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            """
                    select p from ResidentialArea as p
                        left join p.populatedPlace pp
                        left join pp.municipality m
                        left join m.region rg
                        left join rg.country ca
                    where (p.status in (:statuses))
                    and (:prompt is null or
                        (lower(p.name) like lower(concat('%',:prompt,'%')) or
                        lower(pp.name) like lower(concat('%',:prompt,'%')) or
                        lower(m.name) like lower(concat('%',:prompt,'%')) or
                        lower(rg.name) like lower(concat('%',:prompt,'%')) or
                        lower(ca.name) like lower(concat('%',:prompt,'%'))))
                    and (:populatedPlaceId is null or pp.id = :populatedPlaceId)
                    and (CAST(:residentialAreaType AS string) is null or p.type = :residentialAreaType)
                    and (:excludedItemId is null or p.id <> :excludedItemId)
                    order by p.orderingId asc
            """
    )
    Page<ResidentialArea> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("populatedPlaceId") Long populatedPlaceId,
            @Param("residentialAreaType") ResidentialAreaType residentialAreaType,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            """
            select new phoenix.core.customer.model.response.nomenclature.ResidentialAreaTreeResponse(
                pp.id, 
                pp.name, 
                m.id, 
                m.name, 
                rg.id, 
                rg.name, 
                ca.id, 
                ca.name
            )
            from ResidentialArea p
                left join p.populatedPlace pp
                left join pp.municipality m
                left join m.region rg
                left join rg.country ca
            where p.id = :residentialAreaId
            """
    )
    ResidentialAreaTreeResponse getResidentialAreaTreeView(@Param("residentialAreaId") Long residentialAreaId);

    @Query(
            """
            select count(1) from ResidentialArea r
                where r.id = :id
                and (exists (select 1 from CustomerDetails cd, Customer c
                        where cd.residentialAreaId = :id
                        and c.id = cd.customerId
                        and c.status = 'ACTIVE')
                or exists (select 1 from CustomerCommunications cc, CustomerDetails cd, Customer c
                        where cc.residentialAreaId = :id
                        and cd.id = cc.customerDetailsId
                        and c.id = cd.customerId
                        and cc.status = 'ACTIVE' and c.status = 'ACTIVE')
                )
            """
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );
}
