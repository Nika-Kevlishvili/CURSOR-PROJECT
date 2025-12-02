package bg.energo.phoenix.service.massImport.contract.product;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.CacheObjectForDetails;
import bg.energo.phoenix.model.request.contract.product.ProductContractCreateRequest;
import bg.energo.phoenix.model.request.contract.product.ProductContractUpdateRequest;
import bg.energo.phoenix.process.repository.ProcessRepository;
import bg.energo.phoenix.process.repository.ProcessedRecordInfoRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.service.contract.product.ProductContractService;
import bg.energo.phoenix.service.massImport.AbstractMassImportProcessService;
import bg.energo.phoenix.service.notifications.service.NotificationEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class ProductContractMassImportProcessService extends AbstractMassImportProcessService {

    @Value("${app.cfg.productContract.massImport.numberOfRowsPerTask}")
    private Integer numberOfRowsPerTsk;
    @Value("${app.cfg.productContract.massImport.numberOfTasksPerThread}")
    private Integer numberOfCallablesPerThread;
    @Value("${app.cfg.productContract.massImport.numberOfThreads}")
    private Integer numberOfThreads;

    private final ProductContractExcelMapper excelMapper;
    private final ProductContractService productContractService;
    private final ProductContractRepository contractRepository;

    public ProductContractMassImportProcessService(ProcessRepository processRepository,
                                                   ProductContractRepository contractRepository,
                                                   ProductContractService productContractService,
                                                   ProcessedRecordInfoRepository processRecordInfoRepository,
                                                   ProductContractExcelMapper excelMapper,
                                                   NotificationEventPublisher notificationEventPublisher) {
        super(processRepository, processRecordInfoRepository, notificationEventPublisher);
        this.excelMapper = excelMapper;
        this.productContractService = productContractService;
        this.contractRepository = contractRepository;
    }

    @Override
    public boolean supports(EventType eventType) {
        return eventType.equals(EventType.PRODUCT_CONTRACT_MASS_IMPORT_PROCESS);
    }

    @Override
    protected String processRow(Row row, Set<String> permissions, String processSysUserId, LocalDate date, Long processRecordInfo) {
        List<String> errorMessages = new ArrayList<>();
        String contractNumber = excelMapper.getContractNumber(row);
        Integer contractVersion = excelMapper.getContractVersion(row);
        CreateEdit createEdit = excelMapper.getCreateEdit(row);
        LocalDate startDate = excelMapper.getStartDate(row);
        if (contractNumber == null && contractVersion == null && createEdit == null) {
            ProductContractCreateRequest productContractCreateRequest = excelMapper.productContractCreateRequest(row, errorMessages);
            validateRequest(errorMessages, productContractCreateRequest);
            return String.valueOf(productContractService.create(productContractCreateRequest));
        } else if (contractNumber != null && contractVersion == null && createEdit != null && createEdit.equals(CreateEdit.C)) {
            CacheObject contractObject = contractRepository.getContractCacheObjectByNumber(contractNumber)
                    .orElseThrow(() -> new DomainEntityNotFoundException("Contract with given number not found!;"));

            ProductContractUpdateRequest productContractUpdateRequest = excelMapper.productContractUpdateRequest(contractObject.getId(), row, errorMessages);
            if (startDate == null) {
                throw new IllegalArgumentException("Start date should be provided!;");
            }
            productContractUpdateRequest.setStartDate(startDate);
            productContractUpdateRequest.setSavingAsNewVersion(true);
            return String.valueOf(productContractService.edit(
                            contractObject.getId(),
                            null,
                            productContractUpdateRequest,
                            false
                    )
            );
        } else if (contractNumber != null && contractVersion != null && createEdit != null) {
            if (createEdit.equals(CreateEdit.C)) {
                CacheObjectForDetails contractObject;
                if (contractVersion == 0) {
                    contractObject = contractRepository.getLatestContractCacheObject(contractNumber)
                            .orElseThrow(() -> new DomainEntityNotFoundException("Contract with given number not found!;"));
                } else {
                    contractObject = contractRepository.getContractCacheObjectByNumberAndVersion(contractNumber, contractVersion)
                            .orElseThrow(() -> new DomainEntityNotFoundException("Contract with given number not found!;"));
                }

                ProductContractUpdateRequest productContractUpdateRequest = excelMapper.productContractUpdateRequest(contractObject.getId(), row, errorMessages);
                if (startDate == null) {
                    throw new IllegalArgumentException("Start date should be provided!;");
                }
                productContractUpdateRequest.setStartDate(startDate);
                productContractUpdateRequest.setSavingAsNewVersion(true);
                return String.valueOf(productContractService.edit(
                        contractObject.getId(),
                        Integer.valueOf(contractObject.getVersionId().toString()),
                        productContractUpdateRequest,
                        false)
                );
            } else {
                CacheObjectForDetails contractObject;
                if (contractVersion == 0) {
                    contractObject = contractRepository.getLatestContractCacheObject(contractNumber)
                            .orElseThrow(() -> new DomainEntityNotFoundException("Contract with given number not found!;"));
                } else {
                    contractObject = contractRepository.getContractCacheObjectByNumberAndVersion(contractNumber, contractVersion)
                            .orElseThrow(() -> new DomainEntityNotFoundException("Contract with given number not found!;"));
                }
                ProductContractUpdateRequest productContractUpdateRequest = excelMapper.productContractUpdateRequest(contractObject.getId(), row, errorMessages);
                productContractUpdateRequest.setStartDate(startDate);
                validateRequest(errorMessages, productContractUpdateRequest);
                return String.valueOf(productContractService.edit(
                        contractObject.getId(),
                        Integer.valueOf(contractObject.getVersionId().toString()),
                        productContractUpdateRequest,
                        false)
                );
            }

        } else {
            throw new DomainEntityNotFoundException("Invalid Parameters provided!;");
        }

    }


    @Override
    protected String getIdentifier(Row row) {
        return "1";
    }

    @Override
    protected int getNumberOfThreads() {
        return numberOfThreads;
    }

    @Override
    protected int getNumberOfCallablesPerThread() {
        return numberOfCallablesPerThread;
    }

    @Override
    protected int getNumberOfRowsPerTask() {
        return numberOfRowsPerTsk;
    }
}
