package bg.energo.phoenix.model.entity.contract.order.service;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "order_proxies", schema = "service_order")
public class ServiceOrderProxy extends BaseEntity {

    @Id
    @Column(name = "id")
    @SequenceGenerator(
            name = "order_proxies_id_seq",
            sequenceName = "order_proxies_id_seq",
            schema = "service_order",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "order_proxies_id_seq"
    )
    private Long id;

    @Column(name = "proxy_name")
    private String name;

    @Column(name = "proxy_foreign_entity_person")
    private Boolean foreignEntity;

    @Column(name = "proxy_personal_identifier")
    private String identifier;

    @Column(name = "proxy_email")
    private String email;

    @Column(name = "proxy_mobile_phone")
    private String mobilePhone;

    @Column(name = "proxy_attorney_power_number")
    private String attorneyPowerNumber;

    @Column(name = "proxy_date")
    private LocalDate date;

    @Column(name = "proxy_valid_till")
    private LocalDate validTill;

    @Column(name = "proxy_notary_public")
    private String notaryPublic;

    @Column(name = "proxy_registration_number")
    private String registrationNumber;

    @Column(name = "proxy_operation_area")
    private String operationArea;

    @Column(name = "proxy_by_proxy_foreign_entity_person")
    private Boolean authorizedProxyForeignEntity;

    @Column(name = "proxy_by_proxy_name")
    private String authorizedProxyName;

    @Column(name = "proxy_by_proxy_personal_identifier")
    private String authorizedProxyIdentifier;

    @Column(name = "proxy_by_proxy_email")
    private String authorizedProxyEmail;

    @Column(name = "proxy_by_proxy_mobile_phone")
    private String authorizedProxyMobilePhone;

    @Column(name = "proxy_by_proxy_attorney_power_number")
    private String authorizedProxyAttorneyPowerNumber;

    @Column(name = "proxy_by_proxy_date")
    private LocalDate authorizedProxyDate;

    @Column(name = "proxy_by_proxy_valid_till")
    private LocalDate authorizedProxyValidTill;

    @Column(name = "proxy_by_proxy_notary_public")
    private String authorizedProxyNotaryPublic;

    @Column(name = "proxy_by_proxy_registration_number")
    private String authorizedProxyRegistrationNumber;

    @Column(name = "proxy_by_proxy_operation_area")
    private String authorizedProxyOperationArea;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "order_id")
    private Long orderId;

}
