package bg.energo.phoenix.model.view;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "vw_service_contract_terms", schema = "service")
@Immutable
@Data
public class VwServiceContractTerms {

    @Id
    private Long serviceDetailsId;

    @Nationalized
    private String name;

    @Nationalized
    private String nameDesc;

}
