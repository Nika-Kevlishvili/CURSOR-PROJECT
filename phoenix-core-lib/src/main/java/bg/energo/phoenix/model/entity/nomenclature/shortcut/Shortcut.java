package bg.energo.phoenix.model.entity.nomenclature.shortcut;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.nomenclature.shortcut.UserShortcuts;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "shortcuts", schema = "nomenclature")
public class Shortcut extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "shortcut_id_seq",
            schema = "nomenclature",
            sequenceName = "shortcut_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "shortcut_id_seq"
    )
    private Long id;

    @Column(name = "username")
    private String username;
    @Column(name = "user_shortcut")
    @Enumerated(EnumType.STRING)
    private UserShortcuts shortcut;

    @Column(name = "ordering_id")
    private Long orderingId;

}
