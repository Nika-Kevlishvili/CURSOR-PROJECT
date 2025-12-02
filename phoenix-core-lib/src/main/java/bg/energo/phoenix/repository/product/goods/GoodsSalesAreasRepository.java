package bg.energo.phoenix.repository.product.goods;

import bg.energo.phoenix.model.entity.product.goods.GoodsSalesAreas;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GoodsSalesAreasRepository extends JpaRepository<GoodsSalesAreas, Long> {

    List<GoodsSalesAreas> findByGoodsDetails_IdAndStatus(Long id, GoodsSubObjectStatus status);

    @Modifying
    @Query("delete from GoodsSalesAreas gsa where gsa.goodsDetails.id = :goodsDetailsId and gsa.id not in(:ids)")
    void deleteAllOtherThanIdsList(Long goodsDetailsId, List<Long> ids);

    @Modifying
    @Query("delete from GoodsSalesAreas gsa where gsa.goodsDetails.id = :goodsDetailsId")
    void deleteAllByDetailsId(Long goodsDetailsId);


    Optional<GoodsSalesAreas> findByIdAndStatus(Long id, GoodsSubObjectStatus status);

    @Query("""
                select gsa
                from GoodsSalesAreas gsa
                where gsa.status in (:statuses)
                and gsa.goodsDetails.id = :goodsDetailsId
                and gsa.salesArea.status in (:nomenclatureItemStatuses)
            """)
    List<GoodsSalesAreas> findByGoodsDetailsIdAndStatusInAndWithActiveSubObjects(
            @Param("goodsDetailsId") Long goodsDetailsId,
            @Param("statuses") List<GoodsSubObjectStatus> statuses,
            @Param("nomenclatureItemStatuses") List<NomenclatureItemStatus> nomenclatureItemStatuses
    );

    @Query("""
            select gsa from GoodsSalesAreas gsa where gsa.salesArea.id = :id and gsa.goodsDetails.id = :detailsId and gsa.status = :status
            """)
    Optional<GoodsSalesAreas> findBySalesAreaIdAndGoodsDetailsIdAndStatus(Long id, Long detailsId, GoodsSubObjectStatus status);
}
