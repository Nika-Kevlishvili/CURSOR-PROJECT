package bg.energo.phoenix.repository.product.goods;

import bg.energo.phoenix.model.entity.product.goods.Goods;
import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsStatus;
import bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse;
import bg.energo.phoenix.model.response.product.goods.GoodsListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GoodsRepository extends JpaRepository<Goods, Long> {

    /**
     * Retrieves a paginated list of goods based on the provided search criteria.
     *
     * @param searchBy              The field in which the search term should be searched.
     * @param prompt                The search term to look for.
     * @param goodsDetailStatuses   The list of statuses of the goods details to include in the search results.
     * @param groupIds              The list of group IDs to include in the search results.
     * @param supplierIds           The list of supplier IDs to include in the search results.
     * @param salesChannelsIds      The list of sales channel IDs to include in the search results.
     * @param segmentIds            The list of segment IDs to include in the search results.
     * @param goodsStatuses         The list of goods statuses to include in the search results.
     * @param salesChannelDirection The sort direction for the sales channels field, if used for sorting the search results.
     * @param pageable              The page and size parameters for pagination, as well as the sort order and field for sorting the search results.
     * @return A paginated list of {@link GoodsListResponse} objects matching the provided search criteria.
     */
    @Query(
            value = """
                        select new bg.energo.phoenix.model.response.product.goods.GoodsListResponse(
                            goods.id,
                            goodsDetails.name,
                            goodsGroups.name,
                            goodsSuppliers.name,
                            goodsDetails.status,
                            goodsDetails.price,
                            goodsDetails.goodsUnits.name,
                            (case when :salesChannelDirection = 'ASC' then viewGoodsSalesChannels.salesChannelsName
                                  when :salesChannelDirection = 'DESC' then viewGoodsSalesChannels.salesChannelsNameDesc
                                  else viewGoodsSalesChannels.salesChannelsName end),
                            goods.goodsStatusEnum,
                            goodsDetails.id,
                            goods.createDate,
                            goodsDetails.globalSalesChannel
                        )
                        from Goods as goods
                        join GoodsDetails as goodsDetails on goods.lastGoodsDetailsId = goodsDetails.id
                        join GoodsGroups as goodsGroups on goodsDetails.goodsGroups.id = goodsGroups.id
                        join GoodsSuppliers as goodsSuppliers on goodsDetails.goodsSuppliers.id = goodsSuppliers.id
                        join GoodsUnits as goodsUnits on goodsDetails.goodsUnits.id = goodsUnits.id
                        left join VwGoodsSalesChannels as viewGoodsSalesChannels on viewGoodsSalesChannels.goodsDetailsId = goodsDetails.id
                            where goods.id in (
                                select goods.id from Goods goods
                                join GoodsDetails goodsDetails on goodsDetails.goods.id = goods.id
                                    where
                                        (coalesce(:groupIds, '0') = '0' or goodsDetails.goodsGroups.id in :groupIds)
                                        and (coalesce(:goodsStatuses, '0') = '0' or goods.goodsStatusEnum in :goodsStatuses)
                                        and (coalesce(:excludeOldVersion,'false') = 'false' or (:excludeOldVersion = 'true' and goodsDetails.id = goods.lastGoodsDetailsId))
                                        and (coalesce(:goodsDetailStatuses, '0') = '0' or goodsDetails.status in :goodsDetailStatuses)
                                        and (coalesce(:supplierIds, '0') = '0' or goodsDetails.goodsSuppliers.id in :supplierIds)
                                        and (
                                            (coalesce(:salesChannelsIds, '0') <> '0' and :globalSalesChannel is not null 
                                                and exists(select 1 from GoodsSalesChannels goodsSalesChannels
                                                    where goodsSalesChannels.goodsDetails.id = goodsDetails.id
                                                    and goodsSalesChannels.salesChannel.id in :salesChannelsIds
                                                    and goodsSalesChannels.status = 'ACTIVE')
                                                or goodsDetails.globalSalesChannel = :globalSalesChannel)
                                            or (coalesce(:salesChannelsIds, '0') <> '0' and :globalSalesChannel is null 
                                                and exists(select 1 from GoodsSalesChannels goodsSalesChannels
                                                    where goodsSalesChannels.goodsDetails.id = goodsDetails.id
                                                    and goodsSalesChannels.salesChannel.id in :salesChannelsIds
                                                    and goodsSalesChannels.status = 'ACTIVE'))
                                            or (coalesce(:salesChannelsIds, '0') = '0' and :globalSalesChannel is not null and goodsDetails.globalSalesChannel = :globalSalesChannel)
                                            or (coalesce(:salesChannelsIds, '0') = '0' and :globalSalesChannel is null )
                                        )
                                        and (
                                            (coalesce(:segmentIds, '0') <> '0' and :globalSegment is not null 
                                                and exists(select 1 from GoodsSegments goodsSegments
                                                    where goodsSegments.goodsDetails.id = goodsDetails.id
                                                    and goodsSegments.segment.id in :segmentIds
                                                    and goodsSegments.status = 'ACTIVE') or goodsDetails.globalSegment = :globalSegment)
                                            or (coalesce(:segmentIds, '0') <> '0' and :globalSegment is null 
                                                and exists(select 1 from GoodsSegments goodsSegments
                                                    where goodsSegments.goodsDetails.id = goodsDetails.id
                                                    and goodsSegments.segment.id in :segmentIds
                                                    and goodsSegments.status = 'ACTIVE'))
                                            or (coalesce(:segmentIds, '0') = '0' and :globalSegment is not null and goodsDetails.globalSegment = :globalSegment)
                                            or (coalesce(:segmentIds, '0') = '0' and :globalSegment is null )
                                        )
                                    and (:searchBy is null
                                        or (:searchBy = 'ALL' and (lower(goodsDetails.name) like :prompt
                                                or lower(goodsDetails.manufacturerCodeNumber) like :prompt
                                                or lower(goodsDetails.otherSystemConnectionCode) like :prompt
                                                or exists (select 1 from GoodsGroups goodsGroups where goodsDetails.goodsGroups.id = goodsGroups.id and lower(goodsGroups.name) like :prompt)
                                                or exists (select 1 from GoodsSuppliers goodsSuppliers where goodsDetails.goodsSuppliers.id = goodsSuppliers.id and lower(goodsSuppliers.name) like :prompt))
                                            )
                                        or (
                                            (:searchBy = 'NAME' and lower(goodsDetails.name) like :prompt)
                                            or (:searchBy = 'GROUP_NAME' and exists (select 1 from GoodsGroups goodsGroups where goodsDetails.goodsGroups.id = goodsGroups.id and lower(goodsGroups.name) like :prompt))
                                            or (:searchBy = 'GOODS_SUPPLIER' and exists (select 1 from GoodsSuppliers goodsSuppliers where goodsDetails.goodsSuppliers.id = goodsSuppliers.id and lower(goodsSuppliers.name) like :prompt))
                                            or (:searchBy = 'MANUFACTURER_CODE' and lower(goodsDetails.manufacturerCodeNumber) like :prompt)
                                            or (:searchBy = 'OTHER_SYS_CONNECTION_CODE' and lower(goodsDetails.otherSystemConnectionCode) like :prompt)                        \s
                                        )
                                    )
                            )
                    """,
            countQuery = """
                    select count (1)
                        from Goods as goods
                        join GoodsDetails as goodsDetails on goods.lastGoodsDetailsId = goodsDetails.id
                        join GoodsGroups as goodsGroups on goodsDetails.goodsGroups.id = goodsGroups.id
                        join GoodsSuppliers as goodsSuppliers on goodsDetails.goodsSuppliers.id = goodsSuppliers.id
                        join GoodsUnits as goodsUnits on goodsDetails.goodsUnits.id = goodsUnits.id
                        left join VwGoodsSalesChannels as viewGoodsSalesChannels on viewGoodsSalesChannels.goodsDetailsId = goodsDetails.id
                            where goods.id in (
                                select goods.id from Goods goods
                                join GoodsDetails goodsDetails on goodsDetails.goods.id = goods.id
                                    where
                                        (coalesce(:groupIds, '0') = '0' or goodsDetails.goodsGroups.id in :groupIds)
                                        and (coalesce(:goodsStatuses, '0') = '0' or goods.goodsStatusEnum in :goodsStatuses)
                                        and (coalesce(:excludeOldVersion,'false') = 'false' or (:excludeOldVersion = 'true' and goodsDetails.id = goods.lastGoodsDetailsId))
                                        and (coalesce(:goodsDetailStatuses, '0') = '0' or goodsDetails.status in :goodsDetailStatuses)
                                        and (coalesce(:supplierIds, '0') = '0' or goodsDetails.goodsSuppliers.id in :supplierIds)
                                        and (
                                            (coalesce(:salesChannelsIds, '0') <> '0' and :globalSalesChannel is not null 
                                                and exists(select 1 from GoodsSalesChannels goodsSalesChannels
                                                    where goodsSalesChannels.goodsDetails.id = goodsDetails.id
                                                    and goodsSalesChannels.salesChannel.id in :salesChannelsIds
                                                    and goodsSalesChannels.status = 'ACTIVE')
                                                or goodsDetails.globalSalesChannel = :globalSalesChannel)
                                            or (coalesce(:salesChannelsIds, '0') <> '0' and :globalSalesChannel is null 
                                                and exists(select 1 from GoodsSalesChannels goodsSalesChannels
                                                    where goodsSalesChannels.goodsDetails.id = goodsDetails.id
                                                    and goodsSalesChannels.salesChannel.id in :salesChannelsIds
                                                    and goodsSalesChannels.status = 'ACTIVE'))
                                            or (coalesce(:salesChannelsIds, '0') = '0' and :globalSalesChannel is not null and goodsDetails.globalSalesChannel = :globalSalesChannel)
                                            or (coalesce(:salesChannelsIds, '0') = '0' and :globalSalesChannel is null )
                                        )
                                        and (
                                            (coalesce(:segmentIds, '0') <> '0' and :globalSegment is not null 
                                                and exists(select 1 from GoodsSegments goodsSegments
                                                    where goodsSegments.goodsDetails.id = goodsDetails.id
                                                    and goodsSegments.segment.id in :segmentIds
                                                    and goodsSegments.status = 'ACTIVE') or goodsDetails.globalSegment = :globalSegment)
                                            or (coalesce(:segmentIds, '0') <> '0' and :globalSegment is null 
                                                and exists(select 1 from GoodsSegments goodsSegments
                                                    where goodsSegments.goodsDetails.id = goodsDetails.id
                                                    and goodsSegments.segment.id in :segmentIds
                                                    and goodsSegments.status = 'ACTIVE'))
                                            or (coalesce(:segmentIds, '0') = '0' and :globalSegment is not null and goodsDetails.globalSegment = :globalSegment)
                                            or (coalesce(:segmentIds, '0') = '0' and :globalSegment is null )
                                        )
                                    and (:searchBy is null
                                        or (:searchBy = 'ALL' and (lower(goodsDetails.name) like :prompt
                                                or lower(goodsDetails.manufacturerCodeNumber) like :prompt
                                                or lower(goodsDetails.otherSystemConnectionCode) like :prompt
                                                or exists (select 1 from GoodsGroups goodsGroups where goodsDetails.goodsGroups.id = goodsGroups.id and lower(goodsGroups.name) like :prompt)
                                                or exists (select 1 from GoodsSuppliers goodsSuppliers where goodsDetails.goodsSuppliers.id = goodsSuppliers.id and lower(goodsSuppliers.name) like :prompt))
                                            )
                                        or (
                                            (:searchBy = 'NAME' and lower(goodsDetails.name) like :prompt)
                                            or (:searchBy = 'GROUP_NAME' and exists (select 1 from GoodsGroups goodsGroups where goodsDetails.goodsGroups.id = goodsGroups.id and lower(goodsGroups.name) like :prompt))
                                            or (:searchBy = 'GOODS_SUPPLIER' and exists (select 1 from GoodsSuppliers goodsSuppliers where goodsDetails.goodsSuppliers.id = goodsSuppliers.id and lower(goodsSuppliers.name) like :prompt))
                                            or (:searchBy = 'MANUFACTURER_CODE' and lower(goodsDetails.manufacturerCodeNumber) like :prompt)
                                            or (:searchBy = 'OTHER_SYS_CONNECTION_CODE' and lower(goodsDetails.otherSystemConnectionCode) like :prompt)                        \s
                                        )
                                    )
                            )
                    """
    )
    Page<GoodsListResponse> findAll(
            @Param("searchBy") String searchBy,
            @Param("prompt") String prompt,
            @Param("goodsDetailStatuses") List<GoodsDetailStatus> goodsDetailStatuses,
            @Param("groupIds") List<Long> groupIds,
            @Param("supplierIds") List<Long> supplierIds,
            @Param("salesChannelsIds") List<Long> salesChannelsIds,
            @Param("segmentIds") List<Long> segmentIds,
            @Param("goodsStatuses") List<GoodsStatus> goodsStatuses,
            @Param("globalSalesChannel") Boolean globalSalesChannel,
            @Param("globalSegment") Boolean globalSegment,
            @Param("salesChannelDirection") String salesChannelDirection,
            @Param("excludeOldVersion") String excludeOldVersion,
            Pageable pageable
    );
    Optional<Goods> findByIdAndGoodsStatusEnumIn(Long id, List<GoodsStatus> status);

    @Query(
            """
            select new bg.energo.phoenix.model.response.copy.group.CopyDomainWithVersionBaseResponse(
                g.id,
                concat(gd.name, ' (', g.id, ')')
            )
            from Goods g
            join GoodsDetails gd on g.lastGoodsDetailsId = gd.id
                where (:prompt is null or g.id in (
                    select gs.id from Goods gs
                    right outer join GoodsDetails gds on gds.goods.id = gs.id
                      where lower(gds.name) like :prompt or cast(gs.id as string) like :prompt)
                )
                and g.goodsStatusEnum in (:statuses)
            order by g.id DESC
            """
    )
    Page<CopyDomainWithVersionBaseResponse> findByCopyDomainWithVersionBaseRequestAdnStatusIn(
            @Param("prompt") String prompt,
            @Param("statuses") List<GoodsStatus> statuses,
            PageRequest pageRequest
    );

    @Query("""
            select count(gog.id) > 0
            from Goods g
            join GoodsDetails gd on gd.goods.id = g.id
            join GoodsOrderGoods gog on gog.goodsDetailsId = gd.id
            join GoodsOrder go on gog.orderId = go.id
            where g.id = :id
            and go.status = 'ACTIVE'
            """)
    boolean hasActiveConnectionToGoodsOrder(Long id);
}
