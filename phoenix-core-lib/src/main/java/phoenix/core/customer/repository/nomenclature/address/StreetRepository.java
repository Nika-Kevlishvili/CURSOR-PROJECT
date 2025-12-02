package phoenix.core.customer.repository.nomenclature.address;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import phoenix.core.customer.model.entity.nomenclature.address.Street;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.enums.nomenclature.StreetType;
import phoenix.core.customer.model.response.nomenclature.NomenclatureResponse;
import phoenix.core.customer.model.response.nomenclature.StreetTreeResponse;

import java.util.List;
import java.util.Optional;

@Repository
public interface StreetRepository extends JpaRepository<Street, Long> {

    @Query("""
            select p from Street p
            where p.id = :id
            and p.status in :statuses
            """)
    Optional<Street> findByIdAndStatuses(@Param("id") Long id, @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query("""
        select s
        from Street s
        where s.id = :id
        and s.populatedPlace.id = :populatedPlaceId
        and s.status in :statuses
    """)
    Optional<Street> findByIdAndPopulatedPlaceIdAndStatus(
            @Param("id") Long id,
            @Param("populatedPlaceId") Long populatedPlaceId,
            @Param("statuses")List<NomenclatureItemStatus> statuses
    );

    @Query(
            """
                    select p from Street p
                    where p.id <> :currentId
                    and (p.orderingId >= :start and p.orderingId <= :end)
                            """
    )
    List<Street> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select p from Street p" +
                    " where p.orderingId is not null" +
                    " order by p.name"
    )
    List<Street> orderByName();

    @Query("select max(p.orderingId) from Street p")
    Long findLastOrderingId();

    Optional<Street> findByDefaultSelectionTrue();

    @Query(
            """
                    select new phoenix.core.customer.model.response.nomenclature.NomenclatureResponse(
                            p.id,
                            concat(p.name, ' - ', p.type, ' - ', pp.name,' - ', m.name, ' - ', rg.name, ' - ', ca.name) ,
                            p.orderingId,
                            p.defaultSelection,
                            p.status
                            )
                     from Street p
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
                    select p from Street as p
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
                    and (CAST(:streetType AS string) is null or p.type = :streetType)
                    and (:excludedItemId is null or p.id <> :excludedItemId)
                    order by p.orderingId asc
            """
    )
    Page<Street> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("populatedPlaceId") Long populatedPlaceId,
            @Param("streetType") StreetType streetType,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            """
            select new phoenix.core.customer.model.response.nomenclature.StreetTreeResponse(
                pp.id,
                pp.name,
                m.id,
                m.name,
                rg.id,
                rg.name,
                ca.id,
                ca.name
            )
            from Street p
                left join p.populatedPlace pp
                left join pp.municipality m
                left join m.region rg
                left join rg.country ca
            where p.id = :streetId
            """
    )
    StreetTreeResponse getStreetTreeView(@Param("streetId") Long streetId);

    @Query(
            """
            select count(1) from Street s
                where s.id = :id
                and (exists (select 1 from CustomerDetails cd, Customer c
                        where cd.streetId = :id
                        and c.id = cd.customerId
                        and c.status = 'ACTIVE')
                or exists (select 1 from CustomerCommunications cc, CustomerDetails cd, Customer c
                        where cc.streetId = :id
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
