package bg.energo.phoenix.process.massImport;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Service
@Slf4j
public class ExcelHelper {
    private static final String XLSX_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String XLSM_TYPE = "application/vnd.ms-excel.sheet.macroenabled.12";
    private static final String XLS_TYPE = "application/vnd.ms-excel";

    public static boolean hasExcelFormat(MultipartFile file) {
        return isXLS(file) || isXLSM(file) || isXLSX(file);
    }

    public static boolean isXLS(MultipartFile file) {
        return Objects.equals(file.getContentType(), XLS_TYPE);
    }

    public static boolean isXLSX(MultipartFile file) {
        return Objects.equals(file.getContentType(), XLSX_TYPE);
    }

    public static boolean isXLSM(MultipartFile file) {
        return Objects.equals(file.getContentType(), XLSM_TYPE);
    }

    public Workbook createXSSFWorkbook() {
        try {
            return new XSSFWorkbook();
        } catch (Exception e) {
            log.error("Exception handled while trying to create workbook");
            throw e;
        }
    }
}
