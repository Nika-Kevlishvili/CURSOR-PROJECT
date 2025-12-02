package phoenix.core.customer.repository.nomenclature.address;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import phoenix.core.customer.model.entity.nomenclature.address.Region;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.response.nomenclature.NomenclatureResponse;

import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long>, QueryByExampleExecutor<Region> {

    @Query("select r from Region as r" +
                    " left join r.country as rc " +
                    " where (r.status in (:statuses))" +
                    " and (:prompt is null or (" +
                        "lower(r.name) like lower(concat('%',:prompt,'%')) or " +
                        "lower(rc.name) like lower(concat('%',:prompt,'%'))))" +
                    " and (:countryId is null or rc.id = :countryId)" +
                    " and (:excludedItemId is null or r.id <> :excludedItemId) " +
                    " order by r.defaultSelection desc, r.orderingId asc "
    )
    Page<Region> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("countryId") Long countryId,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new phoenix.core.customer.model.response.nomenclature.NomenclatureResponse(" +
                        "r.id, " +
                        "CONCAT(r.name, ' - ', rc.name), " +
                        "r.orderingId, " +
                        "r.defaultSelection, " +
                        "r.status" +
                    ") " +
                    "from Region as r" +
                    " left join r.country as rc " +
                    " where (r.status in (:statuses))" +
                    " and (:prompt is null or (" +
                        "lower(r.name) like lower(concat('%',:prompt,'%')) or " +
                        "lower(rc.name) like lower(concat('%',:prompt,'%')))) " +
                    " and (:excludedItemId is null or r.id <> :excludedItemId) " +
                    " order by r.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    Optional<Region> findByDefaultSelectionTrue();

    @Query(value = "select max(r.orderingId) from Region r")
    Long findLastOrderingId();

    @Query(
            "select r from Region as r" +
                    " where r.id <> :currentId " +
                    " and (r.orderingId >= :start and r.orderingId <= :end) "
    )
    List<Region> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select r from Region as r" +
                    " where r.orderingId is not null" +
                    " order by r.name"
    )
    List<Region> orderByName();

    @Query(
            "select count(m.id) from Municipality as m" +
                    " where m.region.id = :regionId " +
                    " and (m.status in (:statuses))"
    )
    Long getMunicipalitiesCountByStatusAndRegionId(
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("regionId") Long regionId
    );


    @Query(
            """
            select count(1) from Region r
                where r.id = :id
                and (exists (select 1 from
                    Municipality m,
                    PopulatedPlace pp,
                    CustomerCommunications cc,
                    CustomerDetails cd,
                    Customer c
                        where m.region.id = r.id
                        and pp.municipality.id = m.id
                        and cc.populatedPlaceId = pp.id
                        and cd.id = cc.customerDetailsId
                        and c.id = cd.customerId
                        and cc.status = 'ACTIVE' and c.status = 'ACTIVE'
                )
                or exists( select 1 from
                    Municipality m,
                    PopulatedPlace pp,
                    CustomerDetails cd,
                    Customer c
                        where m.region.id = r.id
                        and pp.municipality.id = m.id
                        and pp.id = cd.populatedPlaceId
                        and c.id = cd.customerId
                        and c.status = 'ACTIVE'
                ))
            """
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );
}
