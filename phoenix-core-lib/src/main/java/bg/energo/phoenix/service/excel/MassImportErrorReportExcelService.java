package bg.energo.phoenix.service.excel;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.process.model.response.ProcessedRecordInfoResponse;
import bg.energo.phoenix.process.service.ProcessedRecordInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MassImportErrorReportExcelService extends MultiSheetExcelBaseService {

    private static final int BATCH_SIZE = 100000;

    private final ProcessedRecordInfoService processedRecordInfoService;

    @Override
    public MultiSheetExcelType getMultiSheetExcelType() {
        return MultiSheetExcelType.MASS_IMPORT_ERROR_REPORT;
    }

    @Override
    public int getBatchSize() {
        return BATCH_SIZE;
    }

    @Override
    public List<?> getData(String... args) {
        if (ArrayUtils.isEmpty(args)) {
            log.debug("Args empty, process ID required");
            throw new ClientException(ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long processId = Long.parseLong(args[0]);

        long start = System.currentTimeMillis();
        List<ProcessedRecordInfoResponse> processRecords = processedRecordInfoService.getRecordsByProcessIdAndStatus(processId, false);
        long finish = System.currentTimeMillis() - start;
        log.debug("{} - JPA select took: " + TimeUnit.MILLISECONDS.toSeconds(finish) + " seconds", getMultiSheetExcelType().getValue());

        return processRecords;
    }

    @Override
    public List<String> getHeaders() {
        return List.of(
                "row_number",
                "identifier",
                "identifier_version",
                "errors"
        );
    }

    @Override
    public <T> void fillRow(Worksheet ws, T record, int rowNum) {
        ProcessedRecordInfoResponse response = (ProcessedRecordInfoResponse) record;
        ws.value(rowNum, 0, response.getRecordId());
        ws.value(rowNum, 1, response.getRecordIdentifier());
        ws.value(rowNum, 2, response.getRecordIdentifierVersion());
        ws.value(rowNum, 3, response.getErrorMessage());
    }

}
