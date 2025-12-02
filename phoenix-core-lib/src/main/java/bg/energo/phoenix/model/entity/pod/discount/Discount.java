package bg.energo.phoenix.model.entity.pod.discount;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder

@EqualsAndHashCode(callSuper = false)
@Table(name = "discounts", schema = "pod")
public class Discount extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "discount_seq")
    @SequenceGenerator(name = "discount_seq", schema = "pod", sequenceName = "discounts_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "amount_in_percent")
    private BigDecimal amountInPercent;

    @Column(name = "amount_in_money_per_kwh")
    private BigDecimal amountInMoneyPerKWH;

    @Column(name = "date_from")
    private LocalDate dateFrom;

    @Column(name = "date_to")
    private LocalDate dateTo;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "certificate_number")
    private String certificationNumber;

    @Column(name = "currency_id")
    private Long currencyId;

    @Column(name = "volume_without_discount_in_kwh")
    private Long volumeWithoutDiscountInKWH;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "invoiced")
    private Boolean invoiced;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private EntityStatus status;
}
