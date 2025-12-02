package bg.energo.phoenix.model.response.crm.smsCommunication;

import bg.energo.phoenix.model.response.crm.LongArrayToListConverter;
import jakarta.persistence.Convert;

import java.util.List;

public interface ActiveContractsAndAssociatedCustomersForMassCommunicationProjection {
    Long getCustomerDetailId();
    String getCustomerIdentifier();
    Long getCustomerVersion();
    @Convert(converter = LongArrayToListConverter.class)
    List<Long> getServiceContractDetailIds();
    @Convert(converter = LongArrayToListConverter.class)
    List<Long> getProductContractDetailIds();
}
