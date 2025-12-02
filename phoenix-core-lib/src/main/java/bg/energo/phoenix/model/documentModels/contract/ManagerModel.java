package bg.energo.phoenix.model.documentModels.contract;

import bg.energo.phoenix.model.documentModels.contract.response.ManagersResponse;

import java.util.List;

public class ManagerModel {
    public String Title;
    public String Name;
    public String NameTrsl;
    public String Surname;
    public String SurnameTrsl;
    public String JobPosition;
    public String JobPositionTrsl;
    public List<ManagerProxyModel> ProxyList;


    public ManagerModel from(ManagersResponse response,
                     List<ManagerProxyModel> proxyList) {
        this.Title = response.getTitle();
        this.Name = response.getName();
        this.NameTrsl = response.getName(); //todo
        this.Surname = response.getSurname();
        this.SurnameTrsl = response.getSurname();//todo
        this.JobPosition = response.getJobPosition();
        this.JobPositionTrsl = response.getJobPosition();//todo
        this.ProxyList = proxyList;
        return this;
    }
}
