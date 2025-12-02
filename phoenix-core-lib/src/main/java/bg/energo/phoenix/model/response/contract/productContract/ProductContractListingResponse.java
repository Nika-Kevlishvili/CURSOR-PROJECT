package bg.energo.phoenix.model.response.contract.productContract;

import bg.energo.phoenix.model.entity.nomenclature.product.product.ProductTypes;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailType;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsSubStatus;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ProductContractListingResponse {
    private Long id;
    private String contractNumber;
    private String customer;
    private String productName;
    private String productTypeName;
    private LocalDate dateOfSigning;
    private ContractDetailsStatus contractDetailsStatus;
    private ContractDetailsSubStatus contractDetailsSubStatus;
    private LocalDate activateDate;
    private LocalDate contractTermDate;
    private LocalDate dateOfEntryIntoPerpetuity;
    private ProductContractStatus status;
    private LocalDateTime createDate;

    private Boolean locked;
    private Boolean isLockedByInvoice;

    public ProductContractListingResponse(Long id,
                                          Boolean locked,
                                          String contractNumber,
                                          String customer,
                                          String productName,
                                          ProductTypes productType,
                                          LocalDate dateOfSigning,
                                          ContractDetailsStatus contractDetailsStatus,
                                          ContractDetailsSubStatus contractDetailsSubStatus,
                                          LocalDate activateDate,
                                          LocalDate contractTermDate,
                                          LocalDate dateOfEntryIntoPerpetuity,
                                          ProductContractStatus status,
                                          LocalDateTime createDate,
                                          ContractDetailType detailType,
                                          Integer agreementSuffix,
                                          Boolean isLockedByInvoice) {
        this.id = id;
        if (detailType.equals(ContractDetailType.ADDITIONAL_AGREEMENT) || detailType.equals(ContractDetailType.EX_OFFICIO_AGREEMENT)) {
            this.contractNumber = contractNumber + "#" + (agreementSuffix == null ? 0 : agreementSuffix);
        } else {
            this.contractNumber = contractNumber;
        }
        this.customer = customer;
        this.productName = productName;
        this.productTypeName = productType.getName();
        this.dateOfSigning = dateOfSigning;
        this.contractDetailsStatus = contractDetailsStatus;
        this.contractDetailsSubStatus = contractDetailsSubStatus;
        this.activateDate = activateDate;
        this.contractTermDate = contractTermDate;
        this.dateOfEntryIntoPerpetuity = dateOfEntryIntoPerpetuity;
        this.status = status;
        this.createDate = createDate;
        this.locked = locked;
        this.isLockedByInvoice = isLockedByInvoice;
    }
}
