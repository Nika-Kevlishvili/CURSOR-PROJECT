package bg.energo.phoenix.service.massImport;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.enums.customer.CustomerStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import bg.energo.phoenix.model.request.billing.compensations.CompensationRequest;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import bg.energo.phoenix.util.epb.EPBExcelUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;

@Service
@RequiredArgsConstructor
public class CompensationExcelMapper {

    private final CurrencyRepository currencyRepository;
    private final CustomerRepository customerRepository;
    private final PointOfDeliveryRepository pointOfDeliveryRepository;

    private Long getCurrency(Row row, List<String> errorMessages, Integer columnNumber) {
        String currencyName = EPBExcelUtils.getStringValue(columnNumber, row);
        if (!StringUtils.isEmpty(currencyName)) {
            Optional<CacheObject> currency = currencyRepository.getCacheObjectByNameAndStatusIn(currencyName, List.of(NomenclatureItemStatus.ACTIVE));
            if (currency.isPresent()) {
                return currency.get().getId();
            }
        } else {
            errorMessages.add("Document Currency name is empty;");
            return null;
        }
        return null;
    }

    private Long getCustomerId(Row row, List<String> messages, Integer columnNumber, String message) {
        String customerIdentifier = EPBExcelUtils.getStringValue(columnNumber, row);

        if (!StringUtils.isEmpty(customerIdentifier)) {
            Optional<CacheObject> customer = customerRepository.findCacheObjectByName(customerIdentifier, CustomerStatus.ACTIVE);
            if (customer.isPresent()) {
                return customer.get().getId();
            }
        } else {
            messages.add(message);
            return null;
        }
        return null;
    }

    private Long getPod(Row row, List<String> messages, Integer columnNumber) {
        String podIdentifier = EPBExcelUtils.getStringValue(columnNumber, row);
        if (!StringUtils.isEmpty(podIdentifier)) {
            Optional<CacheObject> pod = pointOfDeliveryRepository.getCacheObjectByIdentifierAndStatus(podIdentifier, PodStatus.ACTIVE);
            if (pod.isPresent()) {
                return pod.get().getId();
            }
        } else {
            messages.add("Pod identifier is empty;");
            return null;
        }
        return null;
    }

    public CompensationRequest getCompensationRequest(Row row, List<String> errorMessages) {
        return CompensationRequest
                .builder()
                .number(EPBExcelUtils.getStringValue(0, row))
                .date(EPBExcelUtils.getLocalDateValue(1, row))
                .documentPeriod(EPBExcelUtils.getLocalDateValue(2, row))
                .volumes(getDecimalValue(3, row))
                .price(getDecimalValue(4, row))
                .reason(EPBExcelUtils.getStringValue(5, row))
                .documentAmount(getDecimalValue(6, row))
                .documentCurrencyId(getCurrency(row, errorMessages, 7))
                .customerId(getCustomerId(row, errorMessages, 8, "Customer Identifier is empty;"))
                .podId(getPod(row, errorMessages, 9))
                .recipientId(getCustomerId(row, errorMessages, 10, "Government Institution Identifier is empty;"))
                .build();
    }

    private BigDecimal getDecimalValue(int columnNumber, Row row) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            BigDecimal result;

            if (row.getCell(columnNumber).getCellType() == CellType.STRING) {
                result = BigDecimal.valueOf(Long.parseLong(row.getCell(columnNumber).getStringCellValue()));
            } else if (row.getCell(columnNumber).getCellType() == CellType.NUMERIC) {
                double value = row.getCell(columnNumber).getNumericCellValue();

                result = BigDecimal.valueOf(value);

                if (result.stripTrailingZeros().scale() <= 0) {
                    result = result.setScale(0, RoundingMode.UNNECESSARY);
                }
            } else {
                throw new ClientException("Invalid cell type in row " + columnNumber + ";", ILLEGAL_ARGUMENTS_PROVIDED);
            }

            return result;
        } else {
            return null;
        }
    }

}
