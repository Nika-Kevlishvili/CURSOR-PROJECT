package bg.energo.phoenix.model.view;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

@Data
@Immutable
@Entity
@Table(
        name = "vw_order_customer_account_managers",
        schema = "service_order"
)
public class VwServiceOrderCustomerAccountManagers {

    @Id
    private Long customerDetailId;

    @Nationalized
    private String displayName;

    @Nationalized
    private String displayNameDesc;

}
