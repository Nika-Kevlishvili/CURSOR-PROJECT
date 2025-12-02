package bg.energo.phoenix.model.entity.product.product.view;

import bg.energo.phoenix.model.response.product.RelatedEntityType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "vw_available_product_and_services", schema = "product")
@Immutable
@Data
public class VwAvailableProductAndServices {
    @Id
    private Long id;

    @Column(name = "displayname")
    private String displayName;

    @Column(name = "name")
    private String name;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private RelatedEntityType type;
}
