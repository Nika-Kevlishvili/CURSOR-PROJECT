package bg.energo.phoenix.service.signing.qes.entities;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "qes_sign_session",schema = "template")
public class QesSignSession extends BaseEntity {
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "qes_sign_session_id_seq"
    )
    @SequenceGenerator(
            name = "qes_sign_session_id_seq",
            sequenceName = "template.qes_sign_session_id_seq",
            allocationSize = 1
    )
    private Long id;
    /**
     * signed path
     */
    @Column(name = "folder_path")
    private String signedPath;
    @Column(name = "unsigned_folder_path")
    private String unsignedPath;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "total_count")
    private Integer totalCount;
    @Column(name = "sign_count")
    private Integer signCount;
}
