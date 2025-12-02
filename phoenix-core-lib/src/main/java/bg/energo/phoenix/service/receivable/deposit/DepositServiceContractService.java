package bg.energo.phoenix.service.receivable.deposit;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.deposit.DepositServiceContract;
import bg.energo.phoenix.repository.receivable.deposit.DepositServiceContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DepositServiceContractService {
    private final DepositServiceContractRepository depositServiceContractRepository;

    public DepositServiceContract  saveDepositServiceContract(DepositServiceContract depositServiceContract) {
        return depositServiceContractRepository.save(depositServiceContract);
    }

    public DepositServiceContract depositServiceContractMapper(Long customerDepositId, Long contractId) {
        DepositServiceContract depositServiceContract = new DepositServiceContract();
        depositServiceContract.setCustomerDepositId(customerDepositId);
        depositServiceContract.setContractId(contractId);
        depositServiceContract.setStatus(EntityStatus.ACTIVE);
        depositServiceContract.setCreateDate(LocalDateTime.now());
        depositServiceContract.setModifyDate(LocalDateTime.now());

        return depositServiceContract;
    }

}
