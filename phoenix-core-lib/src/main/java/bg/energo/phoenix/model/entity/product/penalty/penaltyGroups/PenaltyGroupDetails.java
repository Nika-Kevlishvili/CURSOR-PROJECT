package bg.energo.phoenix.model.entity.product.penalty.penaltyGroups;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "penalty_group_details", schema = "terms")
public class PenaltyGroupDetails extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "penalty_group_details_id_seq",
            sequenceName = "terms.penalty_group_details_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "penalty_group_details_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "penalty_group_id")
    private Long penaltyGroupId;

    @Column(name = "version_id")
    private Integer versionId;

}
