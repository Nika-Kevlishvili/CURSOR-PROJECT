package bg.energo.phoenix.repository.nomenclature.customer;

import bg.energo.phoenix.model.entity.nomenclature.customer.GccConnectionType;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse;
import bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface GccConnectionTypeRepository extends JpaRepository<GccConnectionType,Long> {

    @Query(
            "select c from GccConnectionType as c" +
                    " where (:prompt is null or lower(c.name) like :prompt)" +
                    " and (c.status in (:statuses))" +
                    " and (:excludedItemId is null or c.id <> :excludedItemId) " +
                    " order by c.defaultSelection desc, c.orderingId asc"
    )
    Page<GccConnectionType> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );
    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(c.id, c.name, c.orderingId, c.defaultSelection, c.status) " +
                    "from GccConnectionType as c" +
                    " where (:prompt is null or lower(c.name) like :prompt)" +
                    " and (c.status in (:statuses))" +
                    " order by c.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );
    Optional<GccConnectionType> findByDefaultSelectionTrue();
    @Query("select max(c.orderingId) from GccConnectionType c")
    Long findLastOrderingId();
    @Query(
            "select c from GccConnectionType as c" +
                    " where c.id <> :currentId " +
                    " and (c.orderingId >= :start and c.orderingId <= :end) "
    )
    List<GccConnectionType> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select c from GccConnectionType as c" +
                    " where c.orderingId is not null" +
                    " order by c.name"
    )
    List<GccConnectionType> orderByName();

    @Query(
            """
            select count(1) from GccConnectionType gct
                where gct.id = :id
                and exists (select 1 from ConnectedGroup cg
                    where cg.gccConnectionType.id = :id
                    and cg.status = 'ACTIVE')
            """
    )
    Long getActiveConnectionsCount(
            @Param("id") Long id
    );

    @Query(
            """
                                select c from GccConnectionType c
                                where c.id = :id
                                and c.status in :statuses
                    """
    )
    Optional<GccConnectionType> findByIdAndStatus(@Param("id") Long id,@Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query(
            """
            select count(c.id) from GccConnectionType c
                where lower(c.name) = lower(:name)
                and c.status in (:statuses)
            """
    )
    Long countGccConnectionTypeByStatusAndName(
            @Param("name") String name,
            @Param("statuses") List<NomenclatureItemStatus> statuses
    );

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            g.id,
                            g.name
                        )
                        from GccConnectionType g
                        where g.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
