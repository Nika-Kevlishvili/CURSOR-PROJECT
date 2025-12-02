package bg.energo.phoenix.model.view;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "vw_customer_group_account_managers", schema = "customer")
@Immutable
@Data
public class VwConnectedGroupManager {
    @Id
    private Long connectedGroupId;

    @Nationalized
    private String displayName;

    @Nationalized
    private String displayNameDesc;
}
