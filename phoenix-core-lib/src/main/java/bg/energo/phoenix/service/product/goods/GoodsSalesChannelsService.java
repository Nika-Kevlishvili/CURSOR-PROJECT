package bg.energo.phoenix.service.product.goods;

import bg.energo.phoenix.model.entity.nomenclature.product.SalesChannel;
import bg.energo.phoenix.model.entity.product.goods.Goods;
import bg.energo.phoenix.model.entity.product.goods.GoodsDetails;
import bg.energo.phoenix.model.entity.product.goods.GoodsSalesChannels;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsSubObjectStatus;
import bg.energo.phoenix.model.request.product.goods.edit.EditGoodsRequest;
import bg.energo.phoenix.model.request.product.goods.edit.GoodsSalesChannelsEditRequest;
import bg.energo.phoenix.repository.nomenclature.product.SalesChannelRepository;
import bg.energo.phoenix.repository.product.goods.GoodsSalesChannelsRepository;
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
public class GoodsSalesChannelsService {

    private final SalesChannelRepository salesChannelRepository;
    private final GoodsSalesChannelsRepository goodsSalesChannelsRepository;

    public void createGoodsSalesChannels(List<Long> salesChannelsIds, GoodsDetails goodsDetails, List<String> exceptionMessages) {
        List<GoodsSalesChannels> goodsSalesChannelsList = new ArrayList<>();
        for (Long salesChannelsId : salesChannelsIds){
            GoodsSalesChannels goodsSalesChannels = new GoodsSalesChannels();
            goodsSalesChannels.setGoodsDetails(goodsDetails);
            goodsSalesChannels.setStatus(GoodsSubObjectStatus.ACTIVE);
            goodsSalesChannels.setSalesChannel(getSalesChannel(salesChannelsId, exceptionMessages));
            goodsSalesChannelsList.add(goodsSalesChannels);
        }
        if(exceptionMessages.isEmpty()) goodsSalesChannelsRepository.saveAll(goodsSalesChannelsList);
    }
    public void createGoodsSalesChannelsEqualsCheck(GoodsDetails details, List<Long> salesChannelsIds, GoodsDetails goodsDetails, List<String> exceptionMessages) {
        List<GoodsSalesChannels> goodsSalesChannelsList = new ArrayList<>();
        for (Long salesChannelsId : salesChannelsIds){
            GoodsSalesChannels goodsSalesChannels = new GoodsSalesChannels();
            goodsSalesChannels.setGoodsDetails(goodsDetails);
            goodsSalesChannels.setStatus(GoodsSubObjectStatus.ACTIVE);
            goodsSalesChannels.setSalesChannel(checkSalesChannel(details,salesChannelsId,exceptionMessages));
            goodsSalesChannelsList.add(goodsSalesChannels);
        }
        if(exceptionMessages.isEmpty()) goodsSalesChannelsRepository.saveAll(goodsSalesChannelsList);
    }

    private SalesChannel checkSalesChannel(GoodsDetails details, Long salesChannelsId, List<String> exceptionMessages) {
        Optional<GoodsSalesChannels> goodsSalesChannelOptional =
                goodsSalesChannelsRepository.findBySalesChannelIdAndGoodsDetailsIdAndStatus(salesChannelsId,details.getId(),GoodsSubObjectStatus.ACTIVE);
        if(goodsSalesChannelOptional.isPresent()){
            return getSalesChannel(salesChannelsId, List.of(NomenclatureItemStatus.ACTIVE,NomenclatureItemStatus.INACTIVE),exceptionMessages);
        } else {
           return getSalesChannel(salesChannelsId, List.of(NomenclatureItemStatus.ACTIVE),exceptionMessages);
        }
    }

    public void createOrEditGoodsSalesChannels(Goods goods, GoodsDetails goodsDetails, EditGoodsRequest request, List<String> exceptionMessages){
        if(request.getGlobalSalesChannel()){
            goodsSalesChannelsRepository.deleteAllByDetailsId(goodsDetails.getId());
        } else {
            List<GoodsSalesChannelsEditRequest> goodsSalesChannelsEditRequestList = request.getSalesChannelsIds();
            List<GoodsSalesChannels> goodsSalesChannelsList = new ArrayList<>();
            List<GoodsSalesChannels> savedList = new ArrayList<>();
            for (GoodsSalesChannelsEditRequest item : goodsSalesChannelsEditRequestList) {
                Optional<GoodsSalesChannels> dbSalesChannelsOptional;
                if (item.getId() == null) {
                    dbSalesChannelsOptional = Optional.empty();
                } else {
                    dbSalesChannelsOptional =
                            goodsSalesChannelsRepository.findByIdAndStatus(item.getId(), GoodsSubObjectStatus.ACTIVE);
                }
                if (!dbSalesChannelsOptional.isPresent()) {
                    GoodsSalesChannels goodsSalesChannels = new GoodsSalesChannels();
                    goodsSalesChannels.setSalesChannel(getSalesChannel(item.getSalesChannelsId(), exceptionMessages));
                    goodsSalesChannels.setStatus(item.getStatus());
                    goodsSalesChannels.setGoodsDetails(goodsDetails);
                    if(goodsSalesChannels.getSalesChannel() != null){
                        goodsSalesChannelsList.add(goodsSalesChannels);
                    }
                } else {
                    GoodsSalesChannels dbSalesChannels = dbSalesChannelsOptional.get();
                    dbSalesChannels.setGoodsDetails(goodsDetails);
                    dbSalesChannels.setStatus(item.getStatus());
                    dbSalesChannels.setModifyDate(LocalDateTime.now());
                    GoodsSalesChannels goodsSalesChannels = goodsSalesChannelsRepository.save(dbSalesChannels);
                    savedList.add(goodsSalesChannels);
                }
            }
            if (!CollectionUtils.isEmpty(goodsSalesChannelsList)) {
                savedList.addAll(goodsSalesChannelsRepository.saveAll(goodsSalesChannelsList));
            }
            goodsSalesChannelsRepository.deleteAllOtherThanIdsList(
                    goodsDetails.getId(),
                    savedList.stream().map(GoodsSalesChannels::getId).collect(Collectors.toList()));
        }
    }

    private SalesChannel getSalesChannel(Long salesChannelsId, List<String> exceptionMessages) {
        Optional<SalesChannel> optionalSalesChannel = salesChannelRepository.findByIdAndStatus(salesChannelsId, List.of(NomenclatureItemStatus.ACTIVE,NomenclatureItemStatus.INACTIVE));
        if(optionalSalesChannel.isEmpty()){
            exceptionMessages.add("salesChannelsIds-Not Found Sales Channel with id: " + salesChannelsId + ";");
            return null;
        }else return optionalSalesChannel.get();
    }
    private SalesChannel getSalesChannel(Long salesChannelsId, List<NomenclatureItemStatus> statuses, List<String> exceptionMessages) {
        Optional<SalesChannel> optionalSalesChannel = salesChannelRepository.findByIdAndStatus(salesChannelsId,statuses);
        if(optionalSalesChannel.isEmpty()){
            exceptionMessages.add("salesChannelsIds-Not Found Sales Channel with id: " + salesChannelsId + ";");
            return null;
        }else return optionalSalesChannel.get();
    }
}
