package bg.energo.phoenix.repository.contract.order.goods;

import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderGoods;
import bg.energo.phoenix.model.response.contract.order.goods.GoodsOrderGoodsForInvoiceResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoodsOrderGoodsRepository extends JpaRepository<GoodsOrderGoods, Long> {
    List<GoodsOrderGoods> findAllByOrderId(Long orderId);

    void deleteAllByIdIn(List<Long> ids);
    @Query(value = """
                select
                gog.id as id,
                gog.quantity as quantity,
                gog.price as price,
                gog.currency_id as currencyId,
                c.alt_currency_id as altCurrencyId,
                c.alt_ccy_exchange_rate as altCurrencyExchangeRate,
                gog.income_account_numbers as numberOfIncomingAccount,
                gog.cost_center_controlling_order as costCenterOrControllingOrder,
                gog.goods_units_id as goodsUnitId,
                gog.name as name
                from goods_order.order_goods gog
                join nomenclature.currencies c on gog.currency_id = c.id
                left join goods.goods_details gd on gd.id = gog.goods_details_id
                left join goods.goods g on g.id = gd.goods_id
                where gog.order_id = :orderId
                and (gog.goods_details_id is null or text(g.status) = 'DELETED')
                union
                select
                gog.id,
                gog.quantity,
                gd.price,
                c.id,
                c.alt_currency_id,
                c.alt_ccy_exchange_rate,
                gd.income_account_numbers,
                gd.controlling_orders,
                gd.goods_units_id,
                gd.name
                from goods_order.order_goods gog
                join goods.goods_details gd on gd.id = gog.goods_details_id
                join goods.goods g on gd.goods_id = g.id
                join nomenclature.currencies c on c.id = gd.currency_id
                where gog.order_id = :orderId
                and text(g.status) = 'ACTIVE'
            """, nativeQuery = true)
    List<GoodsOrderGoodsForInvoiceResponse> getAllByOrderId(@Param("orderId") Long orderId);
}