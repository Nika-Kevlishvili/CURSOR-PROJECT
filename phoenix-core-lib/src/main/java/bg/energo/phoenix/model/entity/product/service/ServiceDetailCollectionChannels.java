package bg.energo.phoenix.model.entity.product.service;

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
@Table(schema = "service", name = "service_details_collection_channels")
public class ServiceDetailCollectionChannels extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "service_details_collection_channels_id_seq", sequenceName = "service.service_details_collection_channels_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "service_details_collection_channels_id_seq")
    private Long id;

    @Column(name = "collection_channel_id")
    private Long collectionChannelId;

    @Column(name = "service_details_id")
    private Long serviceDetailsId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status")
    private EntityStatus status;
}
