package bg.energo.phoenix.model.request.billing.companyDetails;

import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyEmail;
import bg.energo.phoenix.model.entity.billing.companyDetails.CompanyTelephone;
import bg.energo.phoenix.model.request.billing.companyDetails.baseDTO.BaseCompanyCommunicationChannelDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class CompanyCommunicationChannelDTO extends BaseCompanyCommunicationChannelDTO {
    private Long id;


    public static CompanyCommunicationChannelDTO toDTO(String communicationChannel, Long id) {
        CompanyCommunicationChannelDTO companyCommunicationChannelDTO = new CompanyCommunicationChannelDTO();
        companyCommunicationChannelDTO.setCommunicationChannel(communicationChannel);
        companyCommunicationChannelDTO.setId(id);
        return companyCommunicationChannelDTO;
    }

    public static List<CompanyCommunicationChannelDTO> fromCompanyTelephoneList(List<CompanyTelephone> dbObjList) {
        List<CompanyCommunicationChannelDTO> dtoList = new ArrayList<>();
        for (CompanyTelephone dbObj : dbObjList) {
            dtoList.add(toDTO(dbObj.getTelephone(), dbObj.getId()));
        }
        return dtoList;
    }

    public static List<CompanyCommunicationChannelDTO> fromCompanyEmailList(List<CompanyEmail> dbObjList) {
        List<CompanyCommunicationChannelDTO> dtoList = new ArrayList<>();
        for (CompanyEmail dbObj : dbObjList) {
            dtoList.add(toDTO(dbObj.getEmail(), dbObj.getId()));
        }
        return dtoList;
    }


}
