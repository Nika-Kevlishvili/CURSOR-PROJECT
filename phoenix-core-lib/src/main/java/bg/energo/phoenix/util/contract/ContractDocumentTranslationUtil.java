package bg.energo.phoenix.util.contract;

import bg.energo.phoenix.model.documentModels.contract.ContractDocumentModel;
import bg.energo.phoenix.model.documentModels.contract.ManagerModel;
import bg.energo.phoenix.model.documentModels.contract.ManagerProxyModel;
import bg.energo.phoenix.model.documentModels.contract.PodModel;
import bg.energo.phoenix.model.enums.translation.Language;
import bg.energo.phoenix.service.translation.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractDocumentTranslationUtil {

    private final TranslationService translationService;

    public void translateModel(ContractDocumentModel documentModel) {
        documentModel.HeadquarterAddressCombTrsl = translationService.translateByCharacters(documentModel.HeadquarterAddressComb, Language.ENGLISH);
        documentModel.HeadquarterPopulatedPlaceTrsl = translationService.translateByCharacters(documentModel.HeadquarterPopulatedPlace, Language.ENGLISH);
        documentModel.HeadquarterDistrictTrsl = translationService.translateByCharacters(documentModel.HeadquarterDistrict, Language.ENGLISH);
        documentModel.HeadquarterQuarterRaNameTrsl = translationService.translateByCharacters(documentModel.HeadquarterQuarterRaName, Language.ENGLISH);
        documentModel.HeadquarterStrBlvdNameTrsl = translationService.translateByCharacters(documentModel.HeadquarterStrBlvdName, Language.ENGLISH);
        documentModel.HeadquarterStrBlvdNumberTrsl = translationService.translateByCharacters(documentModel.HeadquarterStrBlvdNumber, Language.ENGLISH);
        documentModel.HeadquarterBlockTrsl = translationService.translateByCharacters(documentModel.HeadquarterBlock, Language.ENGLISH);
        documentModel.HeadquarterEntranceTrsl = translationService.translateByCharacters(documentModel.HeadquarterEntrance, Language.ENGLISH);
        documentModel.HeadquarterFloorTrsl = translationService.translateByCharacters(documentModel.HeadquarterFloor, Language.ENGLISH);
        documentModel.HeadquarterApartmentTrsl = translationService.translateByCharacters(documentModel.HeadquarterApartment, Language.ENGLISH);
        documentModel.HeadquarterAdditionalInfoTrsl = translationService.translateByCharacters(documentModel.HeadquarterAdditionalInfo, Language.ENGLISH);
        documentModel.CommunicationAddressCombTrsl = translationService.translateByCharacters(documentModel.CommunicationAddressComb, Language.ENGLISH);
        documentModel.CommunicationPopulatedPlaceTrsl = translationService.translateByCharacters(documentModel.CommunicationPopulatedPlace, Language.ENGLISH);
        documentModel.CommunicationDistrictTrsl = translationService.translateByCharacters(documentModel.CommunicationDistrict, Language.ENGLISH);
        documentModel.CommunicationQuarterRaNameTrsl = translationService.translateByCharacters(documentModel.CommunicationQuarterRaName, Language.ENGLISH);
        documentModel.CommunicationStrBlvdNameTrsl = translationService.translateByCharacters(documentModel.CommunicationStrBlvdName, Language.ENGLISH);
        documentModel.CommunicationStrBlvdNumberTrsl = translationService.translateByCharacters(documentModel.CommunicationStrBlvdNumber, Language.ENGLISH);
        documentModel.CommunicationBlockTrsl = translationService.translateByCharacters(documentModel.CommunicationBlock, Language.ENGLISH);
        documentModel.CommunicationEntranceTrsl = translationService.translateByCharacters(documentModel.CommunicationEntrance, Language.ENGLISH);
        documentModel.CommunicationFloorTrsl = translationService.translateByCharacters(documentModel.CommunicationFloor, Language.ENGLISH);
        documentModel.CommunicationApartmentTrsl = translationService.translateByCharacters(documentModel.CommunicationApartment, Language.ENGLISH);
        documentModel.CommunicationAdditionalInfoTrsl = translationService.translateByCharacters(documentModel.CommunicationAdditionalInfo, Language.ENGLISH);
        documentModel.BillingAddressCombTrsl = translationService.translateByCharacters(documentModel.BillingAddressComb, Language.ENGLISH);
        documentModel.BillingPopulatedPlaceTrsl = translationService.translateByCharacters(documentModel.BillingPopulatedPlace, Language.ENGLISH);
        documentModel.BillingDistrictTrsl = translationService.translateByCharacters(documentModel.BillingDistrict, Language.ENGLISH);
        documentModel.BillingQuarterRaNameTrsl = translationService.translateByCharacters(documentModel.BillingQuarterRaName, Language.ENGLISH);
        documentModel.BillingStrBlvdNameTrsl = translationService.translateByCharacters(documentModel.BillingStrBlvdName, Language.ENGLISH);
        documentModel.BillingStrBlvdNumberTrsl = translationService.translateByCharacters(documentModel.BillingStrBlvdNumber, Language.ENGLISH);
        documentModel.BillingBlockTrsl = translationService.translateByCharacters(documentModel.BillingBlock, Language.ENGLISH);
        documentModel.BillingEntranceTrsl = translationService.translateByCharacters(documentModel.BillingEntrance, Language.ENGLISH);
        documentModel.BillingFloorTrsl = translationService.translateByCharacters(documentModel.BillingFloor, Language.ENGLISH);
        documentModel.BillingApartmentTrsl = translationService.translateByCharacters(documentModel.BillingApartment, Language.ENGLISH);
        documentModel.BillingAdditionalInfoTrsl = translationService.translateByCharacters(documentModel.BillingAdditionalInfo, Language.ENGLISH);

        for(ManagerModel managerModel : documentModel.Managers) {
            managerModel.NameTrsl =  translationService.translateByCharacters(managerModel.Name, Language.ENGLISH);
            managerModel.SurnameTrsl = translationService.translateByCharacters(managerModel.Surname, Language.ENGLISH);
            managerModel.JobPositionTrsl =   translationService.translateByCharacters(managerModel.JobPosition, Language.ENGLISH);

            for(ManagerProxyModel managerProxyModel : managerModel.ProxyList) {
                managerProxyModel.ProxyNameTrsl =   translationService.translateByCharacters(managerProxyModel.ProxyName, Language.ENGLISH);
                managerProxyModel.NotaryPublicTrsl = translationService.translateByCharacters(managerProxyModel.NotaryPublic, Language.ENGLISH);
                managerProxyModel.OperationAreaTrsl = translationService.translateByCharacters(managerProxyModel.OperationArea, Language.ENGLISH);
            }
        }
    }

    public void translatePods(List<PodModel> podModels) {
        for(PodModel podModel : podModels){
            podModel.PODPlaceTrsl = translationService.translateByCharacters(podModel.PODPlace, Language.ENGLISH);
        }
    }
}
