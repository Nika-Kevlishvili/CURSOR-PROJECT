package bg.energo.phoenix.process.model.entity;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "process_files", schema = "process_management")
public class ProcessFile extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "process_files_id_seq",
            schema = "process_management",
            sequenceName = "process_files_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "process_files_id_seq"
    )
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "process_id")
    private Long processId;

}
