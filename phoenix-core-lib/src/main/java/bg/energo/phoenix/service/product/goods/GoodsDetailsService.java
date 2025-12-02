package bg.energo.phoenix.service.product.goods;

import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsGroups;
import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsSuppliers;
import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsUnits;
import bg.energo.phoenix.model.entity.product.goods.Goods;
import bg.energo.phoenix.model.entity.product.goods.GoodsDetails;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.product.goods.CreateGoodsRequest;
import bg.energo.phoenix.model.request.product.goods.edit.EditGoodsRequest;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.VatRateRepository;
import bg.energo.phoenix.repository.nomenclature.product.goods.GoodsGroupsRepository;
import bg.energo.phoenix.repository.nomenclature.product.goods.GoodsSuppliersRepository;
import bg.energo.phoenix.repository.nomenclature.product.goods.GoodsUnitsRepository;
import bg.energo.phoenix.repository.product.goods.GoodsDetailsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoodsDetailsService {

    private final GoodsDetailsRepository goodsDetailsRepository;
    private final GoodsGroupsRepository goodsGroupsRepository;
    private final GoodsSuppliersRepository goodsSuppliersRepository;
    private final CurrencyRepository currencyRepository;
    private final GoodsUnitsRepository goodsUnitsRepository;
    private final VatRateRepository vatRateRepository;

    public GoodsDetails createGoodsDetails(CreateGoodsRequest createGoodsRequest, Goods goods, List<String> exceptionMessages) {
        GoodsDetails goodsDetails = new GoodsDetails();
        goodsDetails.setName(createGoodsRequest.getName());
        goodsDetails.setNameTransl(createGoodsRequest.getNameTransl().trim());
        goodsDetails.setPrintingName(createGoodsRequest.getPrintingName());
        goodsDetails.setPrintingNameTransl(createGoodsRequest.getPrintingNameTransl());
        goodsDetails.setGoodsGroups(getGoodsGroup(createGoodsRequest.getGoodsGroupsId(), exceptionMessages));
        goodsDetails.setOtherSystemConnectionCode(createGoodsRequest.getOtherSystemConnectionCode());
        goodsDetails.setGoodsSuppliers(getGoodsSuppliers(createGoodsRequest.getGoodsSuppliersId(),List.of(NomenclatureItemStatus.ACTIVE), exceptionMessages));
        goodsDetails.setManufacturerCodeNumber(createGoodsRequest.getManufacturerCodeNumber());
        goodsDetails.setStatus(createGoodsRequest.getGoodsDetailStatus());
        goodsDetails.setPrice(createGoodsRequest.getPrice());
        goodsDetails.setCurrency(getCurrency(createGoodsRequest.getCurrencyId(),List.of(NomenclatureItemStatus.ACTIVE), exceptionMessages));
        goodsDetails.setGoodsUnits(getGoodsUnits(createGoodsRequest.getGoodsUnitId(),List.of(NomenclatureItemStatus.ACTIVE), exceptionMessages));

        goodsDetails.setGlobalVatRate(createGoodsRequest.getGlobalVatRate());
        goodsDetails.setVatRate(getVatRate(createGoodsRequest.getVatRateId(),List.of(NomenclatureItemStatus.ACTIVE), exceptionMessages));
        goodsDetails.setGlobalSalesArea(createGoodsRequest.getGlobalSalesArea());
        goodsDetails.setGlobalSalesChannel(createGoodsRequest.getGlobalSalesChannel());
        goodsDetails.setGlobalSegment(createGoodsRequest.getGlobalSegment());
        goodsDetails.setGoods(goods);
        goodsDetails.setIncomeAccountNumbers(createGoodsRequest.getIncomeAccountNumbers());
        goodsDetails.setControllingOrderId(createGoodsRequest.getControllingOrderId());
        goodsDetails.setVersionId(1L);
        if (exceptionMessages.isEmpty()) return goodsDetailsRepository.save(goodsDetails);
        else return null;
    }

    public GoodsDetails createGoodsDetails(EditGoodsRequest detailsFromRequest, Goods goods, GoodsDetails goodsDetails, List<String> exceptionMessages){
        GoodsDetails newGoodsDetails = new GoodsDetails();
        newGoodsDetails.setName(detailsFromRequest.getName());
        newGoodsDetails.setNameTransl(detailsFromRequest.getNameTransl().trim());
        newGoodsDetails.setPrintingName(detailsFromRequest.getPrintingName());
        newGoodsDetails.setPrintingNameTransl(detailsFromRequest.getPrintingNameTransl());
        newGoodsDetails.setGoodsGroups(checkGoodsGroups(detailsFromRequest,goods,goodsDetails,exceptionMessages));
        newGoodsDetails.setOtherSystemConnectionCode(detailsFromRequest.getOtherSystemConnectionCode());
        newGoodsDetails.setGoodsSuppliers(checkGoodsSuppliers(detailsFromRequest,goods,goodsDetails,exceptionMessages));
        newGoodsDetails.setManufacturerCodeNumber(detailsFromRequest.getManufacturerCodeNumber());
        newGoodsDetails.setStatus(detailsFromRequest.getGoodsDetailStatus());
        newGoodsDetails.setPrice(detailsFromRequest.getPrice());
        newGoodsDetails.setCurrency(checkCurrency(detailsFromRequest,goods,goodsDetails,exceptionMessages));
        newGoodsDetails.setGoodsUnits(checkGoodsUnits(detailsFromRequest,goods,goodsDetails,exceptionMessages));
        newGoodsDetails.setVatRate(checkVatRate(detailsFromRequest,goods,goodsDetails,exceptionMessages));
        newGoodsDetails.setGoods(goods);
        newGoodsDetails.setIncomeAccountNumbers(detailsFromRequest.getIncomeAccountNumbers());
        newGoodsDetails.setControllingOrderId(detailsFromRequest.getControllingOrderId());
        newGoodsDetails.setVersionId(getLatestNewDetailsVersion(goods,goodsDetails,exceptionMessages));
        newGoodsDetails.setCreateDate(LocalDateTime.now());
        if(exceptionMessages.isEmpty()) return goodsDetailsRepository.save(newGoodsDetails);
        return null;
    }

    private GoodsGroups checkGoodsGroups(EditGoodsRequest detailsFromRequest, Goods goods, GoodsDetails goodsDetails, List<String> exceptionMessages) {
        Long id = detailsFromRequest.getGoodsGroupsId();
        if(id != null){
            if(id.equals(goodsDetails.getGoodsGroups().getId())){
                return goodsDetails.getGoodsGroups();
            } else
                return getGoodsGroup(detailsFromRequest.getGoodsGroupsId(), exceptionMessages);
        }
        return null;

    }

    private GoodsSuppliers checkGoodsSuppliers(EditGoodsRequest detailsFromRequest, Goods goods, GoodsDetails goodsDetails, List<String> exceptionMessages) {
        Long id = detailsFromRequest.getGoodsSuppliersId();
        if(id != null){
            if(id.equals(goodsDetails.getGoodsSuppliers().getId())){
                return goodsDetails.getGoodsSuppliers();
            } else
                return getGoodsSuppliers(id,List.of(NomenclatureItemStatus.ACTIVE,NomenclatureItemStatus.INACTIVE),exceptionMessages);
        }
        return null;
    }

    private Currency checkCurrency(EditGoodsRequest detailsFromRequest, Goods goods, GoodsDetails goodsDetails, List<String> exceptionMessages) {
        Long id = detailsFromRequest.getCurrencyId();
        if(id != null){
            if(id.equals(goodsDetails.getCurrency().getId())){
                return goodsDetails.getCurrency();
            } else
                return getCurrency(id,List.of(NomenclatureItemStatus.ACTIVE,NomenclatureItemStatus.INACTIVE),exceptionMessages);
        }
        return null;

    }

    private GoodsUnits checkGoodsUnits(EditGoodsRequest detailsFromRequest, Goods goods, GoodsDetails goodsDetails, List<String> exceptionMessages) {
        Long id = detailsFromRequest.getGoodsUnitId();
        if(id != null){
            if(id.equals(goodsDetails.getGoodsUnits().getId())){
                return goodsDetails.getGoodsUnits();
            } else
                return getGoodsUnits(id,List.of(NomenclatureItemStatus.ACTIVE,NomenclatureItemStatus.INACTIVE),exceptionMessages);
        }
        return null;
    }

    private VatRate checkVatRate(EditGoodsRequest detailsFromRequest, Goods goods, GoodsDetails goodsDetails, List<String> exceptionMessages) {
        Long id = detailsFromRequest.getVatRateId();
        if(id != null){
            if(goodsDetails.getVatRate() != null){
            if(id.equals(goodsDetails.getVatRate().getId())){
                return goodsDetails.getVatRate();
            } else
                return getVatRate(id,List.of(NomenclatureItemStatus.ACTIVE,NomenclatureItemStatus.INACTIVE),exceptionMessages);
            } else return getVatRate(id,List.of(NomenclatureItemStatus.ACTIVE,NomenclatureItemStatus.INACTIVE),exceptionMessages);
        }
        return null;
    }

    private Long getLatestNewDetailsVersion(Goods goods, GoodsDetails goodsDetails, List<String> exceptionMessages) {
        Optional<GoodsDetails> latestGoodsOptional = goodsDetailsRepository.findById(goods.getLastGoodsDetailsId());
        if (latestGoodsOptional.isPresent()) {
            GoodsDetails goodsDetail = latestGoodsOptional.get();
            Long newVersion = goodsDetail.getVersionId();
            return ++newVersion;
        } else {
            exceptionMessages.add("Can't create new version of goods details by id: " + goodsDetails.getId() + ";");
            return null;
        }
    }

    private VatRate getVatRate(Long vatRateId,List<NomenclatureItemStatus> statuses, List<String> exceptionMessages) {
        if (vatRateId == null)
            return null;
        Optional<VatRate> optionalVatRate = vatRateRepository.findByIdAndStatus(vatRateId, statuses);
        if (optionalVatRate.isEmpty()) {
            exceptionMessages.add("vatRateId-Not Found VAT Rate with id: " + vatRateId + ";");
            return null;
        } else return optionalVatRate.get();

    }

    private GoodsUnits getGoodsUnits(Long goodsUnitId, List<NomenclatureItemStatus> statuses,List<String> exceptionMessages) {
        Optional<GoodsUnits> optionalGoodsUnits = goodsUnitsRepository.findByIdAndStatus(goodsUnitId, statuses);
        if (optionalGoodsUnits.isEmpty()) {
            exceptionMessages.add("goodsUnitId-Not Found Goods Units with id: " + goodsUnitId + ";");
            return null;
        } else return optionalGoodsUnits.get();
    }

    private Currency getCurrency(Long currencyId, List<NomenclatureItemStatus> statuses,List<String> exceptionMessages) {
        Optional<Currency> optionalCurrency = currencyRepository.findByIdAndStatus(currencyId, statuses);
        if (optionalCurrency.isEmpty()) {
            exceptionMessages.add("currencyId-Not Found Currency with id: " + currencyId + ";");
            return null;
        } else return optionalCurrency.get();
    }

    private GoodsSuppliers getGoodsSuppliers(Long goodsSuppliersId, List<NomenclatureItemStatus> status, List<String> exceptionMessages) {
        Optional<GoodsSuppliers> optionalGoodsSuppliers = goodsSuppliersRepository.findByIdAndStatus(goodsSuppliersId, status);
        if (optionalGoodsSuppliers.isEmpty()) {
            exceptionMessages.add("goodsSuppliersId-Not Found Goods Suppliers with id: " + goodsSuppliersId + ";");
            return null;
        } else return optionalGoodsSuppliers.get();
    }

    private GoodsGroups getGoodsGroup(Long goodsGroupsId, List<String> exceptionMessages) {
        Optional<GoodsGroups> optionalGoodsGroups = goodsGroupsRepository.findByIdAndStatus(goodsGroupsId, List.of(NomenclatureItemStatus.ACTIVE));
        if (optionalGoodsGroups.isEmpty()) {
            exceptionMessages.add("goodsGroupsId-Not Found Goods Groups with id: " + goodsGroupsId + ";");
            return null;
        } else return optionalGoodsGroups.get();
    }
}
