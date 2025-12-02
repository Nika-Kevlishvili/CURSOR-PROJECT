package bg.energo.phoenix.model.enums.contract;

import bg.energo.phoenix.model.enums.billing.billings.ContractOrderType;

public enum ContractType {
    PRODUCT_CONTRACT,
    SERVICE_CONTRACT;

    public ContractOrderType toContractOrderType(){
        if (this.equals(PRODUCT_CONTRACT)) {
            return ContractOrderType.PRODUCT_CONTRACT;
        }else {
            return ContractOrderType.SERVICE_CONTRACT;
        }
    }
}
