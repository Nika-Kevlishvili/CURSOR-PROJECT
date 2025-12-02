package bg.energo.phoenix.service.massImport.meters;

import bg.energo.phoenix.model.CacheObject;
import bg.energo.phoenix.model.CacheObjectForParent;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.pod.pod.PodStatus;
import bg.energo.phoenix.model.request.pod.meter.MeterRequest;
import bg.energo.phoenix.repository.nomenclature.product.GridOperatorRepository;
import bg.energo.phoenix.repository.nomenclature.product.priceComponent.ScalesRepository;
import bg.energo.phoenix.repository.pod.pod.PointOfDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.time.LocalDate.from;

@Service
@RequiredArgsConstructor
public class MetersExcelMapper {
    private final GridOperatorRepository gridOperatorRepository;
    private final ScalesRepository scalesRepository;
    private final PointOfDeliveryRepository podRepository;

    public MeterRequest toMetersCreateRequest(Row row, List<CacheObjectForParent> scales, List<String> errorMessages) {
        MeterRequest request = new MeterRequest();
        request.setNumber(getStringValue(1, row));
        String gridOperatorName = getStringValue(2, row);
        request.setGridOperatorId(getGridOperatorId(gridOperatorName, errorMessages));
        request.setPodId(getPodId(getStringValue(3, row), errorMessages));
        request.setInstallmentDate(getDateValue(4, row));
        request.setRemoveDate(getDateValue(5, row));

        Boolean selectAll= getBoolean(getStringValue(6,row),errorMessages,"selectAll");
        List<Long> scaleIds = new ArrayList<>();
        if(Boolean.TRUE.equals(selectAll)){
            for (CacheObjectForParent scale : scales) {
                if(scale.getParentName().equals(gridOperatorName) && scale.getStatus().equals(NomenclatureItemStatus.ACTIVE)){
                    scaleIds.add(scale.getId());
                }
            }
        }else {
            int scaleIndex = 7;

            for (CacheObjectForParent scale : scales) {
                Boolean aBoolean = getBoolean(getStringValue(scaleIndex, row), errorMessages, scale.getName());
                if (Boolean.TRUE.equals(aBoolean)) {
                    scaleIds.add(scale.getId());
                }
                scaleIndex++;
            }

        }
        request.setMeterScales(scaleIds);
        request.setWarningAcceptedByUser(true);
        return request;
    }

    private Long getPodId(String podIdentifier, List<String> errorMessages) {
        Long result = null;
        if (podIdentifier != null) {
            Optional<CacheObject> gridOperator = podRepository
                    .getCacheObjectByIdentifierAndStatus(podIdentifier, PodStatus.ACTIVE);
            if (gridOperator.isPresent()) {
                result = gridOperator.get().getId();
            } else {
                errorMessages.add("Pod-Pod not found with identifier: " + podIdentifier + ";");
            }
        }
        return result;
    }

    private Long getGridOperatorId(String gridOperatorName, List<String> errorMessages) {
        Long result = null;
        if (gridOperatorName != null) {
            Optional<CacheObject> gridOperator = gridOperatorRepository
                    .getCacheObjectByNameAndStatus(gridOperatorName, NomenclatureItemStatus.ACTIVE);
            if (gridOperator.isPresent()) {
                result = gridOperator.get().getId();
            } else {
                errorMessages.add("gridOperatorId-Not found gridOperator with name: " + gridOperatorName + ";");
            }
        }
        return result;
    }

    private Long getScale(String scaleName, List<String> errorMessages) {
        Long result = null;
        if (scaleName != null) {
            Optional<CacheObject> scale = scalesRepository
                    .getCacheObjectByNameAndStatus(scaleName, NomenclatureItemStatus.ACTIVE);
            if (scale.isPresent()) {
                result = scale.get().getId();
            } else {
                errorMessages.add("scaleId-Not found Scale with name: " + scaleName + ";");
            }
        }
        return result;
    }


    private String getStringValue(int columnNumber, Row row) {
        if (row.getCell(columnNumber) != null && row.getCell(columnNumber).getCellType() != CellType.BLANK) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.STRING);
            return row.getCell(columnNumber).getStringCellValue();
        }
        return null;
    }

    private Boolean getBoolean(String value, List<String> errorMessages, String fieldName) {
        if (value != null) {
            if (value.equalsIgnoreCase("YES")) return true;
            if (value.equalsIgnoreCase("NO")) return false;
            errorMessages.add(fieldName + "-Must be provided only YES or NO;");
        }
        return false;
    }


    private LocalDate getDateValue(int columnNumber, Row row) {
        if (row.getCell(columnNumber) != null) {
            ((XSSFCell) row.getCell(columnNumber)).setCellType(CellType.NUMERIC);
            if (row.getCell(columnNumber).getDateCellValue() != null) {
                return from(
                        LocalDate.ofInstant(
                                row.getCell(columnNumber).getDateCellValue().toInstant(), ZoneId.systemDefault()));
            }
        }
        return null;
    }

}
