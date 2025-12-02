package bg.energo.phoenix.model.response.contract.serviceContract;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.nomenclature.product.service.ServiceType;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractContractType;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.customer.CustomerType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Data
@NoArgsConstructor
public class ServiceContractListingResponse {
    private Long id;
    private String contractNumber;
    private String customer;
    private String serviceName;
    private String serviceTypeName;
    private LocalDate dateOfSigning;
    private ServiceContractDetailStatus contractDetailsStatus;
    private ServiceContractDetailsSubStatus contractDetailsSubStatus;
    /*private LocalDate activateDate;*/
    private LocalDate contractTermDate;
    private LocalDate dateOfEntryIntroPerpetuity;
    private EntityStatus status;
    private LocalDateTime createDate;
    private String agreementSuffix;
    private Boolean isLockedByInvoice;

    public ServiceContractListingResponse(Long id,
                                          String contractNumber,
                                          CustomerType customerType,
                                          String customerIdentifier,
                                          String firstName,
                                          String middleName,
                                          String lastName,
                                          String legalFormName,
                                          String serviceName,
                                          ServiceType serviceType,
                                          LocalDate dateOfSigning,
                                          ServiceContractDetailStatus contractDetailsStatus,
                                          ServiceContractDetailsSubStatus contractDetailsSubStatus,
                                          LocalDate contractTermDate,
                                          LocalDate dateOfEntryIntroPerpetuity,
                                          EntityStatus status,
                                          LocalDateTime createDate,
                                          ServiceContractContractType type,
                                          Integer agreementSuffix,
                                          Boolean isLockedByInvoice) {
        this.id = id;
        this.contractNumber = contractNumber;
        this.customer = switch (customerType) {
            case LEGAL_ENTITY -> "%s %s (%s)".formatted(
                    firstName,
                    Objects.requireNonNullElse(legalFormName, ""),
                    customerIdentifier
            );
            case PRIVATE_CUSTOMER, PRIVATE_CUSTOMER_WITH_BUSINESS_ACTIVITY -> "%s %s %s (%s)".formatted(
                    firstName,
                    Objects.requireNonNullElse(middleName, ""),
                    lastName,
                    customerIdentifier
            );
        };
        this.serviceName = serviceName;
        this.serviceTypeName = serviceType.getName();
        this.dateOfSigning = dateOfSigning;
        this.contractDetailsStatus = contractDetailsStatus;
        this.contractDetailsSubStatus = contractDetailsSubStatus;
        /*this.activateDate = activateDate;*/
        this.contractTermDate = contractTermDate;
        this.dateOfEntryIntroPerpetuity = dateOfEntryIntroPerpetuity;
        this.status = status;
        this.createDate = createDate;
        if (type.equals(ServiceContractContractType.ADDITIONAL_AGREEMENT) || type.equals(ServiceContractContractType.EX_OFFICIO_AGREEMENT)) {
            this.contractNumber = contractNumber + "#" + (agreementSuffix == null ? 0 : agreementSuffix);
        } else {
            this.contractNumber = contractNumber;
        }
        this.isLockedByInvoice = isLockedByInvoice;
    }
}
