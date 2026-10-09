package bg.energo.phoenix.bulgariapost.receipt.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "bulgaria_post_receipts", schema = "online_payment")
public class BulgariaPostReceiptEntity {

    @Id
    @SequenceGenerator(
            name = "bulgaria_post_receipts_id_seq",
            sequenceName = "online_payment.bulgaria_post_receipts_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "bulgaria_post_receipts_id_seq"
    )
    private Long id;

    @Column(name = "customer_number", nullable = false, length = 20)
    private String customerNumber;

    @Column(name = "billing_group_number", length = 4)
    private String billingGroupNumber;

    @Column(name = "document_number_raw", nullable = false)
    private String documentNumberRaw;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "exchange", precision = 20, scale = 5)
    private BigDecimal exchange;

    @Column(name = "paid_sum", nullable = false, precision = 20, scale = 2)
    private BigDecimal paidSum;

    @Column(name = "tid", nullable = false, length = 64, unique = true)
    private String tid;

    @Column(name = "post_code", length = 256)
    private String postCode;

    @Column(name = "principal", precision = 20, scale = 2)
    private BigDecimal principal;

    @Column(name = "interest", precision = 20, scale = 2)
    private BigDecimal interest;

}

