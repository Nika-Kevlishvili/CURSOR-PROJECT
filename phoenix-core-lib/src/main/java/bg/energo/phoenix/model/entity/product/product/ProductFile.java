package bg.energo.phoenix.model.entity.product.product;

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
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@SuperBuilder
@Table(schema = "product", name = "product_files")
public class ProductFile extends FileArchivation {
    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "product_files_seq", sequenceName = "product.product_files_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_files_seq")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "product_detail_id")
    private Long productDetailId;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_url")
    protected String localFileUrl;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "product.product_file_status"
            )
    )
    @Column(name = "file_statuses", columnDefinition = "product.product_file_status[]")
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


