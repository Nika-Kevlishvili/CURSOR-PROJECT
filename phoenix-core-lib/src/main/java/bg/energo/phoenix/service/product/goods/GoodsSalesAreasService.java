package bg.energo.phoenix.service.product.goods;

import bg.energo.phoenix.model.entity.nomenclature.product.SalesArea;
import bg.energo.phoenix.model.entity.product.goods.Goods;
import bg.energo.phoenix.model.entity.product.goods.GoodsDetails;
import bg.energo.phoenix.model.entity.product.goods.GoodsSalesAreas;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsSubObjectStatus;
import bg.energo.phoenix.model.request.product.goods.edit.EditGoodsRequest;
import bg.energo.phoenix.model.request.product.goods.edit.GoodsSalesAreaEditRequest;
import bg.energo.phoenix.repository.nomenclature.product.SalesAreaRepository;
import bg.energo.phoenix.repository.product.goods.GoodsSalesAreasRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoodsSalesAreasService {

    private final SalesAreaRepository salesAreaRepository;
    private final GoodsSalesAreasRepository goodsSalesAreasRepository;

    public void createGoodsSalesAreas(List<Long> salesAreasIds, GoodsDetails goodsDetails, List<String> exceptionMessages) {
        List<GoodsSalesAreas> goodsSalesAreasList = new ArrayList<>();
        for(Long salesAreasId : salesAreasIds) {
            GoodsSalesAreas goodsSalesAreas = new GoodsSalesAreas();
            goodsSalesAreas.setGoodsDetails(goodsDetails);
            goodsSalesAreas.setSalesArea(getSalesArea(salesAreasId, exceptionMessages));
            goodsSalesAreas.setStatus(GoodsSubObjectStatus.ACTIVE);
            goodsSalesAreasList.add(goodsSalesAreas);
        }
        if(exceptionMessages.isEmpty()) goodsSalesAreasRepository.saveAll(goodsSalesAreasList);
    }
    public void createGoodsSalesAreasWithEqualsCheck(GoodsDetails details, List<Long> salesAreasIds, GoodsDetails goodsDetails, List<String> exceptionMessages) {
        List<GoodsSalesAreas> goodsSalesAreasList = new ArrayList<>();
        for(Long salesAreasId : salesAreasIds) {
            GoodsSalesAreas goodsSalesAreas = new GoodsSalesAreas();
            goodsSalesAreas.setGoodsDetails(goodsDetails);
            goodsSalesAreas.setSalesArea(checkSalesAreas(details,salesAreasId,exceptionMessages));
            goodsSalesAreas.setStatus(GoodsSubObjectStatus.ACTIVE);
            goodsSalesAreasList.add(goodsSalesAreas);
        }
        if(exceptionMessages.isEmpty()) goodsSalesAreasRepository.saveAll(goodsSalesAreasList);
    }

    private SalesArea checkSalesAreas(GoodsDetails details, Long salesAreasId, List<String> exceptionMessages) {
        Optional<GoodsSalesAreas> goodsSalesAreasOptional =
                goodsSalesAreasRepository.findBySalesAreaIdAndGoodsDetailsIdAndStatus(salesAreasId,details.getId(),GoodsSubObjectStatus.ACTIVE);
        if(goodsSalesAreasOptional.isPresent()){
            return getSalesArea(salesAreasId, List.of(NomenclatureItemStatus.ACTIVE,NomenclatureItemStatus.INACTIVE),exceptionMessages);
        } else {
            return getSalesArea(salesAreasId, List.of(NomenclatureItemStatus.ACTIVE),exceptionMessages);
        }
    }

    public void createOrEditGoodsSalesAreas(Goods goods, GoodsDetails goodsDetails, EditGoodsRequest request, List<String> exceptionMessages){
        if(request.getGlobalSalesArea()){
            goodsSalesAreasRepository.deleteAllByDetailsId(goodsDetails.getId());
        } else {
            List<GoodsSalesAreaEditRequest> goodsSalesAreaEditRequestList = request.getSalesAreasIds();
            List<GoodsSalesAreas> goodsSalesAreasList = new ArrayList<>();
            List<GoodsSalesAreas> savedList = new ArrayList<>();
            for (GoodsSalesAreaEditRequest item : goodsSalesAreaEditRequestList) {
                Optional<GoodsSalesAreas> dbSalesAreaOptional;
                if (item.getId() == null) {
                    dbSalesAreaOptional = Optional.empty();
                } else {
                    dbSalesAreaOptional =
                            goodsSalesAreasRepository.findByIdAndStatus(item.getId(), GoodsSubObjectStatus.ACTIVE);
                }
                if (!dbSalesAreaOptional.isPresent()) {
                    GoodsSalesAreas goodsSalesAreas = new GoodsSalesAreas();
                    goodsSalesAreas.setSalesArea(getSalesArea(item.getSalesAreaId(), exceptionMessages));
                    goodsSalesAreas.setStatus(item.getStatus());
                    goodsSalesAreas.setGoodsDetails(goodsDetails);
                    if(goodsSalesAreas.getSalesArea() != null){
                        goodsSalesAreasList.add(goodsSalesAreas);
                    }
                } else {
                    GoodsSalesAreas dbSalesArea = dbSalesAreaOptional.get();
                    dbSalesArea.setGoodsDetails(goodsDetails);
                    dbSalesArea.setStatus(item.getStatus());
                    dbSalesArea.setModifyDate(LocalDateTime.now());
                    savedList.add(goodsSalesAreasRepository.save(dbSalesArea));
                }
            }
            if (!CollectionUtils.isEmpty(goodsSalesAreasList)) {
                savedList.addAll(goodsSalesAreasRepository.saveAll(goodsSalesAreasList));
            }
            goodsSalesAreasRepository.deleteAllOtherThanIdsList(
                    goodsDetails.getId(),
                    savedList.stream().map(GoodsSalesAreas::getId).collect(Collectors.toList()));
        }
    }
    private SalesArea getSalesArea(Long salesAreasId, List<String> exceptionMessages) {
        Optional<SalesArea> optionalSalesArea = salesAreaRepository.findByIdAndStatus(salesAreasId, List.of(NomenclatureItemStatus.ACTIVE,NomenclatureItemStatus.INACTIVE));
        if(optionalSalesArea.isEmpty()){
            exceptionMessages.add("salesAreasIds-Not Found Sales Area with id: " + salesAreasId + ";");
            return null;
        }else return optionalSalesArea.get();
    }
    private SalesArea getSalesArea(Long salesAreasId, List<NomenclatureItemStatus> statuses,List<String> exceptionMessages) {
        Optional<SalesArea> optionalSalesArea = salesAreaRepository.findByIdAndStatus(salesAreasId, statuses);
        if(optionalSalesArea.isEmpty()){
            exceptionMessages.add("salesAreasIds-Not Found Sales Area with id: " + salesAreasId + ";");
            return null;
        }else return optionalSalesArea.get();
    }
}
