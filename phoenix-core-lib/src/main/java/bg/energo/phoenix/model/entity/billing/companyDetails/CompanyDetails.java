package bg.energo.phoenix.model.entity.billing.companyDetails;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@Table(name = "company_details", schema = "company")
@Entity
@EqualsAndHashCode(callSuper = true)
public class CompanyDetails extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "company_details_id_seq",
            sequenceName = "company.company_details_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "company_details_id_seq"
    )
    private Long id;

    @Column(name = "identifier")
    private String identifier;

    @Column(name = "vat_number")
    private String vatNumber;

    @Column(name = "number_under_excise_duties_tax_wh_act")
    private String numberUnderExciseDutiesTaxWhAct;

    @Column(name = "name")
    private String name;

    @Column(name = "name_transl")
    private String nameTranslated;

    @Column(name = "management_address")
    private String managementAddress;

    @Column(name = "management_address_transl")
    private String managementAddressTranslated;

    @Column(name = "version_id")
    private Long versionId;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "start_date")
    private LocalDate startDate;

}
