package phoenix.core.customer.repository.nomenclature.address;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import phoenix.core.customer.model.entity.nomenclature.address.District;
import phoenix.core.customer.model.enums.nomenclature.NomenclatureItemStatus;
import phoenix.core.customer.model.response.nomenclature.DistrictTreeResponse;
import phoenix.core.customer.model.response.nomenclature.NomenclatureResponse;

import java.util.List;
import java.util.Optional;

public interface DistrictRepository extends JpaRepository<District, Long> {

    @Query("""
        select d
        from District d
        where d.id = :id
        and d.populatedPlace.id = :populatedPlaceId
        and d.status in :statuses
    """)
    Optional<District> findByIdAndPopulatedPlaceIdAndStatus(
            @Param("id") Long id,
            @Param("populatedPlaceId") Long populatedPlaceId,
            @Param("statuses")List<NomenclatureItemStatus> statuses
    );

    @Query("select d from District as d" +
                    " left join d.populatedPlace as dp" +
                    " left join dp.municipality as dpm" +
                    " left join dpm.region as dpmr " +
                    " left join dpmr.country as dpmrc " +
                    " where (d.status in (:statuses))" +
                    " and (:prompt is null or (" +
                        " lower(d.name) like lower(concat('%',:prompt,'%')) or " +
                        " lower(dp.name) like lower(concat('%',:prompt,'%')) or " +
                        " lower(dpm.name) like lower(concat('%',:prompt,'%')) or " +
                        " lower(dpmr.name) like lower(concat('%',:prompt,'%')) or " +
                        " lower(dpmrc.name) like lower(concat('%',:prompt,'%'))) )" +
                    " and (:populatedPlaceId is null or dp.id = :populatedPlaceId)" +
                    " and (:excludedItemId is null or d.id <> :excludedItemId) " +
                    " order by d.orderingId asc "
    )
    Page<District> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("populatedPlaceId") Long populatedPlaceId,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new phoenix.core.customer.model.response.nomenclature.NomenclatureResponse(" +
                        "d.id, " +
                        "CONCAT(d.name, ' - ', dp.name, ' - ', dpm.name, ' - ', dpmr.name, ' - ', dpmrc.name), " +
                        "d.orderingId, " +
                        "d.defaultSelection, " +
                        "d.status" +
                    ") " +
                    "from District as d" +
                    " left join d.populatedPlace as dp" +
                    " left join dp.municipality as dpm" +
                    " left join dpm.region as dpmr " +
                    " left join dpmr.country as dpmrc " +
                    " where (d.status in (:statuses))" +
                    " and (:prompt is null or (" +
                        " lower(d.name) like lower(concat('%',:prompt,'%')) or " +
                        " lower(dp.name) like lower(concat('%',:prompt,'%')) or " +
                        " lower(dpm.name) like lower(concat('%',:prompt,'%')) or " +
                        " lower(dpmr.name) like lower(concat('%',:prompt,'%')) or " +
                        " lower(dpmrc.name) like lower(concat('%',:prompt,'%'))))" +
                    " and (:excludedItemId is null or d.id <> :excludedItemId) " +
                    " order by d.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    Optional<District> findByDefaultSelectionTrue();

    @Query(value = "select max(d.orderingId) from District d")
    Long findLastOrderingId();

    @Query(
            "select d from District as d" +
                    " where d.id <> :currentId " +
                    " and (d.orderingId >= :start and d.orderingId <= :end) "
    )
    List<District> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select d from District as d" +
                    " where d.orderingId is not null" +
                    " order by d.name"
    )
    List<District> orderByName();

    @Query(
            "select new phoenix.core.customer.model.response.nomenclature.DistrictTreeResponse(" +
                        "dp.id, " +
                        "dp.name, " +
                        "dpm.id, " +
                        "dpm.name, " +
                        "dpmr.id, " +
                        "dpmr.name, " +
                        "dpmrc.id, " +
                        "dpmrc.name" +
                    ") " +
                    " from District as d" +
                    " left join d.populatedPlace as dp" +
                    " left join dp.municipality as dpm" +
                    " left join dpm.region as dpmr " +
                    " left join dpmr.country as dpmrc " +
                    " where d.id = :districtId"
    )
    DistrictTreeResponse getDistrictTreeView(@Param("districtId") Long districtId);

    @Query(
            """
            select count(1) from District district
                where district.id = :id
                and (exists (select 1 from CustomerDetails cd, Customer c
                        where cd.districtId = :id
                        and c.id = cd.customerId
                        and c.status = 'ACTIVE')
                or exists (select 1 from CustomerCommunications cc, CustomerDetails cd, Customer c
                        where cc.districtId = :id
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
