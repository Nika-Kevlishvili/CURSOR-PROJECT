package bg.energo.phoenix.model.entity.contract.action;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Immutable;

@Data
@Entity
@Immutable
@Table(name = "vw_action_pods", schema = "action")
public class VwActionPods {

    @Id
    @Column(name = "action_id")
    private Long actionId;

    @Column(name = "name")
    private String name;

    @Column(name = "name_desc")
    private String nameDesc;

}
