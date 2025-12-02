package bg.energo.phoenix.process.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "process_contract_pods", schema = "process_management")

@Data
public class ProcessContractPods {

    @Id
    @Column(name = "id", nullable = false)
    @SequenceGenerator(
            name = "process_contract_pods_id_seq",
            sequenceName = "process_management.process_contract_pods_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "process_contract_pods_id_seq"
    )
    private Long id;

    @Column(name = "processed_record_info_id")
    private Long recordInfoId;

    @Column(name = "contract_pod_id")
    private Long contractPodId;

    @CreationTimestamp
    @Column(name = "create_date", nullable = false)
    private LocalDateTime createDate;
}
