package bg.energo.phoenix.util.contract.product;

import bg.energo.phoenix.model.enums.contract.express.ProductContractVersionStatus;
import bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus;
import bg.energo.phoenix.model.enums.contract.service.ContractVersionStatus;
import bg.energo.phoenix.model.enums.contract.service.ServiceContractDetailStatus;

import java.util.List;
import java.util.Objects;

import static bg.energo.phoenix.model.enums.contract.products.ContractDetailsStatus.*;

public class ProductContractStatusChainUtil {

    public static boolean canBeChanged(ContractDetailsStatus current, ContractDetailsStatus modified) {
        return switch (current) {
            case DRAFT -> List.of(DRAFT,READY,SIGNED,ENTERED_INTO_FORCE, CANCELLED).contains(modified);
            case READY -> List.of(DRAFT,READY, SIGNED,CANCELLED,ENTERED_INTO_FORCE).contains(modified);
            case SIGNED -> List.of(SIGNED,ENTERED_INTO_FORCE, TERMINATED,CANCELLED).contains(modified);
            case ENTERED_INTO_FORCE -> List.of(ENTERED_INTO_FORCE,ACTIVE_IN_TERM, TERMINATED).contains(modified);
            case ACTIVE_IN_TERM -> List.of(ACTIVE_IN_TERM,ACTIVE_IN_PERPETUITY, TERMINATED).contains(modified);
            case ACTIVE_IN_PERPETUITY -> List.of(TERMINATED, ACTIVE_IN_PERPETUITY).contains(modified);
            case TERMINATED -> List.of(TERMINATED,ACTIVE_IN_PERPETUITY, ACTIVE_IN_TERM).contains(modified);
            default -> false;
        };
    }
    public static boolean canServiceContractChanged(ServiceContractDetailStatus current, ServiceContractDetailStatus modified) {
        if(current.equals(modified)){
            return true;
        }
        return switch (current) {
            case DRAFT -> List.of(ServiceContractDetailStatus.SIGNED,ServiceContractDetailStatus.READY, ServiceContractDetailStatus.ENTERED_INTO_FORCE,ServiceContractDetailStatus.CANCELLED).contains(modified);
            case READY -> List.of(ServiceContractDetailStatus.DRAFT,ServiceContractDetailStatus.ENTERED_INTO_FORCE, ServiceContractDetailStatus.SIGNED,CANCELLED).contains(modified);
            case SIGNED -> List.of(ServiceContractDetailStatus.ENTERED_INTO_FORCE, ServiceContractDetailStatus.TERMINATED,ServiceContractDetailStatus.CANCELLED).contains(modified);
            case ENTERED_INTO_FORCE -> List.of(ServiceContractDetailStatus.ACTIVE_IN_TERM, ServiceContractDetailStatus.TERMINATED).contains(modified);
            case ACTIVE_IN_TERM -> List.of(ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY, ServiceContractDetailStatus.TERMINATED).contains(modified);
            case ACTIVE_IN_PERPETUITY -> Objects.equals(ServiceContractDetailStatus.TERMINATED, modified);
            case TERMINATED -> List.of(ServiceContractDetailStatus.ACTIVE_IN_PERPETUITY, ServiceContractDetailStatus.ACTIVE_IN_TERM).contains(modified);
            default -> false;
        };
    }

    public static boolean versionStatusCanBeChanged(ProductContractVersionStatus current, ProductContractVersionStatus modified) {
        if(current.equals(modified)){
            return true;
        }
        return switch (current) {
            case DRAFT -> List.of(ProductContractVersionStatus.DRAFT,ProductContractVersionStatus.READY, ProductContractVersionStatus.CANCELLED).contains(modified);
            case READY -> List.of(ProductContractVersionStatus.READY,ProductContractVersionStatus.DRAFT, ProductContractVersionStatus.SIGNED, ProductContractVersionStatus.CANCELLED).contains(modified);
            case SIGNED -> List.of(ProductContractVersionStatus.SIGNED,ProductContractVersionStatus.CANCELLED).contains(modified);
            default -> false;
        };
    }
    public static boolean serviceContractVersionStatusCanBeChanged(ContractVersionStatus current, ContractVersionStatus modified) {
        if(current.equals(modified)){
            return true;
        }
        return switch (current) {
            case DRAFT -> List.of(ContractVersionStatus.DRAFT,ContractVersionStatus.READY, ContractVersionStatus.CANCELLED).contains(modified);
            case READY -> List.of(ContractVersionStatus.READY,ContractVersionStatus.DRAFT, ContractVersionStatus.SIGNED, ContractVersionStatus.CANCELLED).contains(modified);
            case SIGNED -> List.of(ContractVersionStatus.SIGNED,ContractVersionStatus.CANCELLED).contains(modified);
            default -> false;
        };
    }
}
