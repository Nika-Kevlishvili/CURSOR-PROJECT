package bg.energo.migration.customer.service;

import bg.energo.migration.customer.dto.CustomerRequestEntry;
import bg.energo.migration.customer.entity.CustomerGeneralData;
import bg.energo.migration.customer.entity.communication.CustomerCommunicationView;
import bg.energo.migration.customer.entity.communication.contactpurpose.CustomerCommContactPurposeView;
import bg.energo.migration.customer.entity.communication.contacts.CustomerCommContactView;
import bg.energo.migration.customer.entity.segment.CustomerSegmentView;
import bg.energo.migration.customer.mapper.CustomerRequestMapper;
import bg.energo.migration.customer.models.CustomerManagerImportResposne;
import bg.energo.migration.customer.models.CustomerManagerViewMiddleResponse;
import bg.energo.migration.customer.models.CustomerManagerViewResponse;
import bg.energo.migration.customer.repository.*;
import bg.energo.migration.integration.phoenix.customer.model.enums.CustomerType;
import bg.energo.migration.integration.phoenix.customer.model.enums.Status;
import bg.energo.migration.integration.phoenix.customer.model.request.CreateCustomerRequest;
import com.google.common.util.concurrent.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessCustomerDataService {
    private final CustomerRequestMapper customerRequestMapper;
    private final CustomerMigrationService customerMigrationService;
    private final CustomerSegmentViewRepository customerSegmentViewRepository;
    private final CustomerGeneralDataRepository customerGeneralDataRepository;
    private final CustomerManagerViewRepository customerManagerViewRepository;
    private final CustomerCommContactViewRepository customerCommContactViewRepository;
    private final CustomerCommunicationViewRepository customerCommunicationViewRepository;
    private final CustomerAccountManagerViewRepository customerAccountManagerViewRepository;
    private final CustomerCommContactPurposeViewRepository customerCommContactPurposeViewRepository;
    private final CustomerManagerImportRepository customerManagerImportRepository;

    private List<CustomerCommContactPurposeView> contactPurposes;


    public void processCustomers() {
    }

    public void processCustomersV2() {
        log.info("Starting customer processing...");
        List<CustomerGeneralData> items = customerGeneralDataRepository.findAllByIdFromPhoenixIsNull();

        // Total threads
        int threadCount = 100;
        this.contactPurposes = customerCommContactPurposeViewRepository.findAll();
        // Create thread pool
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Calculate chunk size
        int chunkSize = (int) Math.ceil((double) items.size() / threadCount);

        for (int i = 0; i < items.size(); i += chunkSize) {
            int start = i;
            int end = Math.min(i + chunkSize, items.size());
            List<CustomerGeneralData> subList = items.subList(start, end);
            executor.submit(() -> processItems(subList));
        }

        // Graceful shutdown
        executor.shutdown();
        log.info("All customer processing is complete.");
    }

    private void processItems(List<CustomerGeneralData> items) {

        for (int i = 0; i < items.size(); i++) {
            CustomerGeneralData customer = items.get(i);

            CustomerCommunicationView communications = customerCommunicationViewRepository
                    .findAllByCustomerId(customer.getCustomerId());

            List<bg.energo.migration.customer.entity.accountmanager.CustomerAccountManagerView> accountManagers = customerAccountManagerViewRepository
                    .findAllByCustomerId(customer.getCustomerId());

           List<CustomerSegmentView> segments = customerSegmentViewRepository
                    .findAllByCustomerId(customer.getCustomerId());

           List<CustomerManagerViewResponse> customerManagers = customerManagerViewRepository
                    .findDistinctByPersonalNumber(customer.getCustomerId()).stream().map(CustomerManagerViewResponse::new).toList();

            /*if (customer.getCustomerType() == CustomerType.LEGAL_ENTITY && customerManagers.isEmpty()) {
                customerManagers = getImportedManagers(customer.getCustomerId());
            }*/

            List<CustomerCommContactView> contacts = customerCommContactViewRepository
                    .findAllByCustomerIdentifier(customer.getCustomerIdentifier());

            CreateCustomerRequest request = customerRequestMapper.map(
                    customer,
                    communications,
                    accountManagers,
                    segments,
                    customerManagers,
                    this.contactPurposes,
                    contacts
            );

            processRequest(new CustomerRequestEntry(customer.getCustomerId(), request));
        }

    }



    private void processRequest(CustomerRequestEntry customerRequestEntry) {
        try {
            customerMigrationService
                    .migrateCustomers(
                            customerRequestEntry.foreignId(),
                            customerRequestEntry.request().getCustomerIdentifier(),
                            customerRequestEntry.request()
                    );
        } catch (Exception e) {
            log.error("Error while migrating customer with foreignId: {}", customerRequestEntry.foreignId(), e);
        }
    }


    private List<CustomerManagerViewResponse> getImportedManagers(String customerId) {
        List<CustomerManagerViewResponse> result = new java.util.ArrayList<>();
        List<CustomerManagerImportResposne> managers = customerManagerImportRepository.getCustomerManagerImportByCustomer(customerId).stream().map(CustomerManagerImportResposne::new).toList();
        for (CustomerManagerImportResposne customerManager : managers) {
            CustomerManagerViewResponse manager = generateManagerFromImport(customerManager, 1, customerId);
            result.add(manager);
            if (customerManager.getPosition2() != null && !customerManager.getPosition2().isEmpty() && !customerManager.getPosition2().isBlank()) {
                CustomerManagerViewResponse manager2 = generateManagerFromImport(customerManager, 2, customerId);
                result.add(manager2);
            }
            if (customerManager.getPosition3() != null && !customerManager.getPosition3().isEmpty() && !customerManager.getPosition3().isBlank()) {
                CustomerManagerViewResponse manager3 = generateManagerFromImport(customerManager, 3, customerId);
                result.add(manager3);
            }
        }
        return result;
    }

    private CustomerManagerViewResponse generateManagerFromImport(CustomerManagerImportResposne importManager, int index, String customerId) {
        String personalNumber = null;
        String name = null;
        String middleName = null;
        String surname = null;
        Long titleId = null;
        Long representationMethodId = null;
        String jobPosition = null;
        switch (index) {
            case 1: {
                titleId = customerManagerImportRepository.getTitelIdByTitle(importManager.getTitle1());
                representationMethodId = customerManagerImportRepository.getRepresentationIdByRepresentationName(importManager.getRepresentation1());
                personalNumber = importManager.getPersonalNumber1();
                name = importManager.getName1();
                middleName = importManager.getMiddlename1();
                surname = importManager.getSurname1();
                jobPosition = importManager.getPosition1();
            }
            break;
            case 2: {
                titleId = customerManagerImportRepository.getTitelIdByTitle(importManager.getTitle2());
                representationMethodId = customerManagerImportRepository.getRepresentationIdByRepresentationName(importManager.getRepresentation2());
                name = importManager.getName2();
                middleName = importManager.getMiddlename2();
                surname = importManager.getSurname2();
                jobPosition = importManager.getPosition2();
            }
            break;
            case 3: {
                titleId = customerManagerImportRepository.getTitelIdByTitle(importManager.getTitle3());
                representationMethodId = customerManagerImportRepository.getRepresentationIdByRepresentationName(importManager.getRepresentation3());
                jobPosition = importManager.getPosition3();
                name = importManager.getName3();
                middleName = importManager.getMiddlename3();
                surname = importManager.getSurname3();
            }
            break;
        }
        return new CustomerManagerViewResponse(
                personalNumber,
                customerId,
                name,
                middleName,
                surname,
                titleId,
                representationMethodId,
                jobPosition,
                Status.ACTIVE
        );
    }
}
