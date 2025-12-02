package bg.energo.phoenix.model.documentModels.contract;

import bg.energo.phoenix.model.documentModels.contract.response.ManagerProxyResponse;

public class ManagerProxyModel {
    public String ProxyName;
    public String ProxyNameTrsl;
    public String PowerAttroneyNumber;
    public String NotaryPublic;
    public String NotaryPublicTrsl;
    public String OperationArea;
    public String OperationAreaTrsl;
    public String RegistrationNumber;

    public ManagerProxyModel from(ManagerProxyResponse response) {
        this.ProxyName = response.getProxyName();
        this.ProxyNameTrsl = response.getProxyName(); //todo
        this.PowerAttroneyNumber = response.getPowerAttroneyNumber();
        this.NotaryPublic = response.getNotaryPublic();
        this.NotaryPublicTrsl = response.getNotaryPublic();//todo
        this.OperationArea = response.getOperationArea();
        this.OperationAreaTrsl = response.getOperationArea();//todo
        this.RegistrationNumber=response.getRegistrationNumber();
        return this;
    }
}
