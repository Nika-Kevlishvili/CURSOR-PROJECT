package bg.energo.phoenix.model.entity.product.product;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(schema = "product", name = "product_details_collection_channels")
public class ProductDetailCollectionChannels extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "product_details_collection_channels_id_seq", sequenceName = "product.product_details_collection_channels_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_details_collection_channels_id_seq")
    private Long id;

    @Column(name = "collection_channel_id")
    private Long collectionChannelId;

    @Column(name = "product_details_id")
    private Long productDetailsId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private EntityStatus status;
}
