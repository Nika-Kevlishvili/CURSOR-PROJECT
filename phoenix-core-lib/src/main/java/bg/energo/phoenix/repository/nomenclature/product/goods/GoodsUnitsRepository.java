package bg.energo.phoenix.repository.nomenclature.product.goods;

import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsUnits;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
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

public interface GoodsUnitsRepository extends JpaRepository<GoodsUnits, Long> {
    @Query(
            "select gu from GoodsUnits as gu" +
                    " where (:prompt is null or lower(gu.name) like :prompt)" +
                    " and (gu.status in (:statuses))" +
                    " and (:excludedItemId is null or gu.id <> :excludedItemId) " +
                    " order by gu.isDefault desc, gu.orderingId asc"
    )
    Page<GoodsUnits> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(gu.id, gu.name, gu.orderingId, gu.isDefault, gu.status) " +
                    "from GoodsUnits as gu" +
                    " where (:prompt is null or lower(gu.name) like :prompt)" +
                    " and (gu.status in (:statuses))" +
                    " order by gu.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            Pageable pageable
    );

    Optional<GoodsUnits> findByIsDefaultTrue();

    @Query("select max(gu.orderingId) from GoodsUnits gu")
    Long findLastOrderingId();

    @Query(
            "select gu from GoodsUnits as gu" +
                    " where gu.id <> :currentId " +
                    " and (gu.orderingId >= :start and gu.orderingId <= :end) "
    )
    List<GoodsUnits> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query(
            "select gu from GoodsUnits as gu" +
                    " where gu.orderingId is not null" +
                    " order by gu.name"
    )
    List<GoodsUnits> orderByName();

    @Query(
            """
                                select gu from GoodsUnits gu
                                where gu.id = :id
                                and gu.status in :statuses
                    """
    )
    Optional<GoodsUnits> findByIdAndStatus(@Param("id") Long id, @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query(
            """
                    select count(1) from GoodsUnits as gu
                    where lower(gu.name) = :name and gu.status in :statuses
                    """
    )
    Integer getExistingRecordsCountByName(@Param("name") String name, @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query("""
                select count(1) from  GoodsUnits sa
                    where sa.id = :id
                    and
                    ( exists
                    (select 1 from Goods g
                      join GoodsDetails gd on gd.goods.id = g.id
                       and gd.goodsUnits.id = sa.id
                      where
                        gd.status in (:goodsDetailStatuses)
                        and g.goodsStatusEnum = 'ACTIVE'))
           """)
    Long activeConnectionCount(@Param("id") Long id,
                               @Param("goodsDetailStatuses") List<GoodsDetailStatus> goodsDetailStatuses);

    boolean existsByIdAndStatusIn(Long id, List<NomenclatureItemStatus> statuses);

    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.activity.ActivityNomenclatureResponse(
                            g.id,
                            g.name
                        )
                        from GoodsUnits g
                        where g.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

    Optional<GoodsUnits> findByNameAndStatusIn(String name, List<NomenclatureItemStatus> statuses);
}
