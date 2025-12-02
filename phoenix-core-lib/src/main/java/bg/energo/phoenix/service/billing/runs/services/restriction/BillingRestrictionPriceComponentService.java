package bg.energo.phoenix.service.billing.runs.services.restriction;

import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.*;
import bg.energo.phoenix.model.enums.product.price.priceComponent.PriceComponentStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.ApplicationModelSubObjectStatus;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.VolumesByScaleStatus;
import bg.energo.phoenix.repository.product.price.applicationModel.*;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import bg.energo.phoenix.service.billing.runs.models.restriction.CcyRestrictions;
import bg.energo.phoenix.service.billing.runs.models.restriction.KwhRestriction;
import bg.energo.phoenix.service.billing.runs.models.restriction.PriceComponentOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingRestrictionPriceComponentService {
    private final PriceComponentRepository priceComponentRepository;
    private final ApplicationModelRepository applicationModelRepository;
    //Settlement
    private final VolumesBySettlementPeriodRepository volumesBySettlementPeriodRepository;
    private final SettlementPeriodsKwhRestrictionsRepository settlementPeriodsKwhRestrictionsRepository;
    private final SettlementPeriodsCcyRestrictionsRepository settlementPeriodsCcyRestrictionsRepository;
    //scale
    private final VolumesByScaleRepository volumesByScaleRepository;
    private final VolumesByScaleKwhRestrictionRepository volumesByScaleKwhRestrictionRepository;
    private final VolumesByScaleCcyRestrictionRepository volumesByScaleCcyRestrictionRepository;


    public void getPriceComponentOptions(Long PriceComponentId, PriceComponentOptions priceComponentOptions, List<String> exceptionMessages) {
        //check price component
        Optional<PriceComponent> priceComponentOptional = priceComponentRepository.findByIdAndStatusIn(PriceComponentId, List.of(PriceComponentStatus.ACTIVE));
        if (priceComponentOptional.isEmpty()) {
            exceptionMessages.add("price component with id [%s] is not active;".formatted(PriceComponentId));
            return;
        }
        //get price component options
        PriceComponent priceComponent = priceComponentOptional.get();
        //get active application model
        Optional<ApplicationModel> applicationModelOptional = applicationModelRepository.findByPriceComponentIdAndStatusIn(priceComponent.getId(), List.of(ApplicationModelStatus.ACTIVE));
        if (applicationModelOptional.isEmpty()) {
            exceptionMessages.add("application model (for price component[%s]) is not active;".formatted(PriceComponentId));
            return;
        }
        ApplicationModel applicationModel = applicationModelOptional.get();
        switch (applicationModel.getApplicationType()) {
            case ONE_TIME, PERIODICALLY ->
                    exceptionMessages.add("incorrect application model type (for price component[%s]) oneTime or periodically;".formatted(PriceComponentId));
            case BY_SCALES ->
                    getVolumesByScalePeriodOptions(priceComponentOptions, applicationModel, exceptionMessages);
            case BY_SETTLEMENT_PERIODS ->
                    getVolumesBySettlementPeriodOptions(priceComponentOptions, applicationModel, exceptionMessages);
        }
    }

    private void getVolumesByScalePeriodOptions(PriceComponentOptions priceComponentOptions, ApplicationModel applicationModel, List<String> exceptionMessages) {
        Optional<VolumesByScale> volumesByScaleOptional = volumesByScaleRepository.findByApplicationModelIdAndStatusIn(applicationModel.getId(), List.of(VolumesByScaleStatus.ACTIVE));
        if (volumesByScaleOptional.isEmpty()) {
            exceptionMessages.add("volumes by scale not found (for applicationModel[%s]);".formatted(applicationModel.getId()));
            return;
        }
        VolumesByScale volumesByScale = volumesByScaleOptional.get();
        // witch restriction is selected
        priceComponentOptions.setIsKwtRestrictionSelected(volumesByScale.getRestrictionOfApplicationOnVolume());
        priceComponentOptions.setIsCcyRestrictionSelected(volumesByScale.getRestrictionOfApplicationOnValues());
        //get restriction options
        if (priceComponentOptions.getIsKwtRestrictionSelected()) {
            getKwtRestrictionFromVolumesByScale(priceComponentOptions, volumesByScale);
        }
        if (priceComponentOptions.getIsCcyRestrictionSelected()) {
            getCcyRestrictionsRestrictionFromVolumesByScale(priceComponentOptions, volumesByScale);
        }
    }

    private void getKwtRestrictionFromVolumesByScale(PriceComponentOptions priceComponentOptions, VolumesByScale volumesByScale) {
        //get restriction percent
        priceComponentOptions.setRestrictionPercent(volumesByScale.getVolumeRestrictionPercent());
        //get active restriction kwh
        List<VolumesByScaleKwhRestriction> volumesByScaleKwhRestriction = volumesByScaleKwhRestrictionRepository
                .findAllByVolumesByScaleIdAndStatus(volumesByScale.getId(), ApplicationModelSubObjectStatus.ACTIVE);
        List<KwhRestriction> kwhRestrictions = KwhRestriction.fromVolumesByScaleKwhRestriction(volumesByScaleKwhRestriction);
        priceComponentOptions.setKwhRestrictions(kwhRestrictions);
    }

    private void getCcyRestrictionsRestrictionFromVolumesByScale(PriceComponentOptions priceComponentOptions, VolumesByScale volumesByScale) {
        //get active restriction value base
        List<VolumesByScaleCcyRestriction> volumesByScaleCcyRestriction = volumesByScaleCcyRestrictionRepository
                .findAllByVolumesByScaleIdAndStatus(volumesByScale.getId(), ApplicationModelSubObjectStatus.ACTIVE);
        List<CcyRestrictions> ccyRestrictions = CcyRestrictions.fromVolumesByScaleCcyRestriction(volumesByScaleCcyRestriction);
        priceComponentOptions.setCcyRestrictions(ccyRestrictions);
        //check currency(is default the same)
    }

    private void getVolumesBySettlementPeriodOptions(PriceComponentOptions priceComponentOptions, ApplicationModel applicationModel, List<String> exceptionMessages) {
        //get active VolumesBySettlementPeriod
        Optional<VolumesBySettlementPeriod> volumesBySettlementPeriodOptional = volumesBySettlementPeriodRepository.findByApplicationModelIdAndStatusIn(applicationModel.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        if (volumesBySettlementPeriodOptional.isEmpty()) {
            exceptionMessages.add("volumes by settlement period not found(for applicationModel[%s]);".formatted(applicationModel.getId()));
            return;
        }
        VolumesBySettlementPeriod volumesBySettlementPeriod = volumesBySettlementPeriodOptional.get();
        // witch restriction is selected
        priceComponentOptions.setIsKwtRestrictionSelected(volumesBySettlementPeriod.getIsRestrictedBasedOnVolume());
        priceComponentOptions.setIsCcyRestrictionSelected(volumesBySettlementPeriod.getIsRestrictedBasedOnValue());
        //get restriction options
        if (priceComponentOptions.getIsKwtRestrictionSelected()) {
            getKwtRestrictionFromVolumesBySettlementPeriod(priceComponentOptions, volumesBySettlementPeriod);
        }
        if (priceComponentOptions.getIsCcyRestrictionSelected()) {
            getCcyRestrictionsFromVolumesBySettlementPeriod(priceComponentOptions, volumesBySettlementPeriod);
        }
    }

    private void getKwtRestrictionFromVolumesBySettlementPeriod(PriceComponentOptions priceComponentOptions, VolumesBySettlementPeriod volumesBySettlementPeriod) {
        //get restriction percent
        priceComponentOptions.setRestrictionPercent(volumesBySettlementPeriod.getVolumeRestrictionPercent());
        //get active restriction kwh

        List<SettlementPeriodsKwhRestrictions> settlementPeriodsKwhRestrictions = settlementPeriodsKwhRestrictionsRepository
                .findAllByVolumesBySettlementPeriodIdAndStatusIn(volumesBySettlementPeriod.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        List<KwhRestriction> kwhRestrictions = KwhRestriction.fromSettlementPeriodsKwhRestrictions(settlementPeriodsKwhRestrictions);
        priceComponentOptions.setKwhRestrictions(kwhRestrictions);
    }

    private void getCcyRestrictionsFromVolumesBySettlementPeriod(PriceComponentOptions priceComponentOptions, VolumesBySettlementPeriod volumesBySettlementPeriod) {
        //get active restriction value base
        List<SettlementPeriodsCcyRestrictions> settlementPeriodsCcyRestrictions = settlementPeriodsCcyRestrictionsRepository
                .findAllByVolumesBySettlementPeriodIdAndStatusIn(volumesBySettlementPeriod.getId(), List.of(ApplicationModelSubObjectStatus.ACTIVE));
        List<CcyRestrictions> ccyRestrictions = CcyRestrictions.fromSettlementPeriodsCcyRestrictions(settlementPeriodsCcyRestrictions);
        priceComponentOptions.setCcyRestrictions(ccyRestrictions);
    }
}
