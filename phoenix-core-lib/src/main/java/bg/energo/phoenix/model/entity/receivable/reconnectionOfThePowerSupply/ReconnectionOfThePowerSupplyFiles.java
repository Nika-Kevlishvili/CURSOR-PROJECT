package bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply;

import bg.energo.phoenix.model.entity.FileArchivation;
import bg.energo.phoenix.model.enums.shared.DocumentFileStatus;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.util.List;

@Entity
@SuperBuilder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "power_supply_reconnection_files", schema = "receivable")

public class ReconnectionOfThePowerSupplyFiles extends FileArchivation {
    @Id
    @SequenceGenerator(
            name = "power_supply_reconnection_files_id_seq",
            sequenceName = "receivable.power_supply_reconnection_files_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "power_supply_reconnection_files_id_seq")
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "power_supply_reconnection_id")
    private Long powerSupplyReconnectionId;

    @Column(name = "file_url")
    protected String localFileUrl;

    @Type(
            value = ListArrayType.class,
            parameters = @org.hibernate.annotations.Parameter(
                    name = AbstractArrayType.SQL_ARRAY_TYPE,
                    value = "receivable.file_status"
            )
    )
    @Column(name = "file_statuses", columnDefinition = "receivable.file_status[]")
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
