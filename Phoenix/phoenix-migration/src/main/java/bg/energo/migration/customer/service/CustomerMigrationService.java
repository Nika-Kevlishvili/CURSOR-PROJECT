package bg.energo.migration.customer.service;

import bg.energo.migration.customer.entity.migrationlog.CustomerCreationRequestLog;
import bg.energo.migration.customer.entity.migrationlog.CustomerCreationResponseLog;
import bg.energo.migration.customer.repository.CustomerCreationRequestLogRepository;
import bg.energo.migration.customer.repository.CustomerCreationResponseLogRepository;
import bg.energo.migration.customer.repository.CustomerGeneralDataRepository;
import bg.energo.migration.integration.phoenix.customer.model.request.CreateCustomerRequest;
import bg.energo.migration.integration.phoenix.customer.model.response.CustomerResponse;
import bg.energo.migration.integration.phoenix.customer.service.PhoenixCustomerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerMigrationService {
    private final PhoenixCustomerService phoenixCustomerService;
    private final CustomerGeneralDataRepository customerGeneralDataRepository;
    private final CustomerCreationRequestLogRepository customerCreationRequestLogRepository;
    private final CustomerCreationResponseLogRepository customerCreationResponseLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void migrateCustomers(
            String customerIdForeign,
            String customerIdentifier,
            CreateCustomerRequest createCustomerRequest
    ) {
        UUID processUniqId = UUID.randomUUID();
        log.info("{} - Starting customer migration for foreignId: {}, customerIdentifier: {}",
                processUniqId,
                customerIdForeign,
                customerIdentifier
        );

        Long customerCreationReqId = null;
        try {
            customerCreationReqId = saveCustomerCreationRequest(
                    processUniqId,
                    customerIdForeign,
                    customerIdentifier,
                    createCustomerRequest
            );

            CustomerResponse customerResponse = phoenixCustomerService.phoenixCreateCustomer(createCustomerRequest);

            saveCustomerCreationSuccessfulResponse(
                    processUniqId,
                    customerCreationReqId,
                    customerResponse.id(),
                    customerIdForeign
            );

        } catch (Exception e) {
            log.error("{} - An error occurred during customer migration for foreignId: {}",
                    processUniqId,
                    customerIdForeign,
                    e
            );
            saveCustomerCreationFailedResponse(
                    processUniqId,
                    e.getMessage(),
                    customerCreationReqId
            );
        }

    }

    private Long saveCustomerCreationRequest(
            UUID processUniqId,
            String customerIdForeign,
            String customerIdentifier,
            CreateCustomerRequest request
    ) throws JsonProcessingException {

        String requestJson = objectMapper.writeValueAsString(request);

        log.info("{} - Sending customer creation request to Phoenix. Request payload: {}",
                processUniqId,
                requestJson);

        CustomerCreationRequestLog customerCreationRequestLog =
                CustomerCreationRequestLog
                        .builder()
                        .customerIdForeign(Long.valueOf(customerIdForeign))
                        .customerIdentifier(customerIdentifier)
                        .processUniqId(processUniqId)
                        .payload(requestJson)
                        .createDate(LocalDateTime.now())
                        .build();

        Long requestId = customerCreationRequestLogRepository
                .save(customerCreationRequestLog)
                .getId();

        log.info("{} - Customer creation request saved with requestId: {}",
                processUniqId,
                requestId
        );

        return requestId;
    }


    private void saveCustomerCreationSuccessfulResponse(
            UUID processUniqId,
            Long customerCreationReqId,
            Long savedCustomerId,
            String customerForeignId
    ) {

        log.info("{} - Customer successfully created in Phoenix with customerId: {}",
                processUniqId,
                savedCustomerId
        );

        customerGeneralDataRepository
                .findByCustomerId(customerForeignId)
                .ifPresent(
                        customer -> {
                            log.info("{} - Linking customerId: {} to foreignId: {} in local system",
                                    processUniqId,
                                    savedCustomerId,
                                    customerForeignId
                            );

                            customer.setIdFromPhoenix(savedCustomerId);
                            customerGeneralDataRepository.save(customer);

                            log.info("{} - Customer with foreignId: {} successfully updated with Phoenix ID: {}",
                                    processUniqId,
                                    customerForeignId,
                                    savedCustomerId
                            );

                        }
                );

        CustomerCreationResponseLog customerCreationResponseLog =
                CustomerCreationResponseLog
                        .builder()
                        .requestId(customerCreationReqId)
                        .phoenixCustomerId(savedCustomerId)
                        .createDate(LocalDateTime.now())
                        .build();

        customerCreationResponseLogRepository.save(customerCreationResponseLog);

        log.info("{} - Customer creation response saved successfully for requestId: {}",
                processUniqId,
                customerCreationReqId
        );
    }

    private void saveCustomerCreationFailedResponse(
            UUID processUniqId,
            String exceptionMessage,
            Long customerCreationReqId
    ) {

        log.error("{} - Customer creation failed with error: {}",
                processUniqId,
                exceptionMessage
        );

        CustomerCreationResponseLog customerCreationResponseLog =
                CustomerCreationResponseLog
                        .builder()
                        .requestId(customerCreationReqId)
                        .errorMessage(exceptionMessage)
                        .createDate(LocalDateTime.now())
                        .build();

        customerCreationResponseLogRepository.save(customerCreationResponseLog);

        log.info("{} - Customer creation failure response saved for requestId: {}",
                processUniqId,
                customerCreationReqId
        );
    }
}
