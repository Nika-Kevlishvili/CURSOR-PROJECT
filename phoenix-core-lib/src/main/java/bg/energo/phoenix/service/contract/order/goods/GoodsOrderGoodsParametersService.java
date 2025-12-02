package bg.energo.phoenix.service.contract.order.goods;

import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrder;
import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderGoods;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.VatRate;
import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsUnits;
import bg.energo.phoenix.model.entity.product.goods.GoodsDetails;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.enums.product.goods.GoodsDetailStatus;
import bg.energo.phoenix.model.request.contract.order.goods.GoodsOrderEditRequest;
import bg.energo.phoenix.model.request.contract.order.goods.GoodsOrderGoodsParametersRequest;
import bg.energo.phoenix.model.request.contract.order.goods.GoodsOrderGoodsParametersTableItem;
import bg.energo.phoenix.repository.contract.order.goods.GoodsOrderGoodsRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.VatRateRepository;
import bg.energo.phoenix.repository.nomenclature.product.goods.GoodsUnitsRepository;
import bg.energo.phoenix.repository.product.goods.GoodsDetailsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsOrderGoodsParametersService {
    private final VatRateRepository vatRateRepository;
    private final GoodsDetailsRepository goodsDetailsRepository;
    private final GoodsUnitsRepository goodsUnitsRepository;
    private final CurrencyRepository currencyRepository;
    private final GoodsOrderGoodsRepository goodsOrderGoodsRepository;

    @Transactional
    public void createGoodsOrderGoodsParameters(GoodsOrder goodsOrder, GoodsOrderGoodsParametersRequest request, List<String> errorMessages) {
        Long orderId = goodsOrder.getId();

        goodsOrder.setIncomeAccountNumber(request.getNumberOfIncomeAccount());
        goodsOrder.setCostCenterControllingOrder(request.getCostCenterOrControllingOrder());

        if (!request.isGlobalVatRate()) {
            Long vatRateId = request.getVatRateId();
            if (!Objects.isNull(vatRateId)) {
                Optional<VatRate> vatRateOptional = vatRateRepository
                        .findByIdAndStatus(vatRateId, List.of(NomenclatureItemStatus.ACTIVE));
                if (vatRateOptional.isEmpty()) {
                    log.error("Vat rate not found;");
                    errorMessages.add("goodsParameters.vatRateId-Vat Rate with presented ID not found;");
                } else {
                    goodsOrder.setVatRateId(vatRateId);
                }
            } else {
                errorMessages.add("goodsParameters.vatRateId-Vat Rate must not be null while global vat rate is false;");
            }
        }

        List<GoodsOrderGoods> uncommittedEntities = new ArrayList<>();

        List<GoodsOrderGoodsParametersTableItem> goods = request.getGoods();
        for (int i = 0; i < goods.size(); i++) {
            GoodsOrderGoodsParametersTableItem goodsOrderGoodsParametersTableItem = goods.get(i);
            Long goodsDetailId = goodsOrderGoodsParametersTableItem.getGoodsDetailId();

            createNewGoodsOrderGoodsAndAddToList(errorMessages, uncommittedEntities, orderId, i, goodsOrderGoodsParametersTableItem, goodsDetailId);
        }

        if (CollectionUtils.isNotEmpty(uncommittedEntities) && CollectionUtils.isEmpty(errorMessages)) {
            log.debug("Saving goods order parameters");
            goodsOrderGoodsRepository.saveAll(uncommittedEntities);
        }
    }

    private GoodsOrderGoods createGoodsOrderGoodsManually(int index, long orderId, GoodsOrderGoodsParametersTableItem goodsOrderGoodsParametersTableItem, List<String> errorMessages) {
        boolean isValid = true;

        Long goodsUnitId = goodsOrderGoodsParametersTableItem.getGoodsUnitId();
        Long currencyId = goodsOrderGoodsParametersTableItem.getCurrencyId();

        if (goodsUnitsRepository.findByIdAndStatus(goodsUnitId, List.of(NomenclatureItemStatus.ACTIVE)).isEmpty()) {
            errorMessages.add("goodsParameters.goods[%s].goodsUnitId-Goods Unit not found;".formatted(index));
            isValid = false;
        }

        if (currencyRepository.findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE)).isEmpty()) {
            errorMessages.add("goodsParameters.goods[%s].currencyId-Currency not found;".formatted(index));
            isValid = false;
        }

        if (!isValid) {
            return null;
        }

        return new GoodsOrderGoods(
                null,
                goodsOrderGoodsParametersTableItem.getName(),
                goodsOrderGoodsParametersTableItem.getCodeForConnectionWithOtherSystem(),
                goodsUnitId,
                goodsOrderGoodsParametersTableItem.getQuantity(),
                goodsOrderGoodsParametersTableItem.getPrice(),
                currencyId,
                goodsOrderGoodsParametersTableItem.getNumberOfIncomingAccount(),
                goodsOrderGoodsParametersTableItem.getCostCenterOrControllingOrder(),
                null,
                orderId
        );
    }

    private GoodsOrderGoods findGoodsAndMapToGoodsOrderGoods(int index, long orderId, GoodsOrderGoodsParametersTableItem goodsOrderGoodsParametersTableItem, List<String> errorMessages) {
        Long goodsDetailId = goodsOrderGoodsParametersTableItem.getGoodsDetailId();

        Optional<GoodsDetails> goodsDetailsOptional = goodsDetailsRepository
                .findActiveByGoodsDetailsId(goodsDetailId);
        if (goodsDetailsOptional.isEmpty()) {
            errorMessages.add("goodsParameters.goods[%s].goodsDetailId-Goods Detail with presented id: [%s] not found;".formatted(index, goodsDetailId));
        } else {
            return new GoodsOrderGoods(
                    null,
                    null,
                    null,
                    null,
                    goodsOrderGoodsParametersTableItem.getQuantity(),
                    null,
                    null,
                    null,
                    null,
                    goodsDetailId,
                    orderId
            );
        }

        return null;
    }

    public void editGoodsOrderFromRequest(GoodsOrderEditRequest request, GoodsOrder goodsOrder, List<String> errorMessages) {
        GoodsOrderGoodsParametersRequest goodsParameters = request.getGoodsParameters();

        goodsOrder.setIncomeAccountNumber(goodsParameters.getNumberOfIncomeAccount());
        goodsOrder.setCostCenterControllingOrder(goodsParameters.getCostCenterOrControllingOrder());
        goodsOrder.setGlobalVatRate(request.getGoodsParameters().isGlobalVatRate());
        if (!request.getGoodsParameters().isGlobalVatRate()) {
            if (Objects.isNull(request.getGoodsParameters().getVatRateId())) {
                errorMessages.add("goodsParameters.vatRateId-Vat Rate must not be null while global vat rate is false;");
            } else {
                goodsOrder.setVatRateId(validateVatRateAndReturnId(goodsParameters.getVatRateId(), goodsOrder, errorMessages));
            }
        } else {
            goodsOrder.setVatRateId(null);
        }

        editGoodsOrderGoods(goodsParameters.getGoods(), goodsOrder, errorMessages);
    }

    private Long validateVatRateAndReturnId(Long requestedVatRateId, GoodsOrder goodsOrder, List<String> errorMessages) {
        Optional<VatRate> requestedVatRateOptional = vatRateRepository
                .findByIdAndStatus(requestedVatRateId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));

        if (requestedVatRateOptional.isPresent()) {
            VatRate requestedVatRate = requestedVatRateOptional.get();

            if (requestedVatRate.getStatus().equals(NomenclatureItemStatus.INACTIVE)) {
                if (!goodsOrder.getVatRateId().equals(requestedVatRateId)) {
                    errorMessages.add("goodsParameters.vatRateId-You cannot assign INACTIVE vat rate to goods order;");
                }
            }
        } else {
            errorMessages.add("goodsParameters.vatRateId-Vat Rate with presented ID: [%s] not found;".formatted(requestedVatRateId));
        }

        return requestedVatRateId;
    }

    private void editGoodsOrderGoods(List<GoodsOrderGoodsParametersTableItem> goods, GoodsOrder goodsOrder, List<String> errorMessages) {
        List<GoodsOrderGoods> uncommittedEntities = new ArrayList<>();
        Long orderId = goodsOrder.getId();

        List<GoodsOrderGoods> currentGoodsOrderGoods = goodsOrderGoodsRepository.findAllByOrderId(goodsOrder.getId());
        List<Long> requestedGoodsIds = goods
                .stream()
                .map(GoodsOrderGoodsParametersTableItem::getId)
                .toList();

        for (int i = 0; i < goods.size(); i++) {
            GoodsOrderGoodsParametersTableItem goodsOrderGoodsParametersTableItem = goods.get(i);

            Long id = goodsOrderGoodsParametersTableItem.getId();
            Long requestedGoodsDetailIds = goodsOrderGoodsParametersTableItem.getGoodsDetailId();
            if (id == null) {
                createNewGoodsOrderGoodsAndAddToList(errorMessages, uncommittedEntities, orderId, i, goodsOrderGoodsParametersTableItem, requestedGoodsDetailIds);
            } else {
                Optional<GoodsOrderGoods> updatedGoodsOrderGoodsOptional = currentGoodsOrderGoods
                        .stream()
                        .filter(gog -> gog.getId().equals(id))
                        .findFirst();

                if (updatedGoodsOrderGoodsOptional.isPresent()) {
                    GoodsOrderGoods goodsOrderGoods = updatedGoodsOrderGoodsOptional.get();

                    Long goodsDetailId = goodsOrderGoodsParametersTableItem.getGoodsDetailId();
                    if (goodsDetailId == null) {
                        goodsOrderGoods.setGoodsDetailsId(null);
                        goodsOrderGoods.setName(goodsOrderGoodsParametersTableItem.getName());
                        goodsOrderGoods.setOtherSystemConnectionCode(goodsOrderGoodsParametersTableItem.getCodeForConnectionWithOtherSystem());
                        goodsOrderGoods.setGoodsUnitsId(validateGoodsUnitAndReturnId(i, goodsOrderGoodsParametersTableItem.getGoodsUnitId(), goodsOrderGoods, errorMessages));
                        goodsOrderGoods.setQuantity(goodsOrderGoodsParametersTableItem.getQuantity());
                        goodsOrderGoods.setPrice(goodsOrderGoodsParametersTableItem.getPrice());
                        goodsOrderGoods.setCurrencyId(validateCurrencyAndReturnId(i, goodsOrderGoodsParametersTableItem.getCurrencyId(), goodsOrderGoods, errorMessages));
                        goodsOrderGoods.setIncomeAccountNumber(goodsOrderGoodsParametersTableItem.getNumberOfIncomingAccount());
                        goodsOrderGoods.setCostCenterControllingOrder(goodsOrderGoodsParametersTableItem.getCostCenterOrControllingOrder());
                    } else {
                        goodsOrderGoods.setGoodsDetailsId(validateGoodsDetailAndReturnId(i, goodsOrderGoodsParametersTableItem.getGoodsDetailId(), goodsOrderGoods, errorMessages));
                        goodsOrderGoods.setName(null);
                        goodsOrderGoods.setOtherSystemConnectionCode(null);
                        goodsOrderGoods.setGoodsUnitsId(null);
                        goodsOrderGoods.setQuantity(goodsOrderGoodsParametersTableItem.getQuantity());
                        goodsOrderGoods.setPrice(null);
                        goodsOrderGoods.setCurrencyId(null);
                        goodsOrderGoods.setIncomeAccountNumber(null);
                        goodsOrderGoods.setCostCenterControllingOrder(null);
                    }

                    uncommittedEntities.add(goodsOrderGoods);
                } else {
                    errorMessages.add("goodsParameters.goods[%s].id-Good not found with presented id: [%s] for current Goods Order;".formatted(i, id));
                }
            }
        }

        if (CollectionUtils.isEmpty(errorMessages)) {
            goodsOrderGoodsRepository.saveAll(uncommittedEntities);

            goodsOrderGoodsRepository.deleteAllByIdIn(
                    currentGoodsOrderGoods
                            .stream()
                            .map(GoodsOrderGoods::getId)
                            .filter(id -> !requestedGoodsIds.contains(id))
                            .filter(Objects::nonNull)
                            .toList());
        }
    }
    private void createNewGoodsOrderGoodsAndAddToList(List<String> errorMessages, List<GoodsOrderGoods> uncommittedEntities, Long orderId, int i, GoodsOrderGoodsParametersTableItem goodsOrderGoodsParametersTableItem, Long requestedGoodsDetailIds) {
        Optional<GoodsOrderGoods> goodsOrderGoodsOptional;

        if (requestedGoodsDetailIds == null) {
            goodsOrderGoodsOptional = Optional.ofNullable(createGoodsOrderGoodsManually(i, orderId, goodsOrderGoodsParametersTableItem, errorMessages));
        } else {
            goodsOrderGoodsOptional = Optional.ofNullable(findGoodsAndMapToGoodsOrderGoods(i, orderId, goodsOrderGoodsParametersTableItem, errorMessages));
        }

        goodsOrderGoodsOptional.ifPresent(uncommittedEntities::add);
    }

    private Long validateCurrencyAndReturnId(int index, Long currencyId, GoodsOrderGoods goodsOrderGoods, List<String> errorMessages) {
        Optional<Currency> requestedCurrencyOptional = currencyRepository
                .findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));

        if (requestedCurrencyOptional.isPresent()) {
            Currency requestedCurrency = requestedCurrencyOptional.get();
            if (requestedCurrency.getStatus().equals(NomenclatureItemStatus.INACTIVE)) {
                if (!Objects.equals(goodsOrderGoods.getCurrencyId(), currencyId)) {
                    errorMessages.add("goodsParameters.goods[%s].currencyId-You cannot use INACTIVE currency for Goods Order Goods;".formatted(index));
                }
            }
        } else {
            errorMessages.add("goodsParameters.goods[%s].currencyId-Currency with presented id: [%s] not found;".formatted(index, currencyId));
        }

        return currencyId;
    }

    private Long validateGoodsUnitAndReturnId(int index, Long goodsUnitId, GoodsOrderGoods goodsOrderGoods, List<String> errorMessages) {
        Optional<GoodsUnits> requestedGoodsUnitOptional = goodsUnitsRepository
                .findByIdAndStatus(goodsUnitId, List.of(NomenclatureItemStatus.ACTIVE, NomenclatureItemStatus.INACTIVE));

        if (requestedGoodsUnitOptional.isPresent()) {
            GoodsUnits requestedGoodsUnit = requestedGoodsUnitOptional.get();
            if (requestedGoodsUnit.getStatus().equals(NomenclatureItemStatus.INACTIVE)) {
                if (!Objects.equals(goodsOrderGoods.getGoodsUnitsId(), goodsUnitId)) {
                    errorMessages.add("goodsParameters.goods[%s].goodsUnitId-You cannot use INACTIVE goods unit for Goods Order Goods;".formatted(index));
                }
            }
        } else {
            errorMessages.add("goodsParameters.goods[%s].goodsUnitId-Goods Unit with presented id: [%s] not found;".formatted(index, goodsUnitId));
        }

        return goodsUnitId;
    }

    private Long validateGoodsDetailAndReturnId(int index, Long goodsDetailId, GoodsOrderGoods goodsOrderGoods, List<String> errorMessages) {
        Optional<GoodsDetails> requestedGoodsDetailId = goodsDetailsRepository
                .findActiveByGoodsDetailsIdAndStatuses(goodsDetailId, List.of(GoodsDetailStatus.ACTIVE, GoodsDetailStatus.INACTIVE));

        if (requestedGoodsDetailId.isPresent()) {
            GoodsDetails goodsDetails = requestedGoodsDetailId.get();
            if (goodsDetails.getStatus().equals(GoodsDetailStatus.INACTIVE)) {
                if (!Objects.equals(goodsOrderGoods.getGoodsDetailsId(), goodsDetailId)) {
                    errorMessages.add("goodsParameters.goods[%s].goodsUnitId-You cannot use INACTIVE goods detail for Goods Order Goods;".formatted(index));
                }
            }
        } else {
            errorMessages.add("goodsParameters.goods[%s].goodsDetailId-Goods Detail with presented id: [%s] not found;".formatted(index, goodsDetailId));
        }
        return goodsDetailId;
    }
}
