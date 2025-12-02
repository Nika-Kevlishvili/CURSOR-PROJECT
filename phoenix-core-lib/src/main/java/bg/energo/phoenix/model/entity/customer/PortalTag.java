package bg.energo.phoenix.model.entity.customer;

import bg.energo.common.portal.api.appTag.AppTag;
import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "portal_tags", schema = "customer")
@AllArgsConstructor
@NoArgsConstructor
public class PortalTag extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "portal_tags_id_gen")
    @SequenceGenerator(name = "portal_tags_id_gen", sequenceName = "customer.portal_tags_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "portal_id")
    private String portalId;

    @Column(name = "description")
    private String description;

    @Column(name = "name")
    private String name;
    @Column(name = "description_bg")
    private String descriptionBg;

    @Column(name = "name_bg")
    private String nameBg;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    public PortalTag(AppTag tag,AppTag bulgarianTag) {
        this.portalId=tag.getId().toString();
        this.description=tag.getDescription();
        this.name=tag.getName();
        this.status=EntityStatus.ACTIVE;
        this.nameBg=bulgarianTag.getName();
        this.descriptionBg=bulgarianTag.getDescription();
    }
}