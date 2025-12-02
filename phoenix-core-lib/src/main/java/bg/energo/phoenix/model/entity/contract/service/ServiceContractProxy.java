package bg.energo.phoenix.model.entity.contract.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.contract.products.ContractSubObjectStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(schema = "service_contract", name = "contract_proxies")
public class ServiceContractProxy extends BaseEntity {
    @Id
    @SequenceGenerator(
            name = "service_contract_proxies_id_seq",
            sequenceName = "service_contract.contract_proxies_id_seq",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "service_contract_proxies_id_seq"
    )
    private Long id;

    @Column(name = "proxy_name")
    private String proxyName;

    @Column(name = "proxy_foreign_entity_person")
    private Boolean proxyForeignEntityPerson;

    @Column(name = "proxy_personal_identifier")
    private String proxyPersonalIdentifier;

    @Column(name = "proxy_email")
    private String proxyEmail;

    @Column(name = "proxy_mobile_phone")
    private String proxyMobilePhone;

    @Column(name = "proxy_attorney_power_number")
    private String proxyAttorneyPowerNumber;

    @Column(name = "proxy_date")
    private LocalDate proxyDate;

    @Column(name = "proxy_valid_till")
    private LocalDate proxyValidTill;

    @Column(name = "proxy_notary_public")
    private String proxyNotaryPublic;

    @Column(name = "proxy_registration_number")
    private String proxyRegistrationNumber;

    @Column(name = "proxy_operation_area")
    private String proxyOperationArea;

    @Column(name = "proxy_by_proxy_foreign_entity_person")
    private Boolean proxyByProxyForeignEntityPerson;


    @Column(name = "proxy_by_proxy_name")
    private String proxyByProxyName;

    @Column(name = "proxy_by_proxy_personal_identifier")
    private String proxyByProxyPersonalIdentifier;

    @Column(name = "proxy_by_proxy_email")
    private String proxyByProxyEmail;

    @Column(name = "proxy_by_proxy_mobile_phone")
    private String proxyByProxyMobilePhone;

    @Column(name = "proxy_by_proxy_attorney_power_number")
    private String proxyByProxyAttorneyPowerNumber;

    @Column(name = "proxy_by_proxy_date")
    private LocalDate proxyByProxyDate;

    @Column(name = "proxy_by_proxy_valid_till")
    private LocalDate proxyByProxyValidTill;

    @Column(name = "proxy_by_proxy_notary_public")
    private String proxyByProxyNotaryPublic;

    @Column(name = "proxy_by_proxy_registration_number")
    private String proxyByProxyRegistrationNumber;

    @Column(name = "proxy_by_proxy_operation_area")
    private String proxyByProxyOperationArea;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ContractSubObjectStatus status;

    @Column(name = "contract_detail_id")
    private Long contractDetailId;
}
