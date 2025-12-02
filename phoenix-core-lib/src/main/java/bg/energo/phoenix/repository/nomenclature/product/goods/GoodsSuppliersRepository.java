package bg.energo.phoenix.repository.nomenclature.product.goods;


import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsSuppliers;
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
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsSuppliersRepository extends JpaRepository<GoodsSuppliers, Long> {
    @Query(
            "select gs from GoodsSuppliers as gs" +
            " where (:prompt is null or lower(gs.name) like :prompt" +
            " or (lower(gs.identifier) like lower(concat('%',:prompt,'%') )))" +
            " and (gs.status in (:statuses))" +
            " and (:excludedItemId is null or gs.id <> :excludedItemId) " +
            " order by gs.defaultSelection desc, gs.orderingId asc"
    )
    Page<GoodsSuppliers> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(gs.id, concat(gs.name, ' - (', coalesce(gs.identifier, ''), ')'), gs.orderingId, gs.defaultSelection, gs.status) " +
            "from GoodsSuppliers as gs" +
            " where (:prompt is null or lower(gs.name) like :prompt" +
            " or (lower(gs.identifier) like lower(concat('%',:prompt,'%') )))" +
            " and (:excludedItemId is null or gs.id <> :excludedItemId) " +
            " and (gs.status in (:statuses))" +
            " order by gs.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    Optional<GoodsSuppliers> findByDefaultSelectionTrue();

    @Query("select max(gs.orderingId) from GoodsSuppliers gs")
    Long findLastOrderingId();

    @Query(
            "select gs from GoodsSuppliers as gs" +
            " where gs.id <> :currentId " +
            " and (gs.orderingId >= :start and gs.orderingId <= :end) "
    )
    List<GoodsSuppliers> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query("""
            select gs from GoodsSuppliers as gs
                where gs.orderingId is not null
                order by gs.name
            """
    )
    List<GoodsSuppliers> orderByName();

    @Query("""
             select gs from GoodsSuppliers gs
             where gs.id = :id and gs.status in(:statuses)
            """
    )
    Optional<GoodsSuppliers> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query("""
            select gs from GoodsSuppliers gs
            where gs.name like :name
            and gs.status in(:statuses)
            """)
    List<GoodsSuppliers> findByNameAndStatuses(String name, List<NomenclatureItemStatus> statuses);

    @Query("""
            select count(1) from  GoodsSuppliers sa
                where sa.id = :id
                and
                (exists
                (select 1 from Goods g
                  join GoodsDetails gd on gd.goods.id = g.id
                   and gd.goodsSuppliers.id = sa.id
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
                        from GoodsSuppliers g
                        where g.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
