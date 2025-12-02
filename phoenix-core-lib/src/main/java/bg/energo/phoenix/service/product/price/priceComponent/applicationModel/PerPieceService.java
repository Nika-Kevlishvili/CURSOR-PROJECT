package bg.energo.phoenix.service.product.price.priceComponent.applicationModel;

import bg.energo.phoenix.model.entity.product.price.priceComponent.PriceComponent;
import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.ApplicationModel;
import bg.energo.phoenix.model.entity.product.price.priceComponent.applicationModel.PerPieceRanges;
import bg.energo.phoenix.model.enums.product.price.priceComponent.applicationModel.PerPieceStatus;
import bg.energo.phoenix.model.request.product.price.aplicationModel.ApplicationModelRequest;
import bg.energo.phoenix.model.request.product.price.aplicationModel.SettlementPeriodRange;
import bg.energo.phoenix.model.response.priceComponent.applicationModel.ApplicationModelResponse;
import bg.energo.phoenix.model.response.priceComponent.applicationModel.PerPieceResponse;
import bg.energo.phoenix.model.response.priceComponent.applicationModel.PriceRangesResponse;
import bg.energo.phoenix.repository.product.price.applicationModel.ApplicationModelRepository;
import bg.energo.phoenix.repository.product.price.applicationModel.PerPieceRangesRepository;
import bg.energo.phoenix.repository.product.price.priceComponent.PriceComponentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class PerPieceService implements ApplicationModelBaseService {
    private final PerPieceRangesRepository perPieceRangesRepository;
    private final ApplicationModelRepository applicationModelRepository;
    private final PriceComponentRepository priceComponentRepository;

    private static List<PerPieceRanges> clonePerPiceRanges(ApplicationModel copied, List<PerPieceRanges> ranges) {
        List<PerPieceRanges> tempList = new ArrayList<>();
        for (PerPieceRanges range : ranges) {
            PerPieceRanges clonedRange = new PerPieceRanges();
            clonedRange.setApplicationModel(copied);
            clonedRange.setStatus(PerPieceStatus.ACTIVE);
            clonedRange.setValueFrom(range.getValueFrom());
            clonedRange.setValueTo(range.getValueTo());
            tempList.add(clonedRange);
        }
        return tempList;
    }

    @Transactional
    public void create(ApplicationModel model, ApplicationModelRequest request) {
        List<SettlementPeriodRange> ranges = request.getPerPieceRequest().getRanges();
        List<PerPieceRanges> perPieceRanges = ranges.stream().map(range -> new PerPieceRanges(model, range)).toList();
        perPieceRangesRepository.saveAll(perPieceRanges);
    }

    @Transactional
    public void update(ApplicationModel applicationModel, ApplicationModelRequest request) {
        List<PerPieceRanges> oldPriceRanges = perPieceRangesRepository.findByApplicationModelIdAndStatusIn(applicationModel.getId(), List.of(PerPieceStatus.ACTIVE));
        oldPriceRanges.forEach(x -> x.setStatus(PerPieceStatus.DELETED));
        perPieceRangesRepository.saveAll(oldPriceRanges);
        List<SettlementPeriodRange> ranges = request.getPerPieceRequest().getRanges();
        List<PerPieceRanges> perPieceRanges = ranges.stream().map(range -> new PerPieceRanges(applicationModel, range)).toList();
        perPieceRangesRepository.saveAll(perPieceRanges);
    }

    @Transactional
    public void delete(Long modelId) {
        List<PerPieceRanges> oldPriceRanges = perPieceRangesRepository.findByApplicationModelIdAndStatusIn(modelId, List.of(PerPieceStatus.ACTIVE));
        oldPriceRanges.forEach(x -> x.setStatus(PerPieceStatus.DELETED));
        perPieceRangesRepository.saveAll(oldPriceRanges);
    }

    public ApplicationModelResponse view(ApplicationModel model) {
        List<PerPieceRanges> ranges = perPieceRangesRepository.findByApplicationModelIdAndStatusIn(model.getId(), List.of(PerPieceStatus.ACTIVE));
        List<PriceRangesResponse> responses = ranges.stream().map(PriceRangesResponse::new).toList();
        PerPieceResponse perPieceResponse = new PerPieceResponse(responses);
        ApplicationModelResponse applicationModelResponse = new ApplicationModelResponse(model.getApplicationModelType(), model.getApplicationType(), model.getApplicationLevel());
        applicationModelResponse.setPerPieceResponse(perPieceResponse);
        return applicationModelResponse;
    }

    @Override
    public void clone(ApplicationModel source, ApplicationModel clone) {
        List<PerPieceRanges> ranges = perPieceRangesRepository.findByApplicationModelIdAndStatusIn(source.getId(), List.of(PerPieceStatus.ACTIVE));
        List<PerPieceRanges> tempList = clonePerPiceRanges(clone, ranges);
        perPieceRangesRepository.saveAll(tempList);
    }

    @Override
    public boolean copy(ApplicationModel source, ApplicationModel copied, PriceComponent priceComponent) {
        priceComponentRepository.saveAndFlush(priceComponent);
        copied.setPriceComponent(priceComponent);
        applicationModelRepository.saveAndFlush(copied);
        List<PerPieceRanges> ranges = perPieceRangesRepository.findByApplicationModelIdAndStatusIn(source.getId(), List.of(PerPieceStatus.ACTIVE));
        List<PerPieceRanges> tempList = clonePerPiceRanges(copied, ranges);
        perPieceRangesRepository.saveAll(tempList);
        return true;
    }
}
