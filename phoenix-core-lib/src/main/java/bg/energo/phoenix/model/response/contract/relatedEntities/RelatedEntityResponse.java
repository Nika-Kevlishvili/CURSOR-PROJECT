package bg.energo.phoenix.model.response.contract.relatedEntities;

import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderRelatedGoodsOrder;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderRelatedGoodsOrder;
import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderRelatedServiceOrder;
import bg.energo.phoenix.model.entity.contract.product.ProductContractRelatedGoodsOrder;
import bg.energo.phoenix.model.entity.contract.product.ProductContractRelatedProductContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractRelatedServiceContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractRelatedServiceOrder;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractRelatedGoodsOrder;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractRelatedServiceContract;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractRelatedServiceOrder;
import bg.energo.phoenix.model.request.contract.relatedEntities.RelatedEntityType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RelatedEntityResponse {

    private Long id;

    private Long entityId;

    private RelatedEntityType entityType;

    private Long relatedEntityId;

    private RelatedEntityType relatedEntityType;

    private String name; // name of the relation

    private RelatedEntityType objectType; // type of the object to which the preview should be redirected

    private Long objectId; // id of the object to which the preview should be redirected

    private LocalDateTime createDate;


    public RelatedEntityResponse(ProductContractRelatedProductContract relation,
                                 String number,
                                 String objectType,
                                 long objectId) {
        this.id = relation.getId();
        this.entityId = relation.getProductContractId();
        this.entityType = RelatedEntityType.PRODUCT_CONTRACT;
        this.relatedEntityId = relation.getRelatedProductContractId();
        this.relatedEntityType = RelatedEntityType.PRODUCT_CONTRACT;
        this.name = number;
        this.objectType = RelatedEntityType.valueOf(objectType);
        this.objectId = objectId;
        this.createDate = relation.getCreateDate();
    }

    public RelatedEntityResponse(ProductContractRelatedServiceContract relation,
                                 String number,
                                 String objectType,
                                 long objectId) {
        this.id = relation.getId();
        this.entityId = relation.getProductContractId();
        this.entityType = RelatedEntityType.PRODUCT_CONTRACT;
        this.relatedEntityId = relation.getServiceContractId();
        this.relatedEntityType = RelatedEntityType.SERVICE_CONTRACT;
        this.name = number;
        this.objectType = RelatedEntityType.valueOf(objectType);
        this.objectId = objectId;
        this.createDate = relation.getCreateDate();
    }

    public RelatedEntityResponse(ProductContractRelatedServiceOrder relation,
                                 String number,
                                 String objectType,
                                 long objectId) {
        this.id = relation.getId();
        this.entityId = relation.getProductContractId();
        this.entityType = RelatedEntityType.PRODUCT_CONTRACT;
        this.relatedEntityId = relation.getServiceOrderId();
        this.relatedEntityType = RelatedEntityType.SERVICE_ORDER;
        this.name = number;
        this.objectType = RelatedEntityType.valueOf(objectType);
        this.objectId = objectId;
        this.createDate = relation.getCreateDate();
    }

    public RelatedEntityResponse(ProductContractRelatedGoodsOrder relation,
                                 String number,
                                 String objectType,
                                 long objectId) {
        this.id = relation.getId();
        this.entityId = relation.getProductContractId();
        this.entityType = RelatedEntityType.PRODUCT_CONTRACT;
        this.relatedEntityId = relation.getGoodsOrderId();
        this.relatedEntityType = RelatedEntityType.GOODS_ORDER;
        this.name = number;
        this.objectType = RelatedEntityType.valueOf(objectType);
        this.objectId = objectId;
        this.createDate = relation.getCreateDate();
    }

    public RelatedEntityResponse(ServiceContractRelatedServiceContract relation,
                                 String number,
                                 String objectType,
                                 long objectId) {
        this.id = relation.getId();
        this.entityId = relation.getServiceContractId();
        this.entityType = RelatedEntityType.SERVICE_CONTRACT;
        this.relatedEntityId = relation.getRelatedServiceContractId();
        this.relatedEntityType = RelatedEntityType.SERVICE_CONTRACT;
        this.name = number;
        this.objectType = RelatedEntityType.valueOf(objectType);
        this.objectId = objectId;
        this.createDate = relation.getCreateDate();
    }

    public RelatedEntityResponse(ServiceContractRelatedServiceOrder relation,
                                 String number,
                                 String objectType,
                                 long objectId) {
        this.id = relation.getId();
        this.entityId = relation.getServiceContractId();
        this.entityType = RelatedEntityType.SERVICE_CONTRACT;
        this.relatedEntityId = relation.getServiceOrderId();
        this.relatedEntityType = RelatedEntityType.SERVICE_ORDER;
        this.name = number;
        this.objectType = RelatedEntityType.valueOf(objectType);
        this.objectId = objectId;
        this.createDate = relation.getCreateDate();
    }

    public RelatedEntityResponse(ServiceContractRelatedGoodsOrder relation,
                                 String number,
                                 String objectType,
                                 long objectId) {
        this.id = relation.getId();
        this.entityId = relation.getServiceContractId();
        this.entityType = RelatedEntityType.SERVICE_CONTRACT;
        this.relatedEntityId = relation.getGoodsOrderId();
        this.relatedEntityType = RelatedEntityType.GOODS_ORDER;
        this.name = number;
        this.objectType = RelatedEntityType.valueOf(objectType);
        this.objectId = objectId;
        this.createDate = relation.getCreateDate();
    }

    public RelatedEntityResponse(ServiceOrderRelatedServiceOrder relation,
                                 String number,
                                 String objectType,
                                 long objectId) {
        this.id = relation.getId();
        this.entityId = relation.getServiceOrderId();
        this.entityType = RelatedEntityType.SERVICE_ORDER;
        this.relatedEntityId = relation.getRelatedServiceOrderId();
        this.relatedEntityType = RelatedEntityType.SERVICE_ORDER;
        this.name = number;
        this.objectType = RelatedEntityType.valueOf(objectType);
        this.objectId = objectId;
        this.createDate = relation.getCreateDate();
    }

    public RelatedEntityResponse(ServiceOrderRelatedGoodsOrder relation,
                                 String number,
                                 String objectType,
                                 long objectId) {
        this.id = relation.getId();
        this.entityId = relation.getServiceOrderId();
        this.entityType = RelatedEntityType.SERVICE_ORDER;
        this.relatedEntityId = relation.getGoodsOrderId();
        this.relatedEntityType = RelatedEntityType.GOODS_ORDER;
        this.name = number;
        this.objectType = RelatedEntityType.valueOf(objectType);
        this.objectId = objectId;
        this.createDate = relation.getCreateDate();
    }

    public RelatedEntityResponse(GoodsOrderRelatedGoodsOrder relation,
                                 String number,
                                 String objectType,
                                 long objectId) {
        this.id = relation.getId();
        this.entityId = relation.getGoodsOrderId();
        this.entityType = RelatedEntityType.GOODS_ORDER;
        this.relatedEntityId = relation.getRelatedGoodsOrderId();
        this.relatedEntityType = RelatedEntityType.GOODS_ORDER;
        this.name = number;
        this.objectType = RelatedEntityType.valueOf(objectType);
        this.objectId = objectId;
        this.createDate = relation.getCreateDate();
    }
}
