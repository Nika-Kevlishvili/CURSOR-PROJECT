package bg.energo.phoenix.model.entity.product.service;

import bg.energo.phoenix.model.entity.FileArchivation;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.util.List;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(schema = "service", name = "service_files")
public class ServiceFile extends FileArchivation {
    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "service_files_seq", sequenceName = "service.service_files_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "service_files_seq")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "service_detail_id")
    private Long serviceDetailId;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_url")
    protected String localFileUrl;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "service.service_file_status"
            )
    )
    @Column(name = "file_statuses", columnDefinition = "service.service_file_status[]")
    private List<DocumentFileStatus> fileStatuses;

    @Override
    public String getSignedFileUrl() {
        return localFileUrl;
    }

    @Override
    public void setSignedFileUrl(String signedFileUrl) {
        this.localFileUrl = signedFileUrl;
    }

}
