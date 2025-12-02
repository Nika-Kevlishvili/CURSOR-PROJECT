package phoenix.core.customer.repository.nomenclature.address;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import phoenix.core.customer.model.entity.nomenclature.address.PopulatedPlace;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.response.nomenclature.NomenclatureResponse;
import phoenix.core.customer.model.response.nomenclature.PopulatedPlaceTreeResponse;

import java.util.List;
import java.util.Optional;

public interface PopulatedPlaceRepository extends JpaRepository<PopulatedPlace, Long> {

    @Query("""
        select p
        from PopulatedPlace p
        where p.id = :id
        and p.status in :statuses
    """)
    Optional<PopulatedPlace> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses")List<NomenclatureItemStatus> statuses
    );

    @Query("select new phoenix.core.customer.model.entity.nomenclature.address.PopulatedPlace(" +
                        "p.id, " +
                        "pm, " +
                        "CONCAT(p.name, ' - ', pm.name, ' - ', pmr.name, ' - ', pmrc.name), " +
                        "p.status, " +
                        "p.defaultSelection, " +
                        "p.orderingId, " +
                        "p.systemUserId " +
                    ") " +
                    "from PopulatedPlace as p" +
                    " left join p.municipality as pm" +
                    " left join pm.region as pmr " +
                    " left join pmr.country as pmrc " +
                    " where (p.status in (:statuses))" +
                    " and (:prompt is null or (" +
                        " lower(p.name) like lower(concat('%',:prompt,'%')) or " +
                        " lower(pm.name) like lower(concat('%',:prompt,'%')) or " +
                        " lower(pmr.name) like lower(concat('%',:prompt,'%')) or " +
                        " lower(pmrc.name) like lower(concat('%',:prompt,'%'))))" +
                    " and (:municipalityId is null or pm.id = :municipalityId)" +
                    " and (:excludedItemId is null or p.id <> :excludedItemId) " +
                    " order by p.defaultSelection desc, p.orderingId asc "
    )
    Page<PopulatedPlace> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("municipalityId") Long municipalityId,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new phoenix.core.customer.model.response.nomenclature.NomenclatureResponse(" +
                        "p.id, " +
                        "CONCAT(p.name, ' - ', pm.name, ' - ', pmr.name, ' - ', pmrc.name), " +
                        "p.orderingId, " +
                        "p.defaultSelection, " +
                        "p.status" +
                    ") " +
                    "from PopulatedPlace as p" +
                    " left join p.municipality as pm" +
                    " left join pm.region as pmr " +
                    " left join pmr.country as pmrc " +
                    " where (p.status in (:statuses))" +
                    " and (:prompt is null or (" +
                        " lower(p.name) like lower(concat('%',:prompt,'%')) or " +
                        " lower(pm.name) like lower(concat('%',:prompt,'%')) or " +
                        " lower(pmr.name) like lower(concat('%',:prompt,'%')) or " +
                        " lower(pmrc.name) like lower(concat('%',:prompt,'%'))))" +
                    " and (:excludedItemId is null or p.id <> :excludedItemId) " +
                    " order by p.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    Optional<PopulatedPlace> findByDefaultSelectionTrue();

    @Query(value = "select max(p.orderingId) from PopulatedPlace p")
    Long findLastOrderingId();

    @Query(
            "select p from PopulatedPlace as p" +
                    " where p.id <> :currentId " +
                    " and (p.orderingId >= :start and p.orderingId <= :end) "
    )
    List<PopulatedPlace> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select p from PopulatedPlace as p" +
                    " where p.orderingId is not null" +
                    " order by p.name"
    )
    List<PopulatedPlace> orderByName();

    @Query(
            "select count(d.id) from District as d" +
                    " where d.populatedPlace.id = :populatedPlaceId " +
                    " and (d.status in (:statuses))"
    )
    Long getDistrictsCountByStatusAndPopulatedPlaceId(
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("populatedPlaceId") Long populatedPlaceId
    );

    @Query(
            "select count(s.id) from Street as s" +
                    " where s.populatedPlace.id = :populatedPlaceId " +
                    " and (s.status in (:statuses))"
    )
    Long getStreetsCountByStatusAndPopulatedPlaceId(
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("populatedPlaceId") Long populatedPlaceId
    );

    @Query(
            "select count(r.id) from ResidentialArea as r" +
                    " where r.populatedPlace.id = :populatedPlaceId " +
                    " and (r.status in (:statuses))"
    )
    Long getResidentialAreasCountByStatusAndPopulatedPlaceId(
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("populatedPlaceId") Long populatedPlaceId
    );

    @Query(
            "select count(z.id) from ZipCode as z" +
                    " where z.populatedPlace.id = :populatedPlaceId " +
                    " and (z.status in (:statuses))"
    )
    Long getZipCodesCountByStatusAndPopulatedPlaceId(
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("populatedPlaceId") Long populatedPlaceId
    );

    @Query(
            "select new phoenix.core.customer.model.response.nomenclature.PopulatedPlaceTreeResponse(" +
                        "pm.id, " +
                        "pm.name, " +
                        "pmr.id, " +
                        "pmr.name, " +
                        "pmrc.id, " +
                        "pmrc.name" +
                    ") " +
                    " from PopulatedPlace as p" +
                    " left join p.municipality as pm" +
                    " left join pm.region as pmr " +
                    " left join pmr.country as pmrc " +
                    " where p.id = :populatedPlaceId "
    )
    PopulatedPlaceTreeResponse getPopulatedPlaceTreeView(@Param("populatedPlaceId") Long id);

    @Query(
            """
            select count(1) from PopulatedPlace pp
                where pp.id = :id
                and (exists (select 1 from CustomerCommunications cc, CustomerDetails cd, Customer c
                        where cc.populatedPlaceId = pp.id
                        and cd.id = cc.customerDetailsId
                        and c.id = cd.customerId
                        and cc.status = 'ACTIVE' and c.status = 'ACTIVE'
                )
                or exists( select 1 from CustomerDetails cd, Customer c
                        where pp.id = cd.populatedPlaceId
                        and c.id = cd.customerId
                        and c.status = 'ACTIVE'
                ))
            """
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );
}
