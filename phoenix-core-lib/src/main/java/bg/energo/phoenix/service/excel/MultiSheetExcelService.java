package bg.energo.phoenix.service.excel;

import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiSheetExcelService {

    private final List<MultiSheetExcelBaseService> multiSheetExcelBaseServiceList;

    /**
     * Delegates the generation of a multi-sheet Excel workbook to the appropriate implementation of
     * {@link MultiSheetExcelBaseService}, based on the specified multi-sheet Excel type. The generated
     * workbook is streamed to the provided {@link HttpServletResponse} object's output stream.
     *
     * @param multiSheetExcelType the type of multi-sheet Excel workbook to generate
     * @param response the HTTP response object to stream the generated Excel workbook to
     * @param args the arguments to pass to the {@link MultiSheetExcelBaseService#generateExcel} method for
     *             retrieving input data to export
     */
    public void generateExcel(String multiSheetExcelType, HttpServletResponse response, String... args) {
        findMultiSheetExcelBaseService(multiSheetExcelType).generateExcel(response, args);
    }

    /**
     * Finds a {@link MultiSheetExcelBaseService} instance for the given {@link MultiSheetExcelType}.
     *
     * @param multiSheetExcelType the type of the {@link MultiSheetExcelBaseService} to find
     * @return the {@link MultiSheetExcelBaseService} instance for the given {@link MultiSheetExcelType}
     * @throws DomainEntityNotFoundException if the requested {@link MultiSheetExcelType} does not exist
     * or if no {@link MultiSheetExcelBaseService} instance can be found for the given type
     */
    private MultiSheetExcelBaseService findMultiSheetExcelBaseService(String multiSheetExcelType) {
        Optional<MultiSheetExcelType> excelTypeOptional = Optional.ofNullable(MultiSheetExcelType.fromValue(multiSheetExcelType));
        if (excelTypeOptional.isEmpty()) {
            log.error("Requested multiSheetExcelType does not exist");
            throw new DomainEntityNotFoundException("Requested multiSheetExcelType type does not exist");
        }

        MultiSheetExcelType excelType = excelTypeOptional.get();

        Optional<MultiSheetExcelBaseService> multiSheetExcelServiceOptional = multiSheetExcelBaseServiceList
                .stream()
                .filter(service -> service.getMultiSheetExcelType().equals(excelType))
                .findFirst();

        if (multiSheetExcelServiceOptional.isEmpty()) {
            log.error("Service does not exist for excelType: {}", excelType);
            throw new DomainEntityNotFoundException("Service does not exist for excelType: %s".formatted(excelType));
        }

        return multiSheetExcelServiceOptional.get();
    }

}
