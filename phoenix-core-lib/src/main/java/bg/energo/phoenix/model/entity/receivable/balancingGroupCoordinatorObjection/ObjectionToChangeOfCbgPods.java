package bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "objection_to_change_of_cbg_pods", schema = "receivable")

public class ObjectionToChangeOfCbgPods extends BaseEntity {

    @Id
    @SequenceGenerator(
            name = "objection_to_change_of_cbg_pods_id_seq",
            sequenceName = "receivable.objection_to_change_of_cbg_pods_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "objection_to_change_of_cbg_pods_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "change_of_cbg_id")
    private Long objectionToCbg;

    @Column(name = "pod_id")
    private Long pod;

    public ObjectionToChangeOfCbgPods(Long objectionToCbg, Long pod) {
        this.objectionToCbg = objectionToCbg;
        this.pod = pod;
    }
}
