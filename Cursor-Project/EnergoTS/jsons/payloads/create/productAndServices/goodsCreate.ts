import { envVariables } from "../../../../fixtures/envCashed";
import { randomGens } from "../../../../utils/randomGens";

// -------------------- Type Definitions --------------------
type GoodsDetailStatus = 'ACTIVE' | 'INACTIVE' | 'NEW';

type GoodsPayload = {
    name: string;
    nameTransl: string;
    printingName: string;
    printingNameTransl: string;
    goodsGroupsIdTrans: number;
    otherSystemConnectionCode: string;
    manufacturerCodeNumber: string;
    price: string | number;
    incomeAccountNumbers: string;
    controllingOrderId: string;
    goodsGroupsId: number;
    goodsSuppliersId: number;
    currencyId: number;
    vatRateId: number | null;
    goodsUnitId: number;
    goodsDetailStatus: GoodsDetailStatus;
    salesAreasIds: number[] | null;
    salesChannelsIds: number[] | null;
    segmentsIds: number[] | null;
    globalVatRate: boolean;
    globalSalesArea: boolean;
    globalSalesChannel: boolean;
    globalSegment: boolean;
}

export function Goods(): GoodsPayload {
    return {
        "name": randomGens.generateRandomString(true,false, 10),
        "nameTransl": randomGens.generateRandomString(true,false, 10),
        "printingName": randomGens.generateRandomString(true,false, 10),
        "printingNameTransl": randomGens.generateRandomString(true,false, 10),
        "goodsGroupsIdTrans": envVariables.goods_group,
        "otherSystemConnectionCode": "4",
        "manufacturerCodeNumber": "1",
        "price": "1",
        "incomeAccountNumbers": "1",
        "controllingOrderId": "2",
        "goodsGroupsId": envVariables.goods_group,
        "goodsSuppliersId": envVariables.goods_supplier,
        "currencyId": envVariables.currency,
        "vatRateId": null,
        "goodsUnitId": envVariables.goods_unit,
        "goodsDetailStatus": "ACTIVE",
        "salesAreasIds": null,
        "salesChannelsIds": null,
        "segmentsIds": null,
        "globalVatRate": true,
        "globalSalesArea": true,
        "globalSalesChannel": true,
        "globalSegment": true
    }
}