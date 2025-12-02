package bg.energo.phoenix.model.entity.product.goods;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsGroups;
import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsSuppliers;
import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsUnits;
import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "goods_details", schema = "goods")
public class GoodsDetails extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "goods_details_id_seq", sequenceName = "goods.goods_details_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "goods_details_id_seq")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "name_transl")
    private String nameTransl;

    @Column(name = "printing_name")
    private String printingName;

    @Column(name = "printing_name_transl")
    private String printingNameTransl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_groups_id", nullable = false)
    private GoodsGroups goodsGroups;

    @Column(name = "other_system_connection_code")
    private String otherSystemConnectionCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_suppliers_id", nullable = false)
    private GoodsSuppliers goodsSuppliers;

    @Column(name = "manufacturer_code_number")
    private String manufacturerCodeNumber;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private GoodsDetailStatus status;

    @Column(name = "price")
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", nullable = false)
    private Currency currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_units_id", nullable = false)
    private GoodsUnits goodsUnits;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vat_rate_id", nullable = false)
    private VatRate vatRate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_id", nullable = false)
    private Goods goods;

    @Column(name = "income_account_numbers")
    private String incomeAccountNumbers;

    @Column(name = "controlling_orders")
    private String controllingOrderId;

    @Column(name = "version_id")
    private Long versionId;

    @Column(name = "global_vat_rate")
    private Boolean globalVatRate;

    @Column(name = "global_sales_area")
    private Boolean globalSalesArea;

    @Column(name = "global_sales_channel")
    private Boolean globalSalesChannel;

    @Column(name = "global_segment")
    private Boolean globalSegment;

}
