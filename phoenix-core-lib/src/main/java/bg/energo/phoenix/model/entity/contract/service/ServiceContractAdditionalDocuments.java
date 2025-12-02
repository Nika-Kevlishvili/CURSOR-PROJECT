package bg.energo.phoenix.model.entity.contract.service;

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

@Entity
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "contract_additional_docs", schema = "service_contract")
public class ServiceContractAdditionalDocuments extends FileArchivation {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "service_contract_document_seq")
    @SequenceGenerator(name = "service_contract_document_seq", schema = "service_contract", sequenceName = "contract_additional_docs_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "contract_detail_id")
    private Long contractDetailId;

    @Column(name = "file_url")
    protected String localFileUrl;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "service_contract.contract_file_status"
            )
    )
    @Column(name = "file_statuses", columnDefinition = "service_contract.contract_file_status[]")
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
