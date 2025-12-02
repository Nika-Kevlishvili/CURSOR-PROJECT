package phoenix.core.customer.repository.nomenclature.address;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import phoenix.core.customer.model.entity.nomenclature.address.Municipality;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.response.nomenclature.NomenclatureResponse;

import java.util.List;
import java.util.Optional;

public interface MunicipalityRepository extends JpaRepository<Municipality, Long> {
    @Query("select m from Municipality as m" +
            " left join m.region as mr " +
            " left join mr.country as mrc " +
            " where (m.status in (:statuses))" +
            " and (:prompt is null or (" +
                " lower(m.name) like lower(concat('%',:prompt,'%')) or " +
                " lower(mr.name) like lower(concat('%',:prompt,'%')) or " +
                " lower(mrc.name) like lower(concat('%',:prompt,'%'))))" +
            " and (:regionId is null or mr.id = :regionId)" +
            " and (:excludedItemId is null or m.id <> :excludedItemId) " +
            " order by m.defaultSelection desc, m.orderingId asc "
    )
    Page<Municipality> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("regionId") Long regionId,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new phoenix.core.customer.model.response.nomenclature.NomenclatureResponse(" +
                        "m.id, " +
                        "CONCAT(m.name, ' - ', mr.name, ' - ', mrc.name), " +
                        "m.orderingId, " +
                        "m.defaultSelection, " +
                        "m.status" +
                    ") " +
                    "from Municipality as m" +
                    " left join m.region as mr " +
                    " left join mr.country as mrc " +
                    " where (m.status in (:statuses))" +
                    " and (:prompt is null or (" +
                        " lower(m.name) like lower(concat('%',:prompt,'%')) or " +
                        " lower(mr.name) like lower(concat('%',:prompt,'%')) or " +
                        " lower(mrc.name) like lower(concat('%',:prompt,'%')))) " +
                    " and (:excludedItemId is null or m.id <> :excludedItemId) " +
                    " order by m.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    Optional<Municipality> findByDefaultSelectionTrue();

    @Query(value = "select max(m.orderingId) from Municipality m")
    Long findLastOrderingId();

    @Query(
            "select m from Municipality as m" +
                    " where m.id <> :currentId " +
                    " and (m.orderingId >= :start and m.orderingId <= :end) "
    )
    List<Municipality> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select m from Municipality as m" +
                    " where m.orderingId is not null" +
                    " order by m.name"
    )
    List<Municipality> orderByName();

    @Query(
            "select count(p.id) from PopulatedPlace as p" +
                    " where p.municipality.id = :municipalityId " +
                    " and (p.status in (:statuses))"
    )
    Long getPopulatedPlacesCountByStatusAndMunicipalityId(
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("municipalityId") Long municipalityId
    );

    @Query(
            """
            select count(1) from Municipality m
                where m.id = :id
                and (exists (select 1 from
                    PopulatedPlace pp,
                    CustomerCommunications cc,
                    CustomerDetails cd,
                    Customer c
                        where pp.municipality.id = m.id
                        and cc.populatedPlaceId = pp.id
                        and cd.id = cc.customerDetailsId
                        and c.id = cd.customerId
                        and cc.status = 'ACTIVE' and c.status = 'ACTIVE'
                )
                or exists( select 1 from
                    PopulatedPlace pp,
                    CustomerDetails cd,
                    Customer c
                        where pp.municipality.id = m.id
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
