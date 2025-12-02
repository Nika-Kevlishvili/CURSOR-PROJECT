package bg.energo.phoenix.service.excel;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.ErrorCode;
import bg.energo.phoenix.model.response.billing.billingRun.BillingErrorDataResponse;
import bg.energo.phoenix.repository.billing.billingRun.BillingErrorDataRepository;
import bg.energo.phoenix.service.billing.billingRun.errors.BillingProtocol;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BillingRunErrorReportService extends MultiSheetExcelBaseService{

    private final BillingErrorDataRepository errorDataRepository;
    private static final int BATCH_SIZE = 100000;
    @Override
    public MultiSheetExcelType getMultiSheetExcelType() {
        return MultiSheetExcelType.BILLING_RUN_ERROR_REPORT;
    }

    @Override
    public int getBatchSize() {
        return BATCH_SIZE;
    }

    @Override
    public List<?> getData(String... args) {
        if (ArrayUtils.isEmpty(args)) {
            log.debug("Args empty, Billing ID required");
            throw new ClientException(ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED);
        }

        Long billingId = Long.parseLong(args[0]);
        BillingProtocol billingProtocol = BillingProtocol.valueOf(args[1]);

        return errorDataRepository.findByBillingId(billingId,billingProtocol);
    }

    @Override
    public List<String> getHeaders() {
        return List.of(
                "invoice_number",
                "error_message"
        );
    }

    @Override
    public <T> void fillRow(Worksheet ws, T record, int rowNum) {
        BillingErrorDataResponse response = (BillingErrorDataResponse) record;
        ws.value(rowNum, 0, response.getInvoiceNumber());
        ws.value(rowNum, 1, response.getMessage());
    }
}
