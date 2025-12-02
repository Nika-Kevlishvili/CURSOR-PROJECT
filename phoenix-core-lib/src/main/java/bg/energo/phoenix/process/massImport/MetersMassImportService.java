package bg.energo.phoenix.process.massImport;

import bg.energo.phoenix.event.EventType;
import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.model.CacheObjectForParent;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.permissions.PermissionContextEnum;
import bg.energo.phoenix.process.model.enums.ProcessType;
import bg.energo.phoenix.repository.nomenclature.product.priceComponent.ScalesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;
@Slf4j
@RequiredArgsConstructor
@Service
public class MetersMassImportService extends MassImportBaseService{

    private final ScalesRepository scalesRepository;
    private static String SHEET="Sheet1";

    private static final String FILE_UPLOAD_PATH = "/meter_mass_import";
    @Override
    public DomainType getDomainType() {
        return DomainType.METERS;
    }

    @Override
    protected EventType getEventType() {
        return EventType.METER_MASS_IMPORT_PROCESS;
    }

    @Override
    protected ProcessType getProcessType() {
        return ProcessType.PROCESS_METER_MASS_IMPORT;
    }

    @Override
    protected String getFileUploadPath() {
        return this.FILE_UPLOAD_PATH;
    }

    @Override
    protected PermissionContextEnum getPermissionContext() {
        return PermissionContextEnum.METERS;
    }

    @Override
    public byte[] getMassImportTemplate() {
        try {
            var templatePath = templateRepository
                    .findById(getEventType().name())
                    .orElseThrow(() -> new DomainEntityNotFoundException("Unable to find template path for mass import process"));
            ByteArrayResource file = fileService.downloadFile(templatePath.getFileUrl());
            List<CacheObjectForParent> byStatus = scalesRepository.getCacheObjectByStatus(List.of(NomenclatureItemStatus.ACTIVE,NomenclatureItemStatus.INACTIVE));

            Workbook workbook = new XSSFWorkbook(file.getInputStream());
            Sheet sheet = workbook.getSheet(SHEET);
            int scaleIndex=7;
            for (CacheObjectForParent scale : byStatus) {
                Row row = sheet.getRow(0);
                Cell cell = row.createCell(scaleIndex, CellType.STRING);
                cell.setCellValue((scale.getParentName() + " "+ scale.getName()).replaceAll(" ","_").toLowerCase());
                scaleIndex++;
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            workbook.write(byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } catch (Exception exception) {
            log.error("Could not fetch mass import template", exception);
            throw new ClientException("Could not fetch mass import template", APPLICATION_ERROR);
        }
    }

}
