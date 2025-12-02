package bg.energo.phoenix.service.document;

import bg.energo.phoenix.model.enums.contract.ContractType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailAndSmsDocumentRequest {
    String contractNumber;
    Long emailCommunicationCustomerId;
    Long smsCommunicationCustomerId;
    Long contractDetailId;
    Long contractId;
    ContractType contractType;
}
