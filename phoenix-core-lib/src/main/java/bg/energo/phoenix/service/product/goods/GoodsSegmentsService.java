package bg.energo.phoenix.service.product.goods;

import bg.energo.phoenix.model.entity.nomenclature.customer.Segment;
import bg.energo.phoenix.model.entity.product.goods.Goods;
import bg.energo.phoenix.model.entity.product.goods.GoodsDetails;
import bg.energo.phoenix.model.entity.product.goods.GoodsSegments;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsSubObjectStatus;
import bg.energo.phoenix.model.request.product.goods.edit.EditGoodsRequest;
import bg.energo.phoenix.model.request.product.goods.edit.GoodsSegmentsEditRequest;
import bg.energo.phoenix.repository.nomenclature.customer.SegmentRepository;
import bg.energo.phoenix.repository.product.goods.GoodsSegmentsRepository;
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
public class GoodsSegmentsService {

    private final SegmentRepository segmentRepository;
    private final GoodsSegmentsRepository goodsSegmentsRepository;

    public void createGoodsSegments(List<Long> segmentsIds, GoodsDetails goodsDetails, List<String> exceptionMessages) {
        List<GoodsSegments> goodsSegmentsList = new ArrayList<>();
        for (Long segmentId : segmentsIds) {
            GoodsSegments goodsSegments = new GoodsSegments();
            goodsSegments.setGoodsDetails(goodsDetails);
            goodsSegments.setStatus(GoodsSubObjectStatus.ACTIVE);
            goodsSegments.setSegment(getSegment(segmentId, exceptionMessages));
            goodsSegmentsList.add(goodsSegments);
        }
        if (exceptionMessages.isEmpty()) goodsSegmentsRepository.saveAll(goodsSegmentsList);
    }
    public void createGoodsSegmentsWithEqualsCheck(GoodsDetails details, List<Long> segmentsIds, GoodsDetails goodsDetails, List<String> exceptionMessages) {
        List<GoodsSegments> goodsSegmentsList = new ArrayList<>();
        for (Long segmentId : segmentsIds) {
            GoodsSegments goodsSegments = new GoodsSegments();
            goodsSegments.setGoodsDetails(goodsDetails);
            goodsSegments.setStatus(GoodsSubObjectStatus.ACTIVE);
            goodsSegments.setSegment(checkGoodsSegments(details,segmentId,exceptionMessages));
            goodsSegmentsList.add(goodsSegments);
        }
        if (exceptionMessages.isEmpty()) goodsSegmentsRepository.saveAll(goodsSegmentsList);
    }

    private Segment checkGoodsSegments(GoodsDetails details, Long segmentId, List<String> exceptionMessages) {
        Optional<GoodsSegments> goodsSegmentsOptional =
                goodsSegmentsRepository.findByGoodsDetailsIdAndSegmentIdAndStatus(details.getId(),segmentId,GoodsSubObjectStatus.ACTIVE);
        if(goodsSegmentsOptional.isPresent()){
            return getSegment(segmentId, List.of(NomenclatureItemStatus.ACTIVE,NomenclatureItemStatus.INACTIVE),exceptionMessages);
        } else {
            return getSegment(segmentId, List.of(NomenclatureItemStatus.ACTIVE),exceptionMessages);
        }
    }

    public void createOrEditGoodsSegments(Goods goods, GoodsDetails goodsDetails, EditGoodsRequest request, List<String> exceptionMessages) {
        if (request.getGlobalSegment()) {
            goodsSegmentsRepository.deleteAllByDetailsId(goodsDetails.getId());
        } else {
            List<GoodsSegmentsEditRequest> goodsSegmentsEditRequestList = request.getSegmentsIds();
            List<GoodsSegments> goodsSegmentsList = new ArrayList<>();
            List<GoodsSegments> savedList = new ArrayList<>();
            for (GoodsSegmentsEditRequest item : goodsSegmentsEditRequestList) {
                Optional<GoodsSegments> segmentOptional;
                if (item.getId() == null) {
                    segmentOptional = Optional.empty();
                } else {
                    segmentOptional =
                            goodsSegmentsRepository.findByIdAndStatusIn(item.getId(), List.of(GoodsSubObjectStatus.ACTIVE, GoodsSubObjectStatus.DELETED));
                }
                if (!segmentOptional.isPresent()) {
                    GoodsSegments goodsSegment = new GoodsSegments();
                    goodsSegment.setSegment(getSegment(item.getSegmentId(), exceptionMessages));
                    goodsSegment.setStatus(item.getStatus());
                    goodsSegment.setGoodsDetails(goodsDetails);
                    if(goodsSegment.getSegment() != null){
                        goodsSegmentsList.add(goodsSegment);
                    }
                } else {
                    GoodsSegments dbGoodsSegments = segmentOptional.get();
                    dbGoodsSegments.setGoodsDetails(goodsDetails);
                    dbGoodsSegments.setStatus(item.getStatus());
                    dbGoodsSegments.setModifyDate(LocalDateTime.now());
                    savedList.add(goodsSegmentsRepository.save(dbGoodsSegments));
                }
            }

            if (!CollectionUtils.isEmpty(goodsSegmentsList)) {
                savedList.addAll(goodsSegmentsRepository.saveAll(goodsSegmentsList));
            }
            goodsSegmentsRepository.deleteAllOtherThanIdsList(
                    goodsDetails.getId(),
                    savedList.stream().map(GoodsSegments::getId).collect(Collectors.toList()));
        }
    }

    private Segment getSegment(Long segmentId, List<String> exceptionMessages) {
        Optional<Segment> optionalSegment = segmentRepository.findByIdAndStatus(segmentId, List.of(NomenclatureItemStatus.ACTIVE,NomenclatureItemStatus.INACTIVE));
        if (optionalSegment.isEmpty()) {
            exceptionMessages.add("segmentsIds-Not Found Segment with id: " + segmentId + ";");
            return null;
        } else return optionalSegment.get();
    }
    private Segment getSegment(Long segmentId, List<NomenclatureItemStatus> statuses,List<String> exceptionMessages) {
        Optional<Segment> optionalSegment = segmentRepository.findByIdAndStatus(segmentId, statuses);
        if (optionalSegment.isEmpty()) {
            exceptionMessages.add("segmentsIds-Not Found Segment with id: " + segmentId + ";");
            return null;
        } else return optionalSegment.get();
    }

}
