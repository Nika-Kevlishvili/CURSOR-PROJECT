package bg.energo.phoenix.repository.product.goods;

import bg.energo.phoenix.model.entity.product.goods.GoodsSegments;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GoodsSegmentsRepository extends JpaRepository<GoodsSegments, Long> {
    List<GoodsSegments> findByGoodsDetails_IdAndStatus(Long id, GoodsSubObjectStatus status);

    @Modifying
    @Query("delete from GoodsSegments gs where gs.goodsDetails.id = :goodsDetailsId and gs.id not in(:ids)")
    void deleteAllOtherThanIdsList(Long goodsDetailsId, List<Long> ids);

    @Modifying
    @Query("delete from GoodsSegments gs where gs.goodsDetails.id = :goodsDetailsId")
    void deleteAllByDetailsId(Long goodsDetailsId);

    Optional<GoodsSegments> findByIdAndStatusIn(Long id, List<GoodsSubObjectStatus> statuses);

    @Query("""
                select gs
                from GoodsSegments gs
                where gs.status in (:statuses)
                and gs.goodsDetails.id = :goodsDetailsId
                and gs.segment.status in (:nomenclatureItemStatuses)
            """)
    List<GoodsSegments> findByGoodsDetailsIdAndStatusInAndWithActiveSubObjects(
            @Param("goodsDetailsId") Long goodsDetailsId,
            @Param("statuses") List<GoodsSubObjectStatus> statuses,
            @Param("nomenclatureItemStatuses") List<NomenclatureItemStatus> nomenclatureItemStatuses
    );

    @Query("""
            select gs from GoodsSegments gs where gs.goodsDetails.id = :detailsId and gs.segment.id = :id and gs.status = :status
            """)
    Optional<GoodsSegments> findByGoodsDetailsIdAndSegmentIdAndStatus(Long detailsId, Long id, GoodsSubObjectStatus status);
}
