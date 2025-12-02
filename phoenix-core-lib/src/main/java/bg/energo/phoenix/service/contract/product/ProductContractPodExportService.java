package bg.energo.phoenix.service.contract.product;

import bg.energo.phoenix.model.process.ProductContractPodExportData;
import bg.energo.phoenix.process.massImport.ExcelHelper;
import bg.energo.phoenix.service.xEnergie.XEnergieCommunicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductContractPodExportService {
    private final ExcelHelper excelHelper;
    private final XEnergieCommunicationService xEnergieCommunicationService;

    public void process(List<ProductContractPodExportData> productContractPodExportData) throws Exception {
        try (Workbook workbook = excelHelper.createXSSFWorkbook()) {
            fillData(productContractPodExportData, workbook);
            sendExportData(workbook);
        }
    }

    public Workbook generateExportData(List<ProductContractPodExportData> productContractPodExportData) throws Exception {
        Workbook workbook = excelHelper.createXSSFWorkbook();
        fillData(productContractPodExportData, workbook);
        return workbook;
    }

    /**
     * For Test Purposes only
     */
    private void fillData(List<ProductContractPodExportData> productContractPodExportData, Workbook workbook) {
        Sheet sheet = initializeExportSheet(workbook);

        for (int i = 0; i < productContractPodExportData.size(); i++) {
            ProductContractPodExportData data = productContractPodExportData.get(i);

            Row row = sheet.createRow(i + 1);

            addValueInCell(row, 0, data.getPodIdentifier());
            addValueInCell(row, 1, data.getPodAdditionalIdentifier());
            addValueInCell(row, 2, data.getPodIdentifierForEANorEIC());
            addValueInCell(row, 3, data.getDateOfActivation());
            addValueInCell(row, 4, data.getDateOfDeactivation());
            addValueInCell(row, 5, data.getType());
            addValueInCell(row, 6, data.getVoltageLevelOne());
            addValueInCell(row, 7, data.getIsleOperation());
            addValueInCell(row, 8, data.getAncillaryServicesProviderCheckbox());
            addValueInCell(row, 9, data.getMeasurementType());
            addValueInCell(row, 10, data.getSourceType());
            addValueInCell(row, 11, data.getCustomerNumber());
            addValueInCell(row, 12, data.getDistributionNetwork());
            addValueInCell(row, 13, data.getNeighbouringNetwork());
            addValueInCell(row, 14, data.getSupplier());
            addValueInCell(row, 15, data.getAlternateSupplier());
            addValueInCell(row, 16, data.getMeteringDataProvider());
            addValueInCell(row, 17, data.getAncillaryServicesProvider());
            addValueInCell(row, 18, data.getNeighbouringPod());
            addValueInCell(row, 19, data.getAccountingSubject());
            addValueInCell(row, 20, data.getInstalledPower());
            addValueInCell(row, 21, data.getConsumptionEstimate());
            addValueInCell(row, 22, data.getLpRegion());
            addValueInCell(row, 23, data.getLpClass());
            addValueInCell(row, 24, data.getSummaryForAccountingSubjectCheckbox());
            addValueInCell(row, 25, data.getAbroadCheckbox());
            addValueInCell(row, 26, data.getLockedCheckbox());
            addValueInCell(row, 27, data.getLRS());
            addValueInCell(row, 28, data.getObserver());
            addValueInCell(row, 29, data.getDealId());
            addValueInCell(row, 30, data.getMeasurementSystem());
            addValueInCell(row, 31, data.getNodata());
            addValueInCell(row, 32, data.getBGnoEPRES());
        }
    }

    private void sendExportData(Workbook workbook) throws IOException {
        ByteArrayOutputStream excelContent = new ByteArrayOutputStream();
        try (excelContent) {
            workbook.write(excelContent);
        }
        xEnergieCommunicationService.exportPods(excelContent);
    }

    private void addValueInCell(Row row, int column, String value) {
        Cell cell = row.createCell(column, CellType.STRING);
        if (value != null) {
            cell.setCellValue(value);
        }
    }

    private void addValueInCell(Row row, int column, Long value) {
        Cell cell = row.createCell(column, CellType.STRING);
        if (value != null) {
            cell.setCellValue(value);
        }
    }

    private void addValueInCell(Row row, int column, LocalDate value) {
        Cell cell = row.createCell(column, CellType.STRING);
        if (value != null) {
            cell.setCellValue(value.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        }
    }

    private void addValueInCell(Row row, int column, Integer value) {
        Cell cell = row.createCell(column, CellType.NUMERIC);
        if (value != null) {
            cell.setCellValue(value);
        }
    }

    private Sheet initializeExportSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet();
        Row headers = sheet.createRow(0);// headers row

        Cell name = headers.createCell(0, CellType.STRING);// name
        name.setCellValue("Name");

        Cell description = headers.createCell(1, CellType.STRING);// description
        description.setCellValue("Description");

        Cell EANorEIC = headers.createCell(2, CellType.STRING);// EAN/EIC
        EANorEIC.setCellValue("EAN/EIC");

        Cell dateFrom = headers.createCell(3, CellType.STRING);// Date from
        dateFrom.setCellValue("Date from");

        Cell dateTo = headers.createCell(4, CellType.STRING);// Date To
        dateTo.setCellValue("Date To");

        Cell type = headers.createCell(5, CellType.STRING);// Type
        type.setCellValue("Type");

        Cell voltageLevelOne = headers.createCell(6, CellType.STRING);// Volt. Level 1
        voltageLevelOne.setCellValue("Volt. Level 1");

        Cell isleOperationCheckbox = headers.createCell(7, CellType.STRING);// Isle operation (checkbox)
        isleOperationCheckbox.setCellValue("Isle operation (checkbox)");

        Cell ancillaryServicesProviderCheckbox = headers.createCell(8, CellType.STRING);// Ancillary services provider (checkbox)
        ancillaryServicesProviderCheckbox.setCellValue("Ancillary services provider (checkbox)");

        Cell measurementType = headers.createCell(9, CellType.STRING);// Measurement type
        measurementType.setCellValue("Measurement type");

        Cell sourceType = headers.createCell(10, CellType.STRING);// Source Type
        sourceType.setCellValue("Source type");

        Cell pdtOwner = headers.createCell(11, CellType.STRING);// PDT owner
        pdtOwner.setCellValue("PDT owner");

        Cell distributionNetwork = headers.createCell(12, CellType.STRING);// Distribution network
        distributionNetwork.setCellValue("Distribution network");

        Cell neighbouringNetwork = headers.createCell(13, CellType.STRING);// Neighbouring network
        neighbouringNetwork.setCellValue("Neighbouring network");

        Cell supplier = headers.createCell(14, CellType.STRING);// Supplier
        supplier.setCellValue("Supplier");

        Cell alternateSupplier = headers.createCell(15, CellType.STRING);// Alternate Supplier
        alternateSupplier.setCellValue("Alternate supplier");

        Cell meteringDataProvider = headers.createCell(16, CellType.STRING);// Metering Data Provider
        meteringDataProvider.setCellValue("Metering data provider");

        Cell ancillaryServicesProvider = headers.createCell(17, CellType.STRING);// Ancillary Services Provider
        ancillaryServicesProvider.setCellValue("Ancillary services provider");

        Cell neighbouringPOD = headers.createCell(18, CellType.STRING);// Neighbouring POD
        neighbouringPOD.setCellValue("Neighbouring POD");

        Cell accountingSubject = headers.createCell(19, CellType.STRING);// Accounting Subject
        accountingSubject.setCellValue("Accounting subject");

        Cell installedPower = headers.createCell(20, CellType.STRING);// Installed Power
        installedPower.setCellValue("Installed power");

        Cell yearlyConsumptionEstimate = headers.createCell(21, CellType.STRING);// Yearly consumption estimate
        yearlyConsumptionEstimate.setCellValue("Yearly consumption estimate");

        Cell lpRegion = headers.createCell(22, CellType.STRING);// LP Region
        lpRegion.setCellValue("LP region");

        Cell lpClass = headers.createCell(23, CellType.STRING);// LP Class
        lpClass.setCellValue("LP class");

        Cell summaryForAccountingSubjectCheckbox = headers.createCell(24, CellType.STRING);// Summary For Accounting Subject (checkbox)
        summaryForAccountingSubjectCheckbox.setCellValue("Summary for accounting subject (checkbox)");

        Cell abroadCheckbox = headers.createCell(25, CellType.STRING);// Abroad (checkbox)
        abroadCheckbox.setCellValue("Abroad (checkbox)");

        Cell lockedCheckbox = headers.createCell(26, CellType.STRING);// Locked (checkbox)
        lockedCheckbox.setCellValue("Locked (checkbox)");

        Cell lrsCheckbox = headers.createCell(27, CellType.STRING);// LRS (checkbox)
        lrsCheckbox.setCellValue("LRS (checkbox)");

        Cell observer = headers.createCell(28, CellType.STRING);// Observer
        observer.setCellValue("Observer");

        Cell dealID = headers.createCell(29, CellType.STRING);// Deal ID
        dealID.setCellValue("Deal ID");

        Cell measurementSystem = headers.createCell(30, CellType.STRING);// Measurement system
        measurementSystem.setCellValue("Measurement system");

        Cell noData = headers.createCell(31, CellType.STRING);// No Data
        noData.setCellValue("No data");

        Cell bgNoEPRES = headers.createCell(32, CellType.STRING);// BG No EPRES
        bgNoEPRES.setCellValue("BG No EPRES");

        return sheet;
    }
}
