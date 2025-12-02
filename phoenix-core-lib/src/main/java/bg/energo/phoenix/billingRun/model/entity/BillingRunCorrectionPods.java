package bg.energo.phoenix.billingRun.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;


@Entity
@Table(name = "correction_pods", schema = "billing_run")
@Data
public class BillingRunCorrectionPods {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "run_id")
    private Long runId;

    @Column(name = "correction_run_id")
    private Long correctionRunId;

    @Column(name = "pod_id")
    private Long podId;

    @Column(name = "full_reversal_needed")
    private Boolean fullReversalNeeded;
}
