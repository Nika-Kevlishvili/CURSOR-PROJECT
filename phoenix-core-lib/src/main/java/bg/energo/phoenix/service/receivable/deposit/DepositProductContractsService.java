package bg.energo.phoenix.service.receivable.deposit;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.receivable.deposit.DepositProductContract;
import bg.energo.phoenix.repository.receivable.deposit.DepositProductContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DepositProductContractsService {
    private final DepositProductContractRepository depositProductContractRepository;

    public DepositProductContract saveDepositProductContract(DepositProductContract depositProductContract) {
        return depositProductContractRepository.saveAndFlush(depositProductContract);
    }

    public DepositProductContract depositProductContractMapper(Long customerDepositId, Long contractId) {
        DepositProductContract depositProductContract = new DepositProductContract();
        depositProductContract.setCustomerDepositId(customerDepositId);
        depositProductContract.setContractId(contractId);
        depositProductContract.setStatus(EntityStatus.ACTIVE);
        depositProductContract.setCreateDate(LocalDateTime.now());
        depositProductContract.setModifyDate(LocalDateTime.now());

        return depositProductContract;
    }

}
