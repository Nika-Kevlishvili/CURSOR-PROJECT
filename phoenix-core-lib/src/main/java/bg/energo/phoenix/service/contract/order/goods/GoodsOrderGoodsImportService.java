package bg.energo.phoenix.service.contract.order.goods;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.exception.DomainEntityNotFoundException;
import bg.energo.phoenix.exception.IllegalArgumentsProvidedException;
import bg.energo.phoenix.model.entity.nomenclature.product.Currency;
import bg.energo.phoenix.model.entity.nomenclature.product.goods.GoodsUnits;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.contract.order.goods.GoodsOrderGoodsParametersTableItem;
import bg.energo.phoenix.model.request.contract.order.goods.GoodsOrderGoodsParametersTableItemResponse;
import bg.energo.phoenix.model.response.nomenclature.goods.GoodsUnitsShortResponse;
import bg.energo.phoenix.model.response.nomenclature.product.currency.CurrencyShortResponse;
import bg.energo.phoenix.process.model.entity.Template;
import bg.energo.phoenix.process.repository.TemplateRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.nomenclature.product.goods.GoodsUnitsRepository;
import bg.energo.phoenix.service.document.ftpService.FileService;
import bg.energo.phoenix.util.epb.EPBExcelUtils;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

import static bg.energo.phoenix.exception.ErrorCode.APPLICATION_ERROR;
import static bg.energo.phoenix.exception.ErrorCode.DOMAIN_ENTITY_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsOrderGoodsImportService {
    private final TemplateRepository templateRepository;
    private final FileService fileService;
    private final GoodsUnitsRepository goodsUnitsRepository;
    private final CurrencyRepository currencyRepository;

    public List<GoodsOrderGoodsParametersTableItemResponse> validateFileContentAndMapToResponse(MultipartFile file) {
        EPBExcelUtils.validateFileFormat(file);

        Template template = templateRepository
                .findById(EPBFinalFields.IMPORT_TEMPLATE_ID)
                .orElseThrow(() -> new DomainEntityNotFoundException("Template for Goods Order Goods not found;"));

        EPBExcelUtils.validateFileContent(file, fileService.downloadFile(template.getFileUrl()).getByteArray(), 1);

        List<GoodsOrderGoodsParametersTableItemResponse> result = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            Iterator<Row> iterator = sheet.iterator();

            if (iterator.hasNext()) iterator.next(); // skip headers

            while (iterator.hasNext()) {
                Row row = iterator.next();

                String goodsOrderName = EPBExcelUtils.getStringValue(0, row);
                String goodsOrderCode = EPBExcelUtils.getStringValue(1, row);
                String goodsOrderGoodsUnitId = EPBExcelUtils.getStringValue(2, row);
                Integer quantity = EPBExcelUtils.getIntegerValue(3, row);
                String priceStringValue = EPBExcelUtils.getStringValue(4, row);
                BigDecimal price = StringUtils.isBlank(priceStringValue) ? null : new BigDecimal(priceStringValue);
                String currencyName = EPBExcelUtils.getStringValue(5, row);
                String numberOfIncomingAccount = EPBExcelUtils.getStringValue(6, row);
                String costCenterControllingOrder = EPBExcelUtils.getStringValue(7, row);

                GoodsOrderGoodsParametersTableItem item = new GoodsOrderGoodsParametersTableItem(
                        null,
                        null,
                        goodsOrderName,
                        goodsOrderCode,
                        getGoodsUnit(goodsUnitsRepository.findByNameAndStatusIn(goodsOrderGoodsUnitId,List.of(NomenclatureItemStatus.ACTIVE)),goodsOrderGoodsUnitId),
                        quantity,
                        price,
                        getCurrency(currencyRepository.findByNameAndStatusIn(currencyName,List.of(NomenclatureItemStatus.ACTIVE)),currencyName),
                        numberOfIncomingAccount,
                        costCenterControllingOrder
                );

                result.add(validateGoodsOrderGoods(item, row.getRowNum() + 1));
            }
        } catch (IllegalArgumentsProvidedException e) {
            log.error("Illegal arguments provided in file", e);
            throw e;
        } catch (Exception e) {
            log.error("Exception handled while trying to parse uploaded template;", e);
            throw new ClientException("Exception handled while trying to parse uploaded template;", APPLICATION_ERROR);
        }

        return result;
    }

    private Long getCurrency(Optional<Currency> currencyId, String currencyName) {
        if(currencyId.isPresent()){
            return currencyId.get().getId();
        } else throw new ClientException("Can't find currency with name:%s".formatted(currencyName), DOMAIN_ENTITY_NOT_FOUND);
    }

    private Long getGoodsUnit(Optional<GoodsUnits> byNameAndStatusIn, String goodsOrderGoodsUnitId) {
        if(byNameAndStatusIn.isPresent()){
            return byNameAndStatusIn.get().getId();
        } else throw new ClientException("Can't find goods unit with given name %s;".formatted(goodsOrderGoodsUnitId), DOMAIN_ENTITY_NOT_FOUND);
    }

    private GoodsOrderGoodsParametersTableItemResponse validateGoodsOrderGoods(GoodsOrderGoodsParametersTableItem item, int i) {
        GoodsOrderGoodsParametersTableItemResponse response = new GoodsOrderGoodsParametersTableItemResponse();
        Range<Integer> codeForConnectionWithOtherSystemRange = Range.between(1, 256);
        Range<Integer> quantityRange = Range.between(1, 9999);
        Range<BigDecimal> priceRange = Range.between(new BigDecimal("0.01"), new BigDecimal("999999999999.99"), Comparator.naturalOrder());
        Range<Integer> numberOfIncomingAccountRange = Range.between(1, 32);
        Range<Integer> costCenterOrControllingOrderRange = Range.between(1, 32);
        Range<Integer> nameLengthRange = Range.between(1, 1024);

        String name = item.getName();
        String codeForConnectionWithOtherSystem = item.getCodeForConnectionWithOtherSystem();
        Long goodsUnitId = item.getGoodsUnitId();
        Integer quantity = item.getQuantity();
        BigDecimal price = item.getPrice();
        Long currencyId = item.getCurrencyId();
        String numberOfIncomingAccount = item.getNumberOfIncomingAccount();
        String costCenterOrControllingOrder = item.getCostCenterOrControllingOrder();

        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentsProvidedException("goods[%s].goods_order_name-Name must not be blank;".formatted(i));
        }

        name = name.trim();

        if (!nameLengthRange.contains(name.length())) {
            throw new IllegalArgumentsProvidedException("goods[%s].goods_order_name-Name length must be in range: min [%s] and max [%s];".formatted(i, nameLengthRange.getMinimum(), nameLengthRange.getMaximum()));
        }

        if (codeForConnectionWithOtherSystem != null) {
            if (!codeForConnectionWithOtherSystemRange.contains(codeForConnectionWithOtherSystem.length())) {
                throw new IllegalArgumentsProvidedException("goods[%s].goods_order_code-Code For Connection With Other System length must be in range: min [%s] and max [%s];".formatted(i, codeForConnectionWithOtherSystemRange.getMinimum(), codeForConnectionWithOtherSystemRange.getMaximum()));
            }
        }

        if (goodsUnitId == null) {
            throw new IllegalArgumentsProvidedException("goods[%s].goods_order_goods_unit-Goods Unit ID must not be null;".formatted(i));
        }

        GoodsUnits goodsUnit = goodsUnitsRepository
                .findByIdAndStatus(goodsUnitId, List.of(NomenclatureItemStatus.ACTIVE))
                .orElseThrow(() -> new IllegalArgumentsProvidedException("goods[%s].goods_order_goods_unit-Goods Unit with presented id: [%s] not found;".formatted(i, goodsUnitId)));

        if (!quantityRange.contains(quantity)) {
            throw new IllegalArgumentsProvidedException("goods[%s].goods_order_quantity-Quantity must be in range: min [%s] and max [%s];".formatted(i, quantityRange.getMinimum(), quantityRange.getMaximum()));
        }

        if (Objects.isNull(price)) {
            throw new IllegalArgumentsProvidedException("goods[%s].goods_order_price-Price must not be null;".formatted(i));
        } else {
            int scale = price.scale();
            int precision = price.precision();
            if ((scale > 2) || (precision > 15)) {
                throw new IllegalArgumentsProvidedException("goods[%s].goods_order_price-Invalid price format, correct format is: scale of [%s] and precision of [%s];".formatted(i, 2, 15));
            }
        }

        if (!priceRange.contains(price)) {
            throw new IllegalArgumentsProvidedException("goods[%s].goods_order_price-Price must be in range: min [%s] and max [%s];".formatted(i, priceRange.getMinimum(), priceRange.getMaximum()));
        }

        if (currencyId == null) {
            throw new IllegalArgumentsProvidedException("goods[%s].goods_order_currency-Currency ID must not be null;".formatted(i));
        }

        Currency currency = currencyRepository
                .findByIdAndStatus(currencyId, List.of(NomenclatureItemStatus.ACTIVE))
                .orElseThrow(() -> new IllegalArgumentsProvidedException("goods[%s].goods_order_currency-Currency with presented id: [%s] not found;".formatted(i, currencyId)));

        if (numberOfIncomingAccount != null) {
            if (!numberOfIncomingAccountRange.contains(numberOfIncomingAccount.length())) {
                throw new IllegalArgumentsProvidedException("goods[%s].goods_order_number_of_incoming_account-Number of Incoming Account length must be in range: min [%s] and max [%s];".formatted(i, numberOfIncomingAccountRange.getMinimum(), numberOfIncomingAccountRange.getMaximum()));
            }
        }

        if (costCenterOrControllingOrder != null) {
            if (!costCenterOrControllingOrderRange.contains(costCenterOrControllingOrder.length())) {
                throw new IllegalArgumentsProvidedException("goods[%s].goods_order_cost_center_controlling_order-Cost Center Or Controlling Order length must be in range: min [%s] and max [%s];".formatted(i, costCenterOrControllingOrderRange.getMinimum(), costCenterOrControllingOrderRange.getMaximum()));
            }
        }

        response.setName(name);
        response.setCodeForConnectionWithOtherSystem(codeForConnectionWithOtherSystem);
        response.setGoodsUnit(new GoodsUnitsShortResponse(goodsUnit));
        response.setQuantity(quantity);
        response.setPrice(price);
        response.setCurrency(new CurrencyShortResponse(currency));
        response.setNumberOfIncomingAccount(numberOfIncomingAccount);
        response.setCostCenterOrControllingOrder(costCenterOrControllingOrder);

        return response;
    }
}
