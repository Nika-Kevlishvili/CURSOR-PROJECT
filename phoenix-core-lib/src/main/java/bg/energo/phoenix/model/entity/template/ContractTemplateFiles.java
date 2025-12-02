package bg.energo.phoenix.model.entity.template;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SuperBuilder
@Table(name = "template_files", schema = "template")
public class ContractTemplateFiles extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "template_files_id_gen")
    @SequenceGenerator(name = "template_files_id_gen", sequenceName = "template_files_id_seq", schema = "template", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

}
