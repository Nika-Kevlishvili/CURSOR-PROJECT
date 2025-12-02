package bg.energo.phoenix.model.entity.product.termination.terminationGroup;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "termination_group_details", schema = "product")
public class TerminationGroupDetails extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "termination_group_details_id_seq",
            sequenceName = "product.termination_group_details_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "termination_group_details_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "termination_group_id")
    private Long terminationGroupId;

    @Column(name = "version_id")
    private Long versionId;

}
