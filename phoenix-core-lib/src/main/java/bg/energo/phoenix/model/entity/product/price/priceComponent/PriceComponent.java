package bg.energo.phoenix.model.entity.product.price.priceComponent;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.entity.nomenclature.product.priceComponent.PriceComponentPriceType;
import bg.energo.phoenix.model.entity.nomenclature.product.priceComponent.PriceComponentValueType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.IssuedSeparateInvoice;
import bg.energo.phoenix.model.enums.product.price.priceComponent.NumberType;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.XEnergieApplicationType;
import bg.energo.phoenix.model.request.product.price.priceComponent.PriceComponentRequest;
import bg.energo.phoenix.service.billing.runs.models.BillingDataPriceComponentGroup;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Objects;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "price_components", schema = "price_component")

@SqlResultSetMapping(
        name = "price_component.billingDataMapping",
        classes = {
                @ConstructorResult(targetClass = BillingDataPriceComponentGroup.class,
                        columns = {
                                @ColumnResult(name = "contractDetailId"),
                                @ColumnResult(name = "priceComponentId"),
                                @ColumnResult(name = "startDate"),
                                @ColumnResult(name = "endDate"),


                        }
                )}
)
@NamedNativeQuery(
        name = "price_component.billing_price_component_search",
        query = """
                with prod as (select pd.id
                              from product_contract.contract_details cd
                                       join product.product_details pd on pd.id = cd.product_detail_id
                              where cd.id = :id)
                select prdId as contractDetailId,result.id as priceComponentId, result.start_date as startDate, result.endDate as endDate
                from (select prd.id as prdId, pc.id, pcgd.start_date, ppcg.product_detail_id, (
                    select pcgd2.start_date
                    from price_component.price_component_group_details pcgd2
                    where pcgd2.price_component_group_id =pcg.id
                    and pcgd2.start_date>pcgd.start_date
                    order by pcgd2.start_date limit 1
                    ) as endDate
                      from prod prd,
                           product.product_price_component_groups ppcg
                               join price_component.price_component_groups pcg on pcg.id = ppcg.price_component_group_id
                               join price_component.price_component_group_details pcgd on pcg.id = pcgd.price_component_group_id
                               join price_component.price_components pc on pc.price_component_group_detail_id = pcgd.id
                      where ppcg.product_detail_id = prd.id

                      union all
                      select ppc.price_component_id, null, ppc.product_detail_id, null
                      from product.product_price_components ppc,
                           prod prd
                      where ppc.product_detail_id = prd.id) as result
                order by result.id;
                """,
        resultSetMapping = "price_component.billingDataMapping"
)
public class PriceComponent extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "price_components_id_seq",
            sequenceName = "price_component.price_components_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "price_components_id_seq"
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_component_price_type_id")
    private PriceComponentPriceType priceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_component_value_type_id")
    private PriceComponentValueType valueType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id")
    private Currency currency;

    @Column(name = "currency_id", insertable = false, updatable = false)
    private Long currencyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vat_rate_id")
    private VatRate vatRate;

    @Column(name = "vat_rate_id", insertable = false, updatable = false)
    private Long vatRateId;

    @Column(name = "name")
    private String name;

    @Column(name = "invoice_and_template_text")
    private String invoiceAndTemplateText;

    @Column(name = "number_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NumberType numberType;

    @Column(name = "global_vat_rate")
    private Boolean globalVatRate;

    @Column(name = "discount")
    private Boolean discount;

    @Column(name = "income_account_number")
    private String incomeAccountNumber;

    @Column(name = "cost_center_controlling_order")
    private String costCenterControllingOrder;

    @Column(name = "contract_template_tag")
    private String contractTemplateTag;

    @Column(name = "price_in_words")
    private String priceInWords;

    @Column(name = "price_formula")
    private String priceFormula;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PriceComponentStatus status;

    @Column(name = "issued_separate_invoice")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private IssuedSeparateInvoice issuedSeparateInvoice;

    @Column(name = "conditions")
    private String conditions;

    @Column(name = "price_component_group_detail_id")
    private Long priceComponentGroupDetailId;

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "priceComponent")
    private List<PriceComponentFormulaVariable> formulaVariables;

    @Column(name = "don_not_include_in_the_vat_base")
    private Boolean doNotIncludeInTheVatBase;

    @Column(name = "alt_invoice_recipient_customer_detail_id")
    private Long alternativeRecipientCustomerDetailId;

    @Column(name = "xenergie_application")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private XEnergieApplicationType xenergieApplicationType;

    public PriceComponent(PriceComponentRequest request) {
        this.name = request.getName();
        this.invoiceAndTemplateText = request.getDisplayName();
        this.numberType = request.getNumberType();
        this.globalVatRate = request.getGlobalVatRate();
        this.discount = Objects.requireNonNullElse(request.getDiscount(), false);
        this.incomeAccountNumber = request.getNumberOfIncomeAccount();
        this.costCenterControllingOrder = request.getControllingOrder();
        this.contractTemplateTag = request.getTagForContractTemplate();
        this.status = PriceComponentStatus.ACTIVE;
        this.doNotIncludeInTheVatBase = Objects.requireNonNullElse(request.getDoNotIncludeVatBase(), false);
        this.alternativeRecipientCustomerDetailId = request.getAlternativeRecipientCustomerDetailId();
        this.xenergieApplicationType = (!Objects.requireNonNullElse(request.getConsumer(), false) && !Objects.requireNonNullElse(request.getGenerator(), false)) ? null :
                (Objects.requireNonNullElse(request.getConsumer(), false) ? XEnergieApplicationType.CONSUMER : XEnergieApplicationType.GENERATOR);
    }

    @Override
    public String toString() {
        return "PriceComponent{" +
                "id=" + id +
                ", priceType=" + priceType +
                ", valueType=" + valueType +
                ", currency=" + currency +
                ", vatRate=" + vatRate +
                ", name='" + name + '\'' +
                ", invoiceAndTemplateText='" + invoiceAndTemplateText + '\'' +
                ", numberType=" + numberType +
                ", globalVatRate=" + globalVatRate +
                ", discount=" + discount +
                ", incomeAccountNumber='" + incomeAccountNumber + '\'' +
                ", costCenterControllingOrder='" + costCenterControllingOrder + '\'' +
                ", contractTemplateTag='" + contractTemplateTag + '\'' +
                ", priceInWords='" + priceInWords + '\'' +
                ", priceFormula='" + priceFormula + '\'' +
                ", status=" + status +
                ", issuedSeparateInvoice=" + issuedSeparateInvoice +
                ", conditions='" + conditions + '\'' +
                ", priceComponentGroupDetailId=" + priceComponentGroupDetailId +
                '}';
    }
}
