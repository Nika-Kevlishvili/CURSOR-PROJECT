package bg.energo.phoenix.repository.nomenclature.product.goods;

import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsGroups;
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
public interface GoodsGroupsRepository extends JpaRepository<GoodsGroups, Long> {
    @Query(
            "select gg from GoodsGroups as gg" +
                    " where (:prompt is null or lower(gg.name) like :prompt " +
                    " or (:prompt is null or lower(gg.nameTransliterated) like :prompt))" +
                    " and (gg.status in (:statuses))" +
                    " and (:excludedItemId is null or gg.id <> :excludedItemId) " +
                    " order by gg.defaultSelection desc, gg.orderingId asc"
    )
    Page<GoodsGroups> filter(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    @Query(
            "select new bg.energo.phoenix.model.response.nomenclature.NomenclatureResponse(gg.id, gg.name, gg.orderingId, gg.defaultSelection, gg.status) " +
                    "from GoodsGroups as gg" +
                    " where (:prompt is null or lower(gg.name) like :prompt" +
                    " or (:prompt is null or lower(gg.nameTransliterated) like :prompt))" +
                    " and (:excludedItemId is null or gg.id <> :excludedItemId) " +
                    " and (gg.status in (:statuses))" +
                    " order by gg.orderingId asc"
    )
    Page<NomenclatureResponse> filterNomenclature(
            @Param("prompt") String prompt,
            @Param("statuses") List<NomenclatureItemStatus> statuses,
            @Param("excludedItemId") Long excludedItemId,
            Pageable pageable
    );

    Optional<GoodsGroups> findByDefaultSelectionTrue();

    @Query("select max(gg.orderingId) from GoodsGroups gg")
    Long findLastOrderingId();

    @Query("""
            select gg from GoodsGroups gg
            where gg.name like :name
            and gg.status in(:statuses)
           """)
    List<GoodsGroups> findByNameAndStatuses(@Param("name") String name, @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query(
            "select gg from GoodsGroups as gg" +
                    " where gg.id <> :currentId " +
                    " and (gg.orderingId >= :start and gg.orderingId <= :end) "
    )
    List<GoodsGroups> findInOrderingIdRange(
            @Param("start") Long start,
            @Param("end") Long end,
            @Param("currentId") Long currentId,
            Sort sort
    );

    @Query("""
            select gg from GoodsGroups as gg
                where gg.orderingId is not null
                order by gg.name
            """
    )
    List<GoodsGroups> orderByName();

    @Query("""
             select gg from GoodsGroups gg
             where gg.id = :id and gg.status in(:statuses)
            """
    )
    Optional<GoodsGroups> findByIdAndStatus(
            @Param("id") Long id,
            @Param("statuses") List<NomenclatureItemStatus> statuses);

    @Query("""
            select count(1) from  GoodsGroups sa
            where sa.id = :id
            and
            ( exists
            (select 1 from Goods g
              join GoodsDetails gd on gd.goods.id = g.id
               and gd.goodsGroups.id = sa.id
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
                        from GoodsGroups g
                        where g.id in :ids
                    """
    )
    List<ActivityNomenclatureResponse> findByIdIn(List<Long> ids);

}
