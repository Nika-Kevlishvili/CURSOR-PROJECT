package bg.energo.phoenix.util.mi;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.model.enums.time.PeriodType;
import bg.energo.phoenix.model.enums.time.TimeZone;
import bg.energo.phoenix.process.massImport.ExcelHelper;
import bg.energo.phoenix.service.pod.billingByProfile.BillingByProfileImportHelper;
import bg.energo.phoenix.service.product.price.priceParameter.PriceParameterImportHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;
import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExcelMapperForMIFields {
    private final static int LIMIT_OF_ROWS_FOR_FIFTEEN_MINUTES = 35138;
    private final static int LIMIT_OF_ROWS_FOR_ONE_HOUR = 8786;


    public String getPeriodFrom(
            Row row,
            int columnNumber,
            PeriodType periodType,
            TimeZone timezone,
            PriceParameterImportHelper helper
    ) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber)
                                                    .getCellType() != CellType.BLANK) {
            LocalDateTime result;
            String cellData = row.getCell(columnNumber)
                                 .getStringCellValue()
                                 .trim();

            try {
                if (periodType.equals(PeriodType.ONE_MONTH)) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                    result = LocalDate.parse("01." + cellData, formatter)
                                      .atStartOfDay();
                    helper.setTime(String.valueOf(result));
                } else if (periodType.equals(PeriodType.ONE_DAY)) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                    result = LocalDate.parse(cellData, formatter)
                                      .atStartOfDay();
                    helper.setTime(String.valueOf(result));
                } else {
                    if (cellData.contains("*")) {
                        cellData = cellData.substring(0, cellData.indexOf('*'))
                                           .trim();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                        result = LocalDateTime.parse(cellData, formatter);
                        checkForShiftedHour(result, timezone);
                        helper.setTime(result + "*");
                    } else {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                        result = LocalDateTime.parse(cellData, formatter);
                        helper.setTime(String.valueOf(result));
                    }
                }
            } catch (Exception e) {
                log.error("Invalid time format for provided time period.", e);
                throw new ClientException(
                        "Invalid time format for provided time period in row " + (row.getRowNum() + 1) + ". " + e.getMessage(),
                        ILLEGAL_ARGUMENTS_PROVIDED
                );
            }
            LocalDateTime resultTime;
            if (helper.getTime()
                      .contains("*")) {
                resultTime = LocalDateTime.parse(helper.getTime()
                                                       .substring(
                                                               0,
                                                               helper.getTime()
                                                                     .indexOf('*')
                                                       )
                                                       .trim());
            } else {
                resultTime = LocalDateTime.parse(helper.getTime());
            }
            if (resultTime.isBefore(LocalDateTime.of(1990, 1, 1, 0, 0))) {
                log.error("Provided period should be after 01.01.1990 in row " + (row.getRowNum() + 1) + ".");
                throw new ClientException(
                        "Provided period should be after 01.01.1990 in row " + (row.getRowNum() + 1) + ". ",
                        ILLEGAL_ARGUMENTS_PROVIDED
                );
            }
            validatePeriodFrom(resultTime, periodType, row.getRowNum() + 1);
            return helper.getTime();
        } else {
            log.error("Period is blank in row " + (row.getRowNum() + 1) + ".");
            throw new ClientException("Period is blank in row " + (row.getRowNum() + 1) + ".", ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }

    public String getPeriodFrom(
            Row row,
            int columnNumber,
            PeriodType periodType,
            TimeZone timezone,
            BillingByProfileImportHelper helper
    ) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber)
                                                    .getCellType() != CellType.BLANK) {
            LocalDateTime result;
            String cellData = row.getCell(columnNumber)
                                 .getStringCellValue()
                                 .trim();

            try {
                if (periodType.equals(PeriodType.ONE_MONTH)) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                    result = LocalDate.parse("01." + cellData, formatter)
                                      .atStartOfDay();
                    helper.setTime(String.valueOf(result));
                } else if (periodType.equals(PeriodType.ONE_DAY)) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                    result = LocalDate.parse(cellData, formatter)
                                      .atStartOfDay();
                    helper.setTime(String.valueOf(result));
                } else {
                    if (cellData.contains("*")) {
                        cellData = cellData.substring(0, cellData.indexOf('*'))
                                           .trim();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                        result = LocalDateTime.parse(cellData, formatter);
                        checkForShiftedHour(result, timezone);
                        helper.setTime(result + "*");
                    } else {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                        result = LocalDateTime.parse(cellData, formatter);
                        helper.setTime(String.valueOf(result));
                    }
                }
            } catch (Exception e) {
                log.error("Invalid time format for provided time period.", e);
                throw new ClientException(
                        "Invalid time format for provided time period in row " + (row.getRowNum() + 1) + ". " + e.getMessage(),
                        ILLEGAL_ARGUMENTS_PROVIDED
                );
            }
            LocalDateTime resultTime;
            if (helper.getTime()
                      .contains("*")) {
                resultTime = LocalDateTime.parse(helper.getTime()
                                                       .substring(
                                                               0,
                                                               helper.getTime()
                                                                     .indexOf('*')
                                                       )
                                                       .trim());
            } else {
                resultTime = LocalDateTime.parse(helper.getTime());
            }
            if (resultTime.isBefore(LocalDateTime.of(1990, 1, 1, 0, 0))) {
                log.error("Provided period should be after 01.01.1990 in row " + (row.getRowNum() + 1) + ".");
                throw new ClientException(
                        "Provided period should be after 01.01.1990 in row " + (row.getRowNum() + 1) + ". ",
                        ILLEGAL_ARGUMENTS_PROVIDED
                );
            }
            validatePeriodFrom(resultTime, periodType, row.getRowNum() + 1);
            return helper.getTime();
        } else {
            log.error("Period is blank in row " + (row.getRowNum() + 1) + ".");
            throw new ClientException("Period is blank in row " + (row.getRowNum() + 1) + ".", ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }

    private void checkForShiftedHour(LocalDateTime period, TimeZone timezone) {
        int hour = period.getHour();
        int minutes = period.getMinute();

        if (timezone.equals(TimeZone.CET)) {
            if (!(hour == 3 && (minutes == 0 || minutes == 15 || minutes == 30 || minutes == 45))) {
                log.error("Time can`t be shifted.");
                throw new ClientException("Time can`t be shifted.", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        } else {
            if (!(hour == 4 && (minutes == 0 || minutes == 15 || minutes == 30 || minutes == 45))) {
                log.error("Time can`t be shifted.");
                throw new ClientException("Time can`t be shifted.", ILLEGAL_ARGUMENTS_PROVIDED);
            }
        }
    }

    public void validatePeriodFrom(LocalDateTime periodFrom, PeriodType periodType, int rowNum) {
        int minute = periodFrom.getMinute();

        if (periodType.equals(PeriodType.FIFTEEN_MINUTES)) {
            if (!(minute == 15 || minute == 30 || minute == 45 || minute == 0)) {
                log.error("Invalid starting time for fifteen minutes time period.");
                throw new ClientException(
                        "Invalid starting time for fifteen minutes time period in row " + rowNum + ".",
                        ILLEGAL_ARGUMENTS_PROVIDED
                );
            }
        } else if (periodType.equals(PeriodType.ONE_HOUR)) {
            if (minute != 0) {
                log.error("Invalid starting time for one hour time period.");
                throw new ClientException(
                        "Invalid starting time for one hour time period in row " + rowNum + ".",
                        ILLEGAL_ARGUMENTS_PROVIDED
                );
            }
        }
    }

    public LocalDateTime getPeriodTo(LocalDateTime periodFrom, PeriodType periodType, int rowNum) {
        LocalDateTime periodTo;
        switch (periodType) {
            case FIFTEEN_MINUTES -> periodTo = periodFrom.plusMinutes(15L);
            case ONE_HOUR -> periodTo = periodFrom.plusHours(1L);
            case ONE_DAY -> periodTo = periodFrom.plusDays(1L);
            case ONE_MONTH -> periodTo = periodFrom.plusMonths(1L);
            default -> throw new IllegalStateException("Unexpected value: " + periodType);
        }
        if (periodTo.isAfter(LocalDateTime.of(2090, 12, 31, 0, 0))) {
            log.error("Provided period should be before 31.12.2090 in row" + rowNum + ".");
            throw new ClientException("Provided period should be before 31.12.2090 in row" + rowNum + ".", ILLEGAL_ARGUMENTS_PROVIDED);
        }
        return periodTo;
    }

    public BigDecimal getPriceValue(int columnNumber, Row row) {

        if (row.getCell(columnNumber) != null && row.getCell(columnNumber)
                                                    .getCellType() != CellType.BLANK) {

            BigDecimal result;
            if (row.getCell(columnNumber)
                   .getCellType() == CellType.STRING) {
                if (row.getCell(columnNumber)
                       .getStringCellValue()
                       .trim()
                       .equals("D")) {
                    result = BigDecimal.valueOf(Long.MAX_VALUE);
                } else {
                    result = BigDecimal.valueOf(Long.parseLong(row.getCell(columnNumber)
                                                                  .getStringCellValue()));
                }
            } else if (row.getCell(columnNumber)
                          .getCellType() == CellType.NUMERIC) {
                DecimalFormat df = new DecimalFormat("#.##############");
                row.getCell(columnNumber)
                   .setCellValue(df.format(row.getCell(columnNumber)
                                              .getNumericCellValue()));
                result = new BigDecimal(row.getCell(columnNumber)
                                           .getStringCellValue());
                if (!result.toPlainString()
                           .matches("^-?[0-9]\\d*(\\.\\d+)?$")) {
                    log.error("Invalid format for provided price.");
                    throw new ClientException(
                            "Invalid format for provided price in row " + (row.getRowNum() + 1) + ".",
                            ILLEGAL_ARGUMENTS_PROVIDED
                    );
                }
            } else {
                log.error("Invalid cell type for provided price.");
                throw new ClientException(
                        "Invalid cell type for provided price in row " + (row.getRowNum() + 1) + ".",
                        ILLEGAL_ARGUMENTS_PROVIDED
                );
            }
            return result;
        } else {
            return null;
        }
    }

    public void validateFileContent(MultipartFile file, byte[] customerMassImportTemplate, PeriodType periodType) {
        List<String> templateHeaders = getTemplateHeaders(customerMassImportTemplate);

        if (templateHeaders.isEmpty()) {
            log.error("Error happened while processing mass import template");
            throw new ClientException("Error happened while processing mass import template", APPLICATION_ERROR);
        }

        try (InputStream is = file.getInputStream(); Workbook workbook = ExcelHelper.isXLS(file) ? new HSSFWorkbook(is) : new XSSFWorkbook(
                is)) {
            Sheet firstSheet = workbook.getSheetAt(0);
            Row headerRow = firstSheet.getRow(0);

            int cellCount = headerRow.getPhysicalNumberOfCells();
            if (cellCount != templateHeaders.size()) {
                log.error("Cell count invalid in header");
                throw new ClientException("Invalid file format", ILLEGAL_ARGUMENTS_PROVIDED);
            }
            for (Cell cell : headerRow) {
                if (!cell.toString()
                         .equals(templateHeaders.get(cell.getColumnIndex()))) {
                    log.error("Headers does not match");
                    throw new ClientException("Invalid file format", ILLEGAL_ARGUMENTS_PROVIDED);
                }
            }

            if ((periodType.equals(PeriodType.FIFTEEN_MINUTES) && firstSheet.getPhysicalNumberOfRows() - 1 > LIMIT_OF_ROWS_FOR_FIFTEEN_MINUTES) ||
                    (periodType.equals(PeriodType.ONE_HOUR) && firstSheet.getPhysicalNumberOfRows() - 1 > LIMIT_OF_ROWS_FOR_ONE_HOUR)) {
                log.error("Maximum interval for one import is exceeded.");
                throw new ClientException("Invalid file format", ILLEGAL_ARGUMENTS_PROVIDED);
            }


        } catch (IOException e) {
            log.error("Error happened while validating mass import file content", e);
            throw new ClientException("Invalid file format", ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }


    private List<String> getTemplateHeaders(byte[] massImportTemplate) {
        List<String> templateHeaders = new ArrayList<>();

        try (InputStream is = new ByteArrayInputStream(massImportTemplate); Workbook templateWorkbook = new XSSFWorkbook(is)) {
            Sheet firstSheet = templateWorkbook.getSheetAt(0);
            Row headerRow = firstSheet.getRow(0);
            for (Cell cell : headerRow) {
                templateHeaders.add(cell.toString());
            }

        } catch (Exception e) {
            log.error("Error happened while processing mass import template");
            throw new ClientException("Error happened while processing mass import template", APPLICATION_ERROR);
        }

        return templateHeaders;
    }

    public void validateFileFormat(MultipartFile file) {
        if (!ExcelHelper.hasExcelFormat(file)) {
            log.error("File has invalid format");
            throw new ClientException("Invalid file format", ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }
}
