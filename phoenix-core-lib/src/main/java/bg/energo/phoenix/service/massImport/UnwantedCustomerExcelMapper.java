package bg.energo.phoenix.service.massImport;

import bg.energo.phoenix.model.entity.customer.UnwantedCustomer;
import bg.energo.phoenix.model.entity.nomenclature.customer.UnwantedCustomerReason;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.customer.unwantedCustomer.UnwantedCustomerCreateRequest;
import bg.energo.phoenix.model.request.customer.unwantedCustomer.UnwantedCustomerEditRequest;
import bg.energo.phoenix.repository.nomenclature.customer.UnwantedCustomerReasonRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UnwantedCustomerExcelMapper {

    private final UnwantedCustomerReasonRepository reasonRepository;

    public UnwantedCustomerCreateRequest toCreateCustomerRequest(Row row, List<String> errorMessages) {
        UnwantedCustomerCreateRequest request = new UnwantedCustomerCreateRequest();
        Cell identificationNumber = row.getCell(1);
        if (identificationNumber != null) {
            request.setIdentificationNumber(identificationNumber.getStringCellValue());
        }
        Cell name = row.getCell(2);
        if (name != null) {
            request.setName(name.getStringCellValue());
        }
        Cell reason = row.getCell(3);
        if (reason != null) {
            Long reasonId = getReasonId(reason.getStringCellValue(), errorMessages);
            request.setUnwantedCustomerReasonId(reasonId);
        }
        Cell additionalInfo = row.getCell(4);
        if (additionalInfo != null) {
            request.setAdditionalInfo(additionalInfo.getStringCellValue());
        }
        Cell contractRestriction = row.getCell(5);
        if (contractRestriction != null) {
            request.setContractCreateRestriction(getBoolean(contractRestriction.getStringCellValue(), errorMessages, "contractCreateRestriction"));
        }
        Cell orderRestriction = row.getCell(6);
        if (orderRestriction != null) {
            request.setOrderCreateRestriction(getBoolean(orderRestriction.getStringCellValue(), errorMessages, "orderCreateRestriction"));
        }
        return request;
    }

    private Long getReasonId(String reasonName, List<String> errorMessages) {
        Long result = null;
        if (reasonName != null) {
            Optional<UnwantedCustomerReason> unwantedReason = reasonRepository
                    .findByNameAndStatus(reasonName, NomenclatureItemStatus.ACTIVE)
                    .stream().findFirst();
            if (unwantedReason.isPresent()) {
                result = unwantedReason.get().getId();
            } else {
                errorMessages.add("unwantedCustomerReasonId-Not found unwantedCustomer with name: " + reasonName + ";");
            }
        }
        return result;
    }

    private Boolean getBoolean(String value, List<String> errorMessages, String fieldName) {
        if (value != null) {
            if (value.equalsIgnoreCase("YES")) return true;
            if (value.equalsIgnoreCase("NO")) return false;
            errorMessages.add(fieldName + "-Must be provided only YES or NO;");
        }
        return null;
    }


    public UnwantedCustomerEditRequest createEditRequest(UnwantedCustomer unwantedCustomer, Row row, List<String> errorMessages) {
        UnwantedCustomerEditRequest request = new UnwantedCustomerEditRequest();
        updateName(unwantedCustomer, row, request);
        updateReason(unwantedCustomer, row, errorMessages, request);
        updateAdditionalInfo(unwantedCustomer, row, request);
        updateContractRestriction(unwantedCustomer, row, errorMessages, request);
        updateOrderRestriction(unwantedCustomer, row, errorMessages, request);
        request.setUnwantedCustomerStatus(unwantedCustomer.getStatus());
        return request;
    }

    private void updateOrderRestriction(UnwantedCustomer unwantedCustomer, Row row, List<String> errorMessages, UnwantedCustomerEditRequest request) {
        Cell cell = row.getCell(6);
        if (cell == null) return;
        String orderRestriction = cell.getStringCellValue();
        if (StringUtils.isNotBlank(orderRestriction)) {
            request.setOrderCreateRestriction(getBoolean(orderRestriction, errorMessages, "contractCreateRestriction"));
        } else {
            request.setOrderCreateRestriction(unwantedCustomer.getCreateOrderRestriction());
        }
    }

    private void updateContractRestriction(UnwantedCustomer unwantedCustomer, Row row, List<String> errorMessages, UnwantedCustomerEditRequest request) {
        Cell cell = row.getCell(5);
        if (cell == null) return;
        String contractRestriction = row.getCell(5).getStringCellValue();
        if (StringUtils.isNotBlank(contractRestriction)) {
            request.setContractCreateRestriction(getBoolean(contractRestriction, errorMessages, "contractCreateRestriction"));
        } else {
            request.setContractCreateRestriction(unwantedCustomer.getCreateContractRestriction());
        }
    }

    private void updateAdditionalInfo(UnwantedCustomer unwantedCustomer, Row row, UnwantedCustomerEditRequest request) {
        Cell cell = row.getCell(4);
        if (cell == null) return;
        String additionalInfo = row.getCell(4).getStringCellValue();
        if (StringUtils.isNotBlank(additionalInfo)) {
            request.setAdditionalInfo(additionalInfo);
        } else {
            request.setAdditionalInfo(unwantedCustomer.getAdditionalInfo());
        }
    }

    private void updateReason(UnwantedCustomer unwantedCustomer, Row row, List<String> errorMessages, UnwantedCustomerEditRequest request) {
        Cell cell = row.getCell(3);
        if (cell == null) return;

        String reasonName = row.getCell(3).getStringCellValue();
        if (StringUtils.isNotBlank(reasonName)) {
            Long reasonId = getReasonId(reasonName, errorMessages);
            request.setUnwantedCustomerReasonId(reasonId);
        } else {
            request.setUnwantedCustomerReasonId(unwantedCustomer.getUnwantedCustomerReasonId());
        }
    }

    private void updateName(UnwantedCustomer unwantedCustomer, Row row, UnwantedCustomerEditRequest request) {
        Cell cell = row.getCell(2);
        if (cell == null) return;
        String name = cell.getStringCellValue();
        if (StringUtils.isNotBlank(name)) {
            request.setName(name);
        } else {
            request.setName(unwantedCustomer.getName());
        }
    }
}
