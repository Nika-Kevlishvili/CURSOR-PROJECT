package bg.energo.phoenix.process.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_record_info", schema = "process_management")

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedRecordInfo {

    @Id
    @Column(name = "id", nullable = false)
    @SequenceGenerator(
            name = "processed_record_id_seq",
            sequenceName = "process_management.processed_record_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "processed_record_id_seq"
    )
    private Long id;

    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "record_identifier")
    private String recordIdentifier;

    @Column(name = "record_identifier_version")
    private String recordIdentifierVersion;

    @Column(name = "success")
    private boolean success;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "process_id")
    private Long processId;

    @CreationTimestamp
    @Column(name = "create_date", nullable = false)
    private LocalDateTime createDate;

}
