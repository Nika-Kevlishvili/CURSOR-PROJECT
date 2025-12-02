package bg.energo.phoenix.service.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.entity.nomenclature.receivable.BalancingGroupCoordinatorGround;
import bg.energo.phoenix.model.entity.nomenclature.receivable.GroundForObjectionWithdrawalToChangeOfACbg;
import bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator;
import bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToAChangeOfABalancingGroupProcessResult;
import bg.energo.phoenix.model.enums.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToChangeOfCbgStatus;
import bg.energo.phoenix.model.response.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ProcessResultResponse;
import bg.energo.phoenix.repository.nomenclature.receivable.BalancingGroupCoordinatorGroundRepository;
import bg.energo.phoenix.repository.nomenclature.receivable.GroundForObjectionWithdrawalToChangeOfACbgRepository;
import bg.energo.phoenix.repository.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository;
import bg.energo.phoenix.repository.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToAChangeOfABalancingGroupProcessResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ObjectionWithdrawalToAChangeOfACbgProcessService {
    private final ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository withdrawalRepository;
    private final ObjectionWithdrawalToAChangeOfABalancingGroupProcessResultRepository processResultRepository;
    private final GroundForObjectionWithdrawalToChangeOfACbgRepository groundForObjectionWithdrawalToChangeOfACbgRepository;
    private final BalancingGroupCoordinatorGroundRepository balancingGroupCoordinatorGroundRepository;
    @Transactional
    public void startProcess(Long objectionToCbgId, ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator withdrawal) {
        GroundForObjectionWithdrawalToChangeOfACbg groundForObjectionWithdrawalToChangeOfACbg = groundForObjectionWithdrawalToChangeOfACbgRepository.findByDefaultSelectionTrue()
                .orElseThrow(() -> new DomainEntityNotFoundException("Ground for objection withdrawal default nomenclature not found!"));
        BalancingGroupCoordinatorGround balancingGroupCoordinatorGround = balancingGroupCoordinatorGroundRepository.findByDefaultSelectionTrue().
                orElseThrow(() -> new DomainEntityNotFoundException("Balancing group for objection withdrawal default nomenclature not found!"));

        List<ProcessResultResponse> processResultResponses = withdrawalRepository.calculate(objectionToCbgId,withdrawal.getId(), withdrawalRepository.existsByChangeOfCbgIdAndNotWithdrawalId(withdrawal.getChangeOfCbgId(), withdrawal.getId()));
        List<ObjectionWithdrawalToAChangeOfABalancingGroupProcessResult> processResults =
                processResultResponses.stream().map(y-> new ObjectionWithdrawalToAChangeOfABalancingGroupProcessResult(y, withdrawal.getId(),balancingGroupCoordinatorGround.getId(),groundForObjectionWithdrawalToChangeOfACbg.getId())).toList();
        processResultRepository.saveAll(processResults);
        withdrawal.setWithdrawalToChangeOfCbgStatus(ObjectionWithdrawalToChangeOfCbgStatus.DRAFT);
    }

}
