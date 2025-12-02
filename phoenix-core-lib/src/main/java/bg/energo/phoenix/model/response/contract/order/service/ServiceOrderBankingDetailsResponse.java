package bg.energo.phoenix.model.response.contract.order.service;

import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrder;
import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
import lombok.Data;

@Data
public class ServiceOrderBankingDetailsResponse {

    private Boolean directDebit;
    private Long bankId;
    private String bankName;
    private String bic;
    private String iban;

    public ServiceOrderBankingDetailsResponse(Bank bank, ServiceOrder serviceOrder) {
        this.directDebit = serviceOrder.getDirectDebit();
        this.bankId = bank == null ? null : bank.getId();
        this.bankName = bank == null ? null : bank.getName();
        this.bic = bank == null ? null : bank.getBic();
        this.iban = serviceOrder.getIban();
    }
}
