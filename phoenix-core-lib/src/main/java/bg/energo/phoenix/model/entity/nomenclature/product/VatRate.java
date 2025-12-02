package bg.energo.phoenix.model.entity.nomenclature.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.nomenclature.product.VatRateRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "vat_rates", schema = "nomenclature")
public class VatRate extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "vat_rates_seq",
            sequenceName = "nomenclature.vat_rates_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "vat_rates_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "value_in_percent")
    private BigDecimal valueInPercent;

    @Column(name = "global_vat_rate")
    private Boolean globalVatRate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "ordering_id")
    private Long orderingId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private NomenclatureItemStatus status;

    public VatRate(VatRateRequest request){
        this.name = request.getName().trim();
        this.valueInPercent = request.getValueInPercent();
        this.globalVatRate = request.getGlobalVatRate();
        this.startDate = request.getStartDate();
        this.status = request.getStatus();
    }
}
