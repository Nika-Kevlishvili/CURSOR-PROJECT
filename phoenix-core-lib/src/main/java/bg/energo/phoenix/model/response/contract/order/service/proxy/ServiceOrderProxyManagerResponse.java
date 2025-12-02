package bg.energo.phoenix.model.response.contract.order.service.proxy;

import bg.energo.phoenix.model.entity.contract.order.service.ServiceOrderProxyManager;
import bg.energo.phoenix.model.entity.customer.Manager;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
public class ServiceOrderProxyManagerResponse {
    private Long id;
    private Long managerId;
    private String name;
    private String managerName;

    public ServiceOrderProxyManagerResponse(ServiceOrderProxyManager orderProxyManager, Manager manager) {
        this.id = orderProxyManager.getId();
        this.managerId = manager.getId();
        this.managerName = "%s%s%s"
                .formatted(
                        manager.getName(),
                        StringUtils.isEmpty(manager.getMiddleName()) ? " " : " " + manager.getMiddleName() + " ",
                        manager.getSurname()
                );
        this.name = "%s%s%s (%s)"
                .formatted(
                        manager.getName(),
                        StringUtils.isEmpty(manager.getMiddleName()) ? " " : " " + manager.getMiddleName() + " ",
                        manager.getSurname(),
                        manager.getJobPosition()
                );
    }
}
