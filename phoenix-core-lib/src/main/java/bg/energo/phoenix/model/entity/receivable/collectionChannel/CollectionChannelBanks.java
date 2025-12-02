package bg.energo.phoenix.model.entity.receivable.collectionChannel;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "collection_channel_banks", schema = "receivable")
public class CollectionChannelBanks extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "collection_channel_banks_id_seq",
            schema = "receivable",
            sequenceName = "collection_channel_banks_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "collection_channel_banks_id_seq"
    )
    private Long id;

    @Column(name = "collection_channel_id")
    private Long collectionChannelId;

    @Column(name = "bank_id")
    private Long bankId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}
