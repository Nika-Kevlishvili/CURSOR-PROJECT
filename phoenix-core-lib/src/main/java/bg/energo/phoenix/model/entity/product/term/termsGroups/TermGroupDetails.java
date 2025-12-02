package bg.energo.phoenix.model.entity.product.term.termsGroups;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "term_group_details", schema = "terms")
public class TermGroupDetails extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "group_details_id_seq",
            sequenceName = "terms.group_details_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "group_details_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "group_Id")
    private Long groupId;

    @Column(name = "version_id")
    private Long versionId;

    @Column(name = "start_date")
    private LocalDateTime startDate;
}
