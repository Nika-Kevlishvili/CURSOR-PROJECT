package bg.energo.phoenix.service.billing.invoice.numberGeneration;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(schema = "invoice", name = "invoice_number_generator")
public class InvoiceNumberTable extends BaseEntity {

    @Id
    @SequenceGenerator(schema = "invoice", allocationSize = 1, name = "invoice_number_generator_id_seq", sequenceName = "invoice_number_generator_id_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "invoice_number_generator_id_seq")
    private Long id;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "type")
    private NumberType numberType;
}
