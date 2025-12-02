package bg.energo.phoenix.model.response.proxy;

import bg.energo.phoenix.model.entity.contract.order.goods.GoodsOrderProxyFiles;
import bg.energo.phoenix.model.entity.contract.proxy.ProductContractProxyFile;
import bg.energo.phoenix.model.entity.contract.service.ServiceContractProxyFiles;
import bg.energo.phoenix.model.entity.crm.emailCommunication.EmailCommunicationAttachment;
import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgFile;
import bg.energo.phoenix.model.entity.receivable.reconnectionOfThePowerSupply.ReconnectionOfThePowerSupplyExecutedFile;
import bg.energo.phoenix.util.epb.EPBFinalFields;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProxyFileResponse {
    private Long id;
    private String fileName;

    public ProxyFileResponse(ProductContractProxyFile proxyFile) {
        this.id = proxyFile.getId();

        try {
            this.fileName = proxyFile.getName().substring(EPBFinalFields.UUID_PREFIX_LENGTH_IN_FILE_NAME);
        } catch (Exception e) {
            this.fileName = proxyFile.getName();
        }
    }
    public ProxyFileResponse(GoodsOrderProxyFiles proxyFile) {
        this.id = proxyFile.getId();

        try {
            this.fileName = proxyFile.getName().substring(EPBFinalFields.UUID_PREFIX_LENGTH_IN_FILE_NAME);
        } catch (Exception e) {
            this.fileName = proxyFile.getName();
        }
    }

    public ProxyFileResponse(ServiceContractProxyFiles proxyFile) {
        this.id = proxyFile.getId();

        try {
            this.fileName = proxyFile.getName().substring(EPBFinalFields.UUID_PREFIX_LENGTH_IN_FILE_NAME);
        } catch (Exception e) {
            this.fileName = proxyFile.getName();
        }
    }


    public ProxyFileResponse(EmailCommunicationAttachment proxyFile) {
        this.id = proxyFile.getId();

        try {
            this.fileName = proxyFile.getName().substring(EPBFinalFields.UUID_PREFIX_LENGTH_IN_FILE_NAME);
        } catch (Exception e) {
            this.fileName = proxyFile.getName();
        }
    }

    public ProxyFileResponse(ObjectionToChangeOfCbgFile proxyFile) {
        this.id = proxyFile.getId();

        try {
            this.fileName = proxyFile.getName().substring(EPBFinalFields.UUID_PREFIX_LENGTH_IN_FILE_NAME);
        } catch (Exception e) {
            this.fileName = proxyFile.getName();
        }
    }


    public ProxyFileResponse(ReconnectionOfThePowerSupplyExecutedFile proxyFile) {
        this.id = proxyFile.getId();
        try {
            this.fileName = proxyFile.getName().substring(EPBFinalFields.UUID_PREFIX_LENGTH_IN_FILE_NAME);
        } catch (Exception e) {
            this.fileName = proxyFile.getName();
        }
    }

}
