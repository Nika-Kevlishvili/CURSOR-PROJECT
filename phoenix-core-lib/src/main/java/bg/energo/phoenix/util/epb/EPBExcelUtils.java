package bg.energo.phoenix.util.epb;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.process.massImport.ExcelHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;
import static bg.energo.phoenix.exception.ErrorCode.ILLEGAL_ARGUMENTS_PROVIDED;

@Slf4j
public class EPBExcelUtils {

    /**
     * Validates the content of the uploaded file against a predefined template.
     *
     * @param file     The uploaded file to validate.
     * @param template The predefined template to validate the file against.
     * @throws ClientException if there is an error while validating the file content.
     */
    public static void validateFileContent(MultipartFile file, byte[] template, int sheetCount) {
        List<String> templateHeaders = getTemplateHeaders(template);

        if (templateHeaders.isEmpty()) {
            log.error("Error happened while processing mass import template");
            throw new ClientException("Error happened while processing mass import template", APPLICATION_ERROR);
        }

        try (
                InputStream is = file.getInputStream();
                Workbook workbook = ExcelHelper.isXLS(file) ? new HSSFWorkbook(is) : new XSSFWorkbook(is)
        ) {
            if (workbook.getNumberOfSheets() > sheetCount) {
                throw new ClientException("Invalid file format, number of sheets more then 1", APPLICATION_ERROR);
            }

            Sheet firstSheet = workbook.getSheetAt(0);
            Row headerRow = firstSheet.getRow(0);

            int cellCount = headerRow.getPhysicalNumberOfCells();
            if (cellCount != templateHeaders.size()) {
                log.error("Cell count invalid in header");
                throw new ClientException("Invalid file format", APPLICATION_ERROR);
            }

            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                String currCell = headerRow.getCell(i).toString();
                if (!currCell.equals(templateHeaders.get(i))) {
                    log.error("Headers does not match");
                    throw new ClientException("Invalid file format", APPLICATION_ERROR);
                }
            }
        } catch (IOException e) {
            log.error("Error happened while validating mass import file content", e);
            throw new ClientException("Invalid file format", ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }

    private static List<String> getTemplateHeaders(byte[] massImportTemplate) {
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

    public static void validateFileFormat(MultipartFile file) {
        if (!ExcelHelper.hasExcelFormat(file)) {
            log.error("File has invalid format");
            throw new ClientException("Invalid file format", ILLEGAL_ARGUMENTS_PROVIDED);
        }
    }

    public static Integer getIntegerValue(int columnNumber, Row row) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.NUMERIC);
            return (int) row.getCell(columnNumber).getNumericCellValue();
        }
        return null;
    }

    public static Long getLongValue(int columnNumber, Row row) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.NUMERIC);
            return (long) row.getCell(columnNumber).getNumericCellValue();
        }
        return null;
    }

    public static BigDecimal getBigDecimalValue(int columnNumber, Row row) {
        Cell cell = row.getCell(columnNumber);
        if (cell != null && cell.getCellType() != CellType.BLANK) {
            if (cell.getCellType() == CellType.STRING) {
                try {
                    return new BigDecimal(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("invalid value to convert into decimal");
                }
            }
            ((XSSFCell) cell).setCellType(CellType.NUMERIC);
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        return null;
    }

    public static LocalDate getLocalDateValue(int columnNumber, Row row) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            Cell cell = row.getCell(columnNumber);

            if (DateUtil.isCellDateFormatted(cell)) {
                Date dateCellValue = cell.getDateCellValue();
                return dateCellValue.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else {
                throw new IllegalArgumentException("Unsupported cell type for Date conversion");
            }
        }
        return null;
    }

    public static String getStringValue(int columnNumber, Row row) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.STRING);
            return row.getCell(columnNumber).getStringCellValue();
        }
        return null;
    }

    public static void validateExcelIsEmpty(MultipartFile file) {
        try {
            try (InputStream fis = file.getInputStream()) {
                Workbook workbook = ExcelHelper.isXLS(file) ? new HSSFWorkbook(fis) : new XSSFWorkbook(fis);

                Sheet firstSheet = workbook.getSheetAt(0);
                if (firstSheet.getPhysicalNumberOfRows() < 2) {
                    log.error("File is empty");
                    throw new IllegalArgumentsProvidedException("File is empty");
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentsProvidedException("File is empty");
        }
    }
}
