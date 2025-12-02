package bg.energo.phoenix.model.entity.contract.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contract_term_end_date_renewal_count", schema = "service_contract")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTermRenewalCount extends BaseEntity {

    @Id
    @SequenceGenerator(name = "service_contract_term_end_date_renewal_count_id_seq", sequenceName = "service_contract.contract_term_end_date_renewal_count_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "service_contract_term_end_date_renewal_count_id_seq")
    private  Long id;

    @Column(name = "service_contract_term_id")
    private Long serviceContractTermId;

    @Column(name = "contract_detail_id")
    private Long contractDetailId;

    @Column(name = "renewal_count")
    private Integer renewalCount;
}
