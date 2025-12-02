package bg.energo.phoenix.model.request.billing.companyDetails;

import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyBank;
import bg.energo.phoenix.model.request.billing.companyDetails.baseDTO.BaseCompanyBankDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class CompanyBankDTO extends BaseCompanyBankDTO {
    private Long id;

    public static CompanyBankDTO toDTO(Long bankId, String bic, String iban, Long id) {
        CompanyBankDTO companyBankDTO = new CompanyBankDTO();
        companyBankDTO.setBankId(bankId);
        companyBankDTO.setBic(bic);
        companyBankDTO.setIban(iban);
        companyBankDTO.setId(id);
        return companyBankDTO;
    }

    public static List<CompanyBankDTO> fromCompanyBankList(List<CompanyBank> dbObjList) {
        List<CompanyBankDTO> dtoList = new ArrayList<>();
        for (CompanyBank dbObj : dbObjList) {
            dtoList.add(toDTO(dbObj.getBankId(), "bic", dbObj.getIban(), dbObj.getId()));
        }
        return dtoList;
    }
}
