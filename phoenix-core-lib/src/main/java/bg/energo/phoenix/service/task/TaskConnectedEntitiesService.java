package bg.energo.phoenix.service.task;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRun;
import bg.energo.phoenix.model.entity.billing.billingRun.BillingRunTasks;
import bg.energo.phoenix.model.entity.contract.contract.ProductContractTask;
import bg.energo.phoenix.model.entity.contract.contract.ServiceContractTask;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrder;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderTask;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrder;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderTask;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.service.ServiceContracts;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunication;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationTask;
import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunication;
import bg.energo.phoenix.model.entity.crm.smsCommunication.SmsCommunicationTasks;
import bg.energo.phoenix.model.entity.customer.Customer;
import bg.energo.phoenix.model.entity.customer.CustomerTask;
import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbg;
import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgTasks;
import bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationOfDisconnectionOfThePowerSupply;
import bg.energo.phoenix.model.entity.receivable.cancellationOfDisconnectionOfThePowerSupply.PowerSupplyDcnCancellationTask;
import bg.energo.phoenix.model.entity.receivable.customerAssessment.CustomerAssessment;
import bg.energo.phoenix.model.entity.receivable.customerAssessment.CustomerAssessmentTasks;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupply.DisconnectionOfPowerSupply;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupply.DisconnectionPowerSupplyTask;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequests;
import bg.energo.phoenix.model.entity.receivable.disconnectionPowerSupplyRequests.DisconnectionPowerSupplyRequestsTasks;
import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFine;
import bg.energo.phoenix.model.entity.receivable.latePaymentFine.LatePaymentFineTask;
import bg.energo.phoenix.model.entity.receivable.massOperationForBlocking.ReceivableBlocking;
import bg.energo.phoenix.model.entity.receivable.massOperationForBlocking.ReceivableBlockingTask;
import bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator;
import bg.energo.phoenix.model.entity.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorTask;
import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminder;
import bg.energo.phoenix.model.entity.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderTasks;
import bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply.ReconnectionOfThePowerSupply;
import bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply.ReconnectionOfThePowerSupplyTasks;
import bg.energo.phoenix.model.entity.receivable.rescheduling.Rescheduling;
import bg.energo.phoenix.model.entity.receivable.rescheduling.ReschedulingTasks;
import bg.energo.phoenix.model.entity.task.Task;
import bg.energo.phoenix.model.enums.contract.products.ProductContractStatus;
import bg.energo.phoenix.model.enums.crm.smsCommunication.SmsCommunicationChannel;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.receivable.massOperationForBlocking.ReceivableSubObjectStatus;
import bg.energo.phoenix.model.enums.task.ConnectedEntityType;
import bg.energo.phoenix.model.request.task.TaskConnectedEntity;
import bg.energo.phoenix.model.response.task.TaskConnectedEntityResponse;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunRepository;
import bg.energo.phoenix.repository.billing.billingRun.BillingRunTasksRepository;
import bg.energo.phoenix.repository.contract.contract.ProductContractTaskRepository;
import bg.energo.phoenix.repository.contract.contract.ServiceContractTaskRepository;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderRepository;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderTaskRepository;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderRepository;
import bg.energo.phoenix.repository.contract.order.service.ServiceOrderTaskRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationRepository;
import bg.energo.phoenix.repository.crm.emailCommunication.EmailCommunicationTaskRepository;
import bg.energo.phoenix.repository.crm.smsCommunication.SmsCommunicationRepository;
import bg.energo.phoenix.repository.crm.smsCommunication.SmsCommunicationTasksRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.customer.CustomerTaskRepository;
import bg.energo.phoenix.repository.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgRepository;
import bg.energo.phoenix.repository.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgTaskRepository;
import bg.energo.phoenix.repository.receivable.cancellationOfDisconnectionOfThePowerSupply.CancellationOfDisconnectionOfThePowerSupplyRepository;
import bg.energo.phoenix.repository.receivable.cancellationOfDisconnectionOfThePowerSupply.PowerSupplyDcnCancellationTaskRepository;
import bg.energo.phoenix.repository.receivable.customerAssessment.CustomerAssessmentRepository;
import bg.energo.phoenix.repository.receivable.customerAssessment.CustomerAssessmentTasksRepository;
import bg.energo.phoenix.repository.receivable.disconnectionPowerSupply.DisconnectionPowerSupplyRepository;
import bg.energo.phoenix.repository.receivable.disconnectionPowerSupply.DisconnectionPowerSupplyTaskRepository;
import bg.energo.phoenix.repository.receivable.disconnectionPowerSupplyRequest.DisconnectionPowerSupplyRequestRepository;
import bg.energo.phoenix.repository.receivable.disconnectionPowerSupplyRequest.DisconnectionPowerSupplyRequestsTasksRepository;
import bg.energo.phoenix.repository.receivable.latePaymentFine.LatePaymentFineRepository;
import bg.energo.phoenix.repository.receivable.latePaymentFine.LatePaymentFineTaskRepository;
import bg.energo.phoenix.repository.receivable.massOperationForBlocking.ReceivableBlockingRepository;
import bg.energo.phoenix.repository.receivable.massOperationForBlocking.ReceivableBlockingTaskRepository;
import bg.energo.phoenix.repository.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository;
import bg.energo.phoenix.repository.receivable.objectionWithdrawalToChangeOfABalancingGroupCoordinator.ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorTaskRepository;
import bg.energo.phoenix.repository.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderRepository;
import bg.energo.phoenix.repository.receivable.powerSupplyDisconnectionReminder.PowerSupplyDisconnectionReminderTasksRepository;
import bg.energo.phoenix.repository.receivable.reconnectionOfPowerSupply.ReconnectionOfThePowerSupplyRepository;
import bg.energo.phoenix.repository.receivable.reconnectionOfPowerSupply.ReconnectionOfThePowerSupplyTasksRepository;
import bg.energo.phoenix.repository.receivable.rescheduling.ReschedulingRepository;
import bg.energo.phoenix.repository.receivable.rescheduling.ReschedulingTasksRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskConnectedEntitiesService {
    private final ProductContractRepository productContractRepository;
    private final ProductContractTaskRepository productContractTaskRepository;
    private final ServiceContractsRepository serviceContractsRepository;
    private final ServiceContractTaskRepository serviceContractTaskRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final ServiceOrderTaskRepository serviceOrderTaskRepository;
    private final GoodsOrderRepository goodsOrderRepository;
    private final GoodsOrderTaskRepository goodsOrderTaskRepository;
    private final CustomerRepository customerRepository;
    private final CustomerTaskRepository customerTaskRepository;
    private final BillingRunRepository billingRunRepository;
    private final BillingRunTasksRepository billingRunTasksRepository;
    private final ReceivableBlockingRepository receivableBlockingRepository;
    private final ReceivableBlockingTaskRepository receivableBlockingTaskRepository;
    private final CustomerAssessmentRepository customerAssessmentRepository;
    private final CustomerAssessmentTasksRepository customerAssessmentTasksRepository;
    private final EmailCommunicationRepository emailCommunicationRepository;
    private final EmailCommunicationTaskRepository emailCommunicationTaskRepository;
    private final SmsCommunicationRepository smsCommunicationRepository;
    private final SmsCommunicationTasksRepository smsCommunicationTasksRepository;
    private final DisconnectionPowerSupplyRequestRepository disconnectionPowerSupplyRequestRepository;
    private final DisconnectionPowerSupplyRequestsTasksRepository disconnectionPowerSupplyRequestsTasksRepository;
    private final ReconnectionOfThePowerSupplyRepository reconnectionOfThePowerSupplyRepository;
    private final ReconnectionOfThePowerSupplyTasksRepository reconnectionOfThePowerSupplyTasksRepository;
    private final CancellationOfDisconnectionOfThePowerSupplyRepository cancellationOfDisconnectionOfThePowerSupplyRepository;
    private final PowerSupplyDcnCancellationTaskRepository powerSupplyDcnCancellationTaskRepository;
    private final DisconnectionPowerSupplyRepository disconnectionPowerSupplyRepository;
    private final DisconnectionPowerSupplyTaskRepository disconnectionPowerSupplyTaskRepository;
    private final ReschedulingRepository reschedulingRepository;
    private final ReschedulingTasksRepository reschedulingTasksRepository;
    private final LatePaymentFineTaskRepository latePaymentFineTaskRepository;
    private final LatePaymentFineRepository latePaymentFineRepository;
    private final ObjectionToChangeOfCbgRepository objectionToChangeOfCbgRepository;
    private final ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository objectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository;
    private final ObjectionToChangeOfCbgTaskRepository objectionToChangeOfCbgTaskRepository;
    private final ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorTaskRepository objectionWithdrawalToAChangeOfABalancingGroupCoordinatorTaskRepository;
    private final PowerSupplyDisconnectionReminderRepository powerSupplyDisconnectionReminderRepository;
    private final PowerSupplyDisconnectionReminderTasksRepository powerSupplyDisconnectionReminderTasksRepository;

    @Transactional
    public void createTaskCustomers(int index, Task task, Long customerId, List<String> exceptionMessages) {
        Optional<Customer> customerOptional = customerRepository.findByIdAndStatuses(customerId, List.of(CustomerStatus.ACTIVE));
        if (customerOptional.isEmpty()) {
            exceptionMessages.add("connectedEntities[%s].id-Customer with presented id [%s] not found;".formatted(index, customerId));
        } else {
            customerTaskRepository.save(new CustomerTask(null, task.getId(), customerOptional.get().getId(), EntityStatus.ACTIVE));
        }
    }

    @Transactional
    public void createTaskCustomerAssessment(int index, Task task, Long customerAssessmentId, List<String> exceptionMessages) {
        Optional<CustomerAssessment> customerAssessmentOptional = customerAssessmentRepository.findByIdAndStatus(customerAssessmentId, EntityStatus.ACTIVE);
        if (customerAssessmentOptional.isEmpty()) {
            exceptionMessages.add("connectedEntities[%s].id-Customer assessment with presented id [%s] not found;".formatted(index, customerAssessmentId));
        } else {
            customerAssessmentTasksRepository.save(new CustomerAssessmentTasks(null, customerAssessmentOptional.get().getId(), task.getId(), EntityStatus.ACTIVE));
        }
    }

    @Transactional
    public void createTaskProductContract(int index, Task task, Long productContractId, List<String> exceptionMessages) {
        Optional<ProductContract> productContractOptional = productContractRepository.findByIdAndStatusIn(productContractId, List.of(ProductContractStatus.ACTIVE));
        if (productContractOptional.isEmpty()) {
            exceptionMessages.add("connectedEntities[%s].id-Product Contract with presented id [%s] not found;".formatted(index, productContractId));
        } else {
            productContractTaskRepository
                    .save(new ProductContractTask(null, productContractOptional.get().getId(), task.getId(), EntityStatus.ACTIVE));
        }
    }

    @Transactional
    public void createTaskServiceContract(int index, Task task, Long serviceContractId, List<String> exceptionMessages) {
        Optional<ServiceContracts> serviceContractOptional = serviceContractsRepository.findByIdAndStatusIn(serviceContractId, List.of(EntityStatus.ACTIVE));
        if (serviceContractOptional.isEmpty()) {
            exceptionMessages.add("connectedEntities[%s].id-Service Contract with presented id [%s] not found;".formatted(index, serviceContractId));
        } else {
            serviceContractTaskRepository
                    .save(new ServiceContractTask(null, serviceContractOptional.get().getId(), task.getId(), EntityStatus.ACTIVE));
        }
    }

    @Transactional
    public void createTaskServiceOrder(int index, Task task, Long serviceOrderId, List<String> exceptionMessages) {
        Optional<ServiceOrder> serviceOrderOptional = serviceOrderRepository.findByIdAndStatusIn(serviceOrderId, List.of(EntityStatus.ACTIVE));
        if (serviceOrderOptional.isEmpty()) {
            exceptionMessages.add("connectedEntities[%s].id-Service Order with presented id [%s] not found;".formatted(index, serviceOrderId));
        } else {
            serviceOrderTaskRepository
                    .save(new ServiceOrderTask(null, serviceOrderOptional.get().getId(), task.getId(), EntityStatus.ACTIVE));
        }
    }

    @Transactional
    public void createTaskGoodsOrder(int index, Task task, Long goodsOrderId, List<String> exceptionMessages) {
        Optional<GoodsOrder> goodsOrderOptional = goodsOrderRepository.findByIdAndStatusIn(goodsOrderId, List.of(EntityStatus.ACTIVE));
        if (goodsOrderOptional.isEmpty()) {
            exceptionMessages.add("connectedEntities[%s].id-Goods Order with presented id [%s] not found;".formatted(index, goodsOrderId));
        } else {
            goodsOrderTaskRepository
                    .save(new GoodsOrderTask(null, goodsOrderOptional.get().getId(), task.getId(), EntityStatus.ACTIVE));
        }
    }

    @Transactional
    public void createTaskBillingRun(Task task, List<TaskConnectedEntity> connectedEntities, List<String> exceptionMessages) {
        Long taskId = task.getId();

        List<Long> connectedBillingIds = connectedEntities
                .stream()
                .filter(entity -> entity.getEntityType().equals(ConnectedEntityType.BILLING))
                .map(TaskConnectedEntity::getId)
                .toList();

        if (CollectionUtils.isNotEmpty(connectedBillingIds)) {
            List<BillingRun> billingRuns = billingRunRepository.findAllById(connectedBillingIds);

            List<Long> existingBillingIds = billingRuns
                    .stream()
                    .map(BillingRun::getId)
                    .toList();

            List<Long> nonMatchingBillingIds = connectedBillingIds
                    .stream()
                    .filter(id -> !existingBillingIds.contains(id))
                    .toList();

            nonMatchingBillingIds
                    .forEach(
                            id -> exceptionMessages.add("connectedEntities[%s].id-BillingRun with presented id [%s] not found;".formatted(connectedBillingIds.indexOf(id), id))
                    );

            if (nonMatchingBillingIds.isEmpty()) {
                billingRunTasksRepository.saveAll(billingRuns
                        .stream()
                        .map(billingRun -> new BillingRunTasks(null, billingRun.getId(), taskId, EntityStatus.ACTIVE))
                        .toList()
                );
            }

        } else {
            throw new ClientException("Connected entity ids is empty", ErrorCode.APPLICATION_ERROR);
        }
    }

    @Transactional
    public void createTaskReceivableBlocking(int index, Task task, Long receivableBlockingId, List<String> exceptionMessages) {
        Optional<ReceivableBlocking> receivableBlocking = receivableBlockingRepository.findByIdAndStatus(receivableBlockingId, EntityStatus.ACTIVE);
        if (receivableBlocking.isEmpty()) {
            exceptionMessages.add("connectedEntities[%s].id-Receivable blocking with presented id [%s] not found;".formatted(index, receivableBlockingId));
        } else {
            receivableBlockingTaskRepository.save(
                    new ReceivableBlockingTask(
                            null,
                            receivableBlocking.get().getId(),
                            task.getId(),
                            ReceivableSubObjectStatus.ACTIVE
                    )
            );
        }
    }

    @Transactional
    public void createTaskReconnectionOfThePowerSupply(int index, Task task, Long reconnectionId, List<String> exceptionMessages) {
        Optional<ReconnectionOfThePowerSupply> reconnection = reconnectionOfThePowerSupplyRepository.findByIdAndStatus(reconnectionId, EntityStatus.ACTIVE);
        if (reconnection.isEmpty()) {
            exceptionMessages.add("connectedEntities[%s].id-Reconnection with presented id [%s] not found;".formatted(index, reconnectionId));
        } else {
            var reconnectionTask = new ReconnectionOfThePowerSupplyTasks();
            reconnectionTask.setReconnectionId(reconnectionId);
            reconnectionTask.setTaskId(task.getId());
            reconnectionTask.setStatus(ReceivableSubObjectStatus.ACTIVE);
            reconnectionOfThePowerSupplyTasksRepository.save(reconnectionTask);
        }
    }

    @Transactional
    public void createTaskCancellationOfRequestForDisconnectionOfPws(int index, Task task, Long cancellationId, List<String> exceptionMessages) {
        Optional<CancellationOfDisconnectionOfThePowerSupply> cancellation = cancellationOfDisconnectionOfThePowerSupplyRepository.findByIdAndEntityStatus(cancellationId, EntityStatus.ACTIVE);
        if (cancellation.isEmpty()) {
            exceptionMessages.add("connectedEntities[%s].id-Cancellation with presented id [%s] not found;".formatted(index, cancellationId));
        } else {
            PowerSupplyDcnCancellationTask cancellationTask = new PowerSupplyDcnCancellationTask();
            cancellationTask.setPowerSupplyDcnCancellationId(cancellationId);
            cancellationTask.setTaskId(task.getId());
            cancellationTask.setStatus(EntityStatus.ACTIVE);
            powerSupplyDcnCancellationTaskRepository.save(cancellationTask);
        }
    }

    @Transactional
    public void createTaskDisconnectionOfPowerSupply(int index, Task task, Long disconnectionId, List<String> exceptionMessages) {
        Optional<DisconnectionOfPowerSupply> disconnection = disconnectionPowerSupplyRepository.findByIdAndStatus(disconnectionId, EntityStatus.ACTIVE);
        if (disconnection.isEmpty()) {
            exceptionMessages.add("connectedEntities[%s].id-Disconnection with presented id [%s] not found;".formatted(index, disconnectionId));
        } else {
            DisconnectionPowerSupplyTask disconnectionTask = new DisconnectionPowerSupplyTask();
            disconnectionTask.setPowerSupplyDisconnectionId(disconnectionId);
            disconnectionTask.setTaskId(task.getId());
            disconnectionTask.setStatus(EntityStatus.ACTIVE);
            disconnectionPowerSupplyTaskRepository.save(disconnectionTask);
        }
    }

    @Transactional
    public void createTaskRescheduling(int index, Task task, Long reschedulingId, List<String> exceptionMessages) {
        Optional<Rescheduling> rescheduling = reschedulingRepository.findByIdAndStatus(reschedulingId, EntityStatus.ACTIVE);
        if (rescheduling.isEmpty()) {
            exceptionMessages.add("connectedEntities[%s].id-Rescheduling with presented id [%s] not found;".formatted(index, reschedulingId));
        } else {
            ReschedulingTasks reschedulingTasks = new ReschedulingTasks();
            reschedulingTasks.setReschedulingId(reschedulingId);
            reschedulingTasks.setTaskId(task.getId());
            reschedulingTasks.setStatus(ReceivableSubObjectStatus.ACTIVE);
            reschedulingTasksRepository.save(reschedulingTasks);
        }
    }

    @Transactional
    public void createTaskEmailCommunication(int index, Task task, Long emailCommunicationId, List<String> exceptionMessages) {
        Optional<EmailCommunication> emailCommunicationOptional = emailCommunicationRepository.findByIdAndEntityStatus(emailCommunicationId, EntityStatus.ACTIVE);
        if (emailCommunicationOptional.isEmpty()) {
            exceptionMessages.add("connectedEntities[%s].id-Email communication with presented id [%s] not found;".formatted(index, emailCommunicationId));
        } else {
            emailCommunicationTaskRepository.save(
                    new EmailCommunicationTask(
                            null,
                            emailCommunicationOptional.get().getId(),
                            task.getId(),
                            EntityStatus.ACTIVE
                    )
            );
        }
    }

    @Transactional
    public void createTaskSmsCommunication(int index, Task task, Long smsCommunicationId, List<String> exceptionMessages, SmsCommunicationChannel communicationChannel) {
        Optional<SmsCommunication> smsCommunicationOptional;
        if (communicationChannel.equals(SmsCommunicationChannel.SMS)) {
            smsCommunicationOptional = smsCommunicationRepository.findBySmsCommunicationCustomerWithActiveStatus(smsCommunicationId);
            if (smsCommunicationOptional.isPresent() && smsCommunicationOptional.get().getCommunicationChannel().equals(SmsCommunicationChannel.MASS_SMS)) {
                exceptionMessages.add("It is not possible to add to add task on sms created with  MASS sms;");
            }
        } else {
            smsCommunicationOptional = smsCommunicationRepository.findByIdAndStatusAndCommunicationChannel(smsCommunicationId, EntityStatus.ACTIVE, SmsCommunicationChannel.MASS_SMS);
        }
        if (smsCommunicationOptional.isEmpty()) {
            exceptionMessages.add("connectedEntities[%s].id-SMS communication with presented id [%s] not found;".formatted(index, smsCommunicationId));
        } else {
            SmsCommunicationTasks smsCommunicationTasks = new SmsCommunicationTasks();
            smsCommunicationTasks.setSmsCommunicationId(smsCommunicationOptional.get().getId());
            smsCommunicationTasks.setTaskId(task.getId());
            smsCommunicationTasks.setStatus(EntityStatus.ACTIVE);
            smsCommunicationTasksRepository.save(smsCommunicationTasks);
        }
    }

    @Transactional
    public void createTaskDisconnectionPowerSupplyRequest(int index, Task task, Long disconnectionPowerSupplyRequestId, List<String> exceptionMessages) {
        Optional<DisconnectionPowerSupplyRequests> disconnectionPowerSupplyRequests = disconnectionPowerSupplyRequestRepository.findByIdAndStatus(
                disconnectionPowerSupplyRequestId,
                EntityStatus.ACTIVE
        );
        if (disconnectionPowerSupplyRequests.isEmpty()) {
            exceptionMessages.add("connectedEntities[%s].id-disconnection Power Supply Request with presented id [%s] not found;".formatted(index, disconnectionPowerSupplyRequestId));
        } else {
            disconnectionPowerSupplyRequestsTasksRepository.save(
                    new DisconnectionPowerSupplyRequestsTasks(
                            null,
                            task.getId(),
                            disconnectionPowerSupplyRequests.get().getId(),
                            EntityStatus.ACTIVE
                    )
            );
        }
    }

    @Transactional
    public void createTaskLatePaymentFine(int index, Task task, Long latePaymentFineId, List<String> exceptionMessages) {
        Optional<LatePaymentFine> latePaymentFineOptional = latePaymentFineRepository.findById(latePaymentFineId);
        if (latePaymentFineOptional.isEmpty()) {
            exceptionMessages.add("connectedEntities[%s].id-Late payment fine with presented id [%s] not found;".formatted(index, latePaymentFineId));
        } else {
            LatePaymentFineTask latePaymentFineTask = new LatePaymentFineTask();
            latePaymentFineTask.setLatePaymentFineId(latePaymentFineId);
            latePaymentFineTask.setTaskId(task.getId());
            latePaymentFineTask.setStatus(ReceivableSubObjectStatus.ACTIVE);
            latePaymentFineTaskRepository.save(latePaymentFineTask);
        }
    }

    @Transactional
    public void createTaskObjectionToChangeOfCbg(int index, Task task, Long objectionToChangeOfCbgId, List<String> exceptionMessages) {
        Optional<ObjectionToChangeOfCbg> objectionToChangeOfCbgOptional = objectionToChangeOfCbgRepository.findByIdAndStatusIn(
                objectionToChangeOfCbgId,
                List.of(EntityStatus.ACTIVE)
        );
        if (objectionToChangeOfCbgOptional.isEmpty()) {
            exceptionMessages.add("connectedEntities[%s].id-Objection change of balancing group coordinator with presented id [%s] not found;".formatted(index, objectionToChangeOfCbgId));
        } else {
            ObjectionToChangeOfCbgTasks objectionToChangeOfCbgTask = new ObjectionToChangeOfCbgTasks();
            objectionToChangeOfCbgTask.setChangeOfCbgId(objectionToChangeOfCbgId);
            objectionToChangeOfCbgTask.setTaskId(task.getId());
            objectionToChangeOfCbgTask.setStatus(EntityStatus.ACTIVE);
            objectionToChangeOfCbgTaskRepository.save(objectionToChangeOfCbgTask);
        }
    }

    @Transactional
    public void createTaskObjectionWithdrawalToChangeOfCbg(int index, Task task, Long objectionWithdrawalToChangeOfCbgId, List<String> exceptionMessages) {
        Optional<ObjectionWithdrawalToAChangeOfABalancingGroupCoordinator> objectionWithdrawalToChangeOfCbgOptional = objectionWithdrawalToAChangeOfABalancingGroupCoordinatorRepository.findByIdAndStatus(
                objectionWithdrawalToChangeOfCbgId,
                EntityStatus.ACTIVE
        );
        if (objectionWithdrawalToChangeOfCbgOptional.isEmpty()) {
            exceptionMessages.add("connectedEntities[%s].id-Objection withdrawal change of balancing group coordinator with presented id [%s] not found;".formatted(index, objectionWithdrawalToChangeOfCbgId));
        } else {
            ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorTask objectionWithdrawalToChangeOfCbgTask = new ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorTask();
            objectionWithdrawalToChangeOfCbgTask.setWithdrawalChangeOfCbgId(objectionWithdrawalToChangeOfCbgId);
            objectionWithdrawalToChangeOfCbgTask.setTaskId(task.getId());
            objectionWithdrawalToChangeOfCbgTask.setStatus(ReceivableSubObjectStatus.ACTIVE);
            objectionWithdrawalToAChangeOfABalancingGroupCoordinatorTaskRepository.save(objectionWithdrawalToChangeOfCbgTask);
        }
    }

    @Transactional
    public void createTaskReminderForDisconnectionOfPowerSupply(int index, Task task, Long reminderForDisconnectionOfPwsId, List<String> exceptionMessages) {
        Optional<PowerSupplyDisconnectionReminder> powerSupplyDisconnectionReminderOptional = powerSupplyDisconnectionReminderRepository.findByIdAndGeneralStatuses(
                reminderForDisconnectionOfPwsId,
                List.of(EntityStatus.ACTIVE)
        );
        if (powerSupplyDisconnectionReminderOptional.isEmpty()) {
            exceptionMessages.add("connectedEntities[%s].id-Power supply disconnection reminder with presented id [%s] not found;".formatted(index, reminderForDisconnectionOfPwsId));
        } else {
            PowerSupplyDisconnectionReminderTasks powerSupplyDisconnectionReminderTasks = new PowerSupplyDisconnectionReminderTasks();
            powerSupplyDisconnectionReminderTasks.setReminderId(reminderForDisconnectionOfPwsId);
            powerSupplyDisconnectionReminderTasks.setTaskId(task.getId());
            powerSupplyDisconnectionReminderTasks.setStatus(ReceivableSubObjectStatus.ACTIVE);
            powerSupplyDisconnectionReminderTasksRepository.save(powerSupplyDisconnectionReminderTasks);
        }
    }

    public List<TaskConnectedEntityResponse> fetchTasksForObjectionWithdrawalToChangeOfCbg(Long taskId) {
        return objectionWithdrawalToAChangeOfABalancingGroupCoordinatorTaskRepository
                .findAllConnectedToObjectionWithdrawalToAChangeOfCbg(taskId)
                .stream()
                .peek(it -> it.setEntityType(ConnectedEntityType.OBJECTION_WITHDRAWAL_TO_CHANGE_OF_CBG))
                .toList();
    }

    public List<TaskConnectedEntityResponse> fetchTasksForObjectionToChangeOfCbg(Long taskId) {
        return objectionToChangeOfCbgTaskRepository
                .findAllConnectedToObjectionChangeOfCbg(taskId)
                .stream()
                .peek(it -> it.setEntityType(ConnectedEntityType.OBJECTION_TO_CHANGE_OF_CBG))
                .toList();
    }

    public List<TaskConnectedEntityResponse> fetchTasksForRescheduling(Long taskId) {
        return reschedulingTasksRepository
                .findAllConnectedToRescheduling(taskId)
                .stream()
                .peek(it -> it.setEntityType(ConnectedEntityType.RESCHEDULING))
                .toList();
    }

    public List<TaskConnectedEntityResponse> fetchTasksForReminderForDisconnectionOfPws(Long taskId) {
        return powerSupplyDisconnectionReminderTasksRepository
                .findAllConnectedToPowerSupplyDisconnectionReminder(taskId)
                .stream()
                .peek(it -> it.setEntityType(ConnectedEntityType.REMINDER_FOR_DISCONNECTION_OF_PWS))
                .toList();
    }

    public List<TaskConnectedEntityResponse> fetchTasksForMassOperationOfBlocking(Long taskId) {
        return receivableBlockingTaskRepository
                .findAllConnectedReceivableBlocking(taskId)
                .stream()
                .peek(it -> it.setEntityType(ConnectedEntityType.RECEIVABLE_BLOCKING))
                .toList();
    }

    public List<TaskConnectedEntityResponse> fetchTasksForDisconnectionOfPowerSupplyRequests(Long taskId) {
        return disconnectionPowerSupplyRequestsTasksRepository
                .findAllConnectedDisconnectionPowerSupplyRequests(taskId)
                .stream()
                .peek(it -> it.setEntityType(ConnectedEntityType.DISCONNECTION_POWER_SUPPLY_REQUESTS))
                .toList();
    }

    public List<TaskConnectedEntityResponse> fetchTasksForReconnectionOfThePowerSupply(Long taskId) {
        return reconnectionOfThePowerSupplyTasksRepository.findAllConnectedReconnectionPowerSupplys(taskId)
                .stream()
                .peek(it -> it.setEntityType(ConnectedEntityType.RECONNECTION_OF_POWER_SUPPLY))
                .toList();
    }

    public List<TaskConnectedEntityResponse> fetchTasksForPowerSupplyDisconnectionCancel(Long taskId) {
        return powerSupplyDcnCancellationTaskRepository.findAllConnectedCancellations(taskId)
                .stream()
                .peek(it -> it.setEntityType(ConnectedEntityType.CANCELLATION_OF_REQUEST_FOR_DISCONNECTION_OF_PWS))
                .toList();
    }

    public List<TaskConnectedEntityResponse> fetchTasksForDisconnectionOfPowerSupply(Long taskId) {
        return disconnectionPowerSupplyTaskRepository.findAllConnectedDisconnections(taskId)
                .stream()
                .peek(it -> it.setEntityType(ConnectedEntityType.DISCONNECTION_OF_POWER_SUPPLY))
                .toList();
    }

    public List<TaskConnectedEntityResponse> fetchTasksForCustomerAssessment(Long taskId) {
        return customerAssessmentTasksRepository
                .findAllConnectedCustomerAssessmentsMapToResponse(taskId)
                .stream()
                .peek(taskConnectedEntityResponse -> taskConnectedEntityResponse.setEntityType(ConnectedEntityType.CUSTOMER_ASSESSMENT))
                .toList();
    }

    public List<TaskConnectedEntityResponse> fetchTasksForCustomer(Long taskId) {
        return customerTaskRepository
                .findAllConnectedCustomersMapToResponse(taskId)
                .stream()
                .peek(taskConnectedEntityResponse -> taskConnectedEntityResponse.setEntityType(ConnectedEntityType.CUSTOMER))
                .toList();
    }

    public List<TaskConnectedEntityResponse> fetchTasksForProductContract(Long taskId) {
        return productContractTaskRepository
                .findAllConnectedContracts(taskId)
                .stream()
                .peek(taskConnectedEntityResponse -> taskConnectedEntityResponse.setEntityType(ConnectedEntityType.PRODUCT_CONTRACT))
                .toList();
    }

    public List<TaskConnectedEntityResponse> fetchTasksForServiceContract(Long taskId) {
        return serviceContractTaskRepository
                .findAllConnectedContracts(taskId)
                .stream()
                .peek(taskConnectedEntityResponse -> taskConnectedEntityResponse.setEntityType(ConnectedEntityType.SERVICE_CONTRACT))
                .toList();
    }

    public List<TaskConnectedEntityResponse> fetchTasksForServiceOrder(Long taskId) {
        return serviceOrderTaskRepository
                .findAllConnectedOrders(taskId)
                .stream()
                .peek(taskConnectedEntityResponse -> taskConnectedEntityResponse.setEntityType(ConnectedEntityType.SERVICE_ORDER))
                .toList();
    }

    public List<TaskConnectedEntityResponse> fetchTasksForGoodsOrder(Long taskId) {
        return goodsOrderTaskRepository
                .findAllConnectedOrders(taskId)
                .stream()
                .peek(taskConnectedEntityResponse -> taskConnectedEntityResponse.setEntityType(ConnectedEntityType.GOODS_ORDER))
                .toList();
    }

    public List<TaskConnectedEntityResponse> fetchTasksForSmsCommunication(Long taskId) {
        return smsCommunicationTasksRepository
                .findAllConnectedSmsCommunicationSingleSms(taskId)
                .stream()
                .peek(taskConnectedEntityResponse -> taskConnectedEntityResponse.setEntityType(ConnectedEntityType.SMS_COMMUNICATION))
                .toList();
    }

    public List<TaskConnectedEntityResponse> fetchTasksForMassSmsCommunication(Long taskId) {
        return smsCommunicationTasksRepository
                .findAllConnectedSmsCommunication(taskId, SmsCommunicationChannel.MASS_SMS)
                .stream()
                .peek(taskConnectedEntityResponse -> taskConnectedEntityResponse.setEntityType(ConnectedEntityType.MASS_SMS_COMMUNICATION))
                .toList();
    }

    public List<TaskConnectedEntityResponse> fetchTasksForEmailCommunication(Long taskId) {
        return emailCommunicationTaskRepository
                .findAllConnectedEmailCommunication(taskId)
                .stream()
                .peek(taskConnectedEntityResponse -> taskConnectedEntityResponse.setEntityType(ConnectedEntityType.EMAIL_COMMUNICATION))
                .toList();
    }

    public List<TaskConnectedEntityResponse> fetchTasksForBillingRun(Long taskId) {
        return billingRunTasksRepository
                .findAllConnectedBillings(taskId)
                .stream()
                .peek(taskConnectedEntityResponse -> taskConnectedEntityResponse.setEntityType(ConnectedEntityType.BILLING))
                .toList();
    }

    public List<TaskConnectedEntityResponse> fetchTasksForLatePaymentFine(Long taskId) {
        return latePaymentFineTaskRepository
                .findAllConnectedLatePayments(taskId)
                .stream()
                .peek(it -> it.setEntityType(ConnectedEntityType.LATE_PAYMENT_FINE))
                .toList();
    }

    public ObjectionToChangeOfCbgTasks fetchTaskForObjectionToChangeOfCbg(Long taskId, Long objectionId, EntityStatus status) {
        return objectionToChangeOfCbgTaskRepository
                .findByTaskIdAndChangeOfCbgIdAndStatus(taskId, objectionId, status)
                .orElseThrow(() -> new DomainEntityNotFoundException("Task is not assigned to current objection to a change of cbg"));
    }

    public ObjectionWithdrawalToAChangeOfABalancingGroupCoordinatorTask fetchTaskForWithdrawalObjectionToChangeOfCbg(Long taskId, Long withdrawalChangeOfCbgId, ReceivableSubObjectStatus status) {
        return objectionWithdrawalToAChangeOfABalancingGroupCoordinatorTaskRepository
                .findByTaskIdAndWithdrawalChangeOfCbgIdAndStatus(taskId, withdrawalChangeOfCbgId, status)
                .orElseThrow(() -> new DomainEntityNotFoundException("Task is not assigned to current objection to a change of cbg withdrawal"));
    }

    public PowerSupplyDisconnectionReminderTasks fetchTaskForPowerSupplyDisconnectionReminder(Long taskId, Long withdrawalChangeOfCbgId, ReceivableSubObjectStatus status) {
        return powerSupplyDisconnectionReminderTasksRepository
                .findByTaskIdAndReminderIdAndStatus(taskId, withdrawalChangeOfCbgId, status)
                .orElseThrow(() -> new DomainEntityNotFoundException("Task is not assigned to current power supply disconnection reminder"));
    }

    public LatePaymentFineTask fetchTaskForLatePaymentFine(Long taskId, Long latePaymentFineId, ReceivableSubObjectStatus status) {
        return latePaymentFineTaskRepository
                .findByTaskIdAndLatePaymentFineIdAndStatus(taskId, latePaymentFineId, status)
                .orElseThrow(() -> new DomainEntityNotFoundException("Task is not assigned to current late payment fine"));
    }
}
