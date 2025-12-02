package bg.energo.phoenix.model.entity.product.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.product.product.ProductContractTermRenewalType;
import bg.energo.phoenix.model.enums.product.product.ProductSubObjectStatus;
import bg.energo.phoenix.model.enums.product.product.ProductTermPeriodType;
import bg.energo.phoenix.model.enums.product.product.ProductTermType;
import bg.energo.phoenix.model.request.product.product.BaseProductTermsRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@NoArgsConstructor
@AllArgsConstructor

@Table(schema = "product", name = "product_contract_terms")
public class ProductContractTerms extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "product_contract_terms_id_seq",
            sequenceName = "product.product_contract_terms_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "product_contract_terms_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;
    @Column(name = "contract_term_period_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private ProductTermPeriodType periodType;
    @Column(name = "contract_term_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private ProductTermType type;
    @Column(name = "value")
    private Integer value;
    @Column(name = "perpetuity_cause")
    private Boolean perpetuityCause;

    @Column(name = "product_details_id")
    private Long productDetailsId;

    @Column(name = "automatic_renewal")
    private Boolean automaticRenewal;

    @Column(name = "number_of_renewals")
    private Integer numberOfRenewals;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ProductSubObjectStatus status;

    @Column(name = "renewal_period_value")
    private Integer renewalPeriodValue;

    @Column(name = "renewal_period_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    private ProductContractTermRenewalType renewalPeriodType;

    public ProductContractTerms(BaseProductTermsRequest termsRequest, Long productDetailsId) {
        this.periodType = termsRequest.getTypeOfTerms();
        this.type = termsRequest.getPeriodType();
        this.value = termsRequest.getValue();
        this.name = termsRequest.getName();
        this.perpetuityCause = termsRequest.isPerpetuityCause();
        this.productDetailsId = productDetailsId;
        this.automaticRenewal = termsRequest.getAutomaticRenewal();
        this.numberOfRenewals = termsRequest.getNumberOfRenewals();
        this.status = ProductSubObjectStatus.ACTIVE;
        this.renewalPeriodValue = termsRequest.getRenewalPeriodValue();
        this.renewalPeriodType = termsRequest.getRenewalPeriodType();
    }
}
