package bg.energo.phoenix.model.entity.contract.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "contract_service_additional_params", schema = "service_contract")
public class ServiceContractAdditionalParams extends BaseEntity {
    @Id
    @SequenceGenerator(name = "contract_additional_params_id_seq", sequenceName = "service_contract.contract_additional_params_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "contract_additional_params_id_seq")
    private Long id;

    @Column(name = "label")
    private String label;

    @Column(name = "value")
    private String value;

    @Column(name = "service_additional_param_id")
    private Long serviceAdditionalParamId;

    @Column(name = "contract_detail_id")
    private Long contractDetailId;
}
