package bg.energo.phoenix.model.entity.contract.product;

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

@SuperBuilder
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "contract_files", schema = "product_contract")
public class ProductContractFile extends FileArchivation {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_contract_file_seq")
    @SequenceGenerator(name = "product_contract_file_seq", schema = "product_contract", sequenceName = "contract_files_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "contract_detail_id")
    private Long contractDetailId;

    @Column(name = "file_url")
    protected String localFileUrl;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "product_contract.contract_file_status"
            )
    )
    @Column(name = "file_statuses", columnDefinition = "product_contract.contract_file_status[]")
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