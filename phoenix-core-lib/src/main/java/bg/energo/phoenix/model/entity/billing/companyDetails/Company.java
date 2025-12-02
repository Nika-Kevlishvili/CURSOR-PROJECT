package bg.energo.phoenix.model.entity.billing.companyDetails;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Table(name = "companies", schema = "company")
@Entity
@EqualsAndHashCode(callSuper = true)
public class Company extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "companies_id_seq",
            sequenceName = "company.companies_id_seq",
            allocationSize = 1)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "companies_id_seq"
    )
    private Long id;

    @Column(name = "company_number",columnDefinition = "serial")
//    @SequenceGenerator(
//            name = "company_number_seq",
//            sequenceName = "company.company_number_seq",
//            allocationSize = 1)
//    @GeneratedValue(
//            strategy = GenerationType.SEQUENCE,
//            generator = "company_number_seq")
    private Long companyNumber;

}
