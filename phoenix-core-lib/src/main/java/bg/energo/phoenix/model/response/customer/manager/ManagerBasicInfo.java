package bg.energo.phoenix.model.response.customer.manager;

import bg.energo.phoenix.model.entity.customer.Manager;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@AllArgsConstructor
public class ManagerBasicInfo {

    private Long id;
    private Long customerDetailId;
    private String name; // name (role)
    private String managerName; // name only

    public ManagerBasicInfo(Manager manager) {
        this.id = manager.getId();
        this.customerDetailId = manager.getCustomerDetailId();
        this.name = getFormattedNameAndRole(manager).concat(" (" + manager.getJobPosition() + ")");
        this.managerName = getFormattedNameAndRole(manager);
    }

    private static String getFormattedNameAndRole(Manager manager) {
        return manager.getName()
                .concat(StringUtils.isEmpty(manager.getMiddleName()) ? "" : " " + manager.getMiddleName())
                .concat(" ")
                .concat(manager.getSurname());
    }
}
