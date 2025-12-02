package bg.energo.phoenix.repository.product.goods;

import bg.energo.phoenix.model.entity.product.goods.GoodsDetails;
import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
import bg.energo.phoenix.model.response.contract.order.goods.GoodsSearchShortResponse;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse;
import bg.energo.phoenix.model.response.goods.GoodsVersionsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GoodsDetailsRepository extends JpaRepository<GoodsDetails, Long> {
    Optional<GoodsDetails> findByGoodsIdAndVersionId(Long id, Long versionId);

    Optional<GoodsDetails> findFirstByGoodsId(@Param("id") Long id, Sort sort);

    @Query("select new bg.energo.phoenix.model.response.goods.GoodsVersionsResponse(gd.versionId,gd.status,gd.createDate) from GoodsDetails gd " +
            "where gd.goods.id = :id and gd.status in (:statuses) order by gd.versionId asc")
    List<GoodsVersionsResponse> getVersions(@Param("id") Long id, @Param("statuses") List<GoodsDetailStatus> statuses);

    @Query(value = "SELECT max(gd.versionId) FROM GoodsDetails gd where gd.id = :id")
    Optional<Long> findMaxVersionByGoodsDetails(Long id);

    @Query("""
            select new bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionMiddleResponse(gd.id,gd.versionId,gd.createDate)
            from GoodsDetails gd
            where gd.goods.id = :goodsId
            order by gd.createDate ASC
            """)
    List<CopyDomainWithVersionMiddleResponse> findByGoodsId(
            @Param("goodsId") Long goodsId);

    @Query("""
            select gd from GoodsDetails gd
            where gd.id = :goodsDetailsId
            and gd.status = 'ACTIVE'
            and gd.goods.goodsStatusEnum = 'ACTIVE'
            """)
    Optional<GoodsDetails> findActiveByGoodsDetailsId(Long goodsDetailsId);

    @Query("""
            select gd from GoodsDetails gd
            where gd.id = :goodsDetailsId
            and gd.status in (:statuses)
            and gd.goods.goodsStatusEnum = 'ACTIVE'
            """)
    Optional<GoodsDetails> findActiveByGoodsDetailsIdAndStatuses(@Param("goodsDetailsId") Long goodsDetailsId, @Param("statuses") List<GoodsDetailStatus> statuses);

    @Query("""
            select new bg.energo.phoenix.model.response.contract.order.goods.GoodsSearchShortResponse(gd) from GoodsDetails gd
            where gd.status = 'ACTIVE'
            and gd.goods.goodsStatusEnum = 'ACTIVE'
            and (
                    coalesce(:promptForNull,'0') = '0' or
                    (
                        lower(gd.name) like(:prompt)
                        or text(gd.goods.id) like (:prompt)
                    )
                )
            order by gd.goods.id, gd.versionId
            """)
    Page<GoodsSearchShortResponse> searchGoodsForGoodsOrder( @Param("prompt")String prompt,@Param("promptForNull")String promptForNull,Pageable pageRequest);

    @Query("""
            select DISTINCT gd from GoodsDetails gd
                             join GoodsOrderGoods gog on gog.goodsDetailsId = gd.id
                             join GoodsOrder o on gog.orderId  = o.id
                            where gd.id =:id
                              and o.status = 'ACTIVE'
            """)
    List<GoodsDetails> checkForBoundObjects(Long id);
}
