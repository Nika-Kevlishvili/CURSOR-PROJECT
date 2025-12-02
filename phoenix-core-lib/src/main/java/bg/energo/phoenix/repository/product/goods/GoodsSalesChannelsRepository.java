package bg.energo.phoenix.repository.product.goods;

import bg.energo.phoenix.model.entity.product.goods.GoodsSalesChannels;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsSubObjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GoodsSalesChannelsRepository extends JpaRepository<GoodsSalesChannels, Long> {
    List<GoodsSalesChannels> findByGoodsDetails_IdAndStatus(Long id, GoodsSubObjectStatus status);

    @Modifying
    @Query("delete from GoodsSalesChannels gsc where gsc.goodsDetails.id = :goodsDetailsId and gsc.id not in(:ids)")
    void deleteAllOtherThanIdsList(Long goodsDetailsId, List<Long> ids);

    @Modifying
    @Query("delete from GoodsSalesChannels gsc where gsc.goodsDetails.id = :goodsDetailsId")
    void deleteAllByDetailsId(Long goodsDetailsId);

    Optional<GoodsSalesChannels> findByIdAndStatus(Long id, GoodsSubObjectStatus status);

    @Query("""
                select gsc
                from GoodsSalesChannels gsc
                where gsc.status in (:statuses)
                and gsc.goodsDetails.id = :goodsDetailsId
                and gsc.salesChannel.status in (:nomenclatureItemStatuses)
            """)
    List<GoodsSalesChannels> findByGoodsDetailsIdAndStatusInAndWithActiveSubObjects(
            @Param("goodsDetailsId") Long goodsDetailsId,
            @Param("statuses") List<GoodsSubObjectStatus> statuses,
            @Param("nomenclatureItemStatuses") List<NomenclatureItemStatus> nomenclatureItemStatuses
    );

    @Query("""
            select gsc from GoodsSalesChannels gsc where  gsc.salesChannel.id = :salesChannelsId and gsc.goodsDetails.id = :id and gsc.status = :goodsSubObjectStatus
            """)
    Optional<GoodsSalesChannels> findBySalesChannelIdAndGoodsDetailsIdAndStatus(Long salesChannelsId, Long id, GoodsSubObjectStatus goodsSubObjectStatus);
}
