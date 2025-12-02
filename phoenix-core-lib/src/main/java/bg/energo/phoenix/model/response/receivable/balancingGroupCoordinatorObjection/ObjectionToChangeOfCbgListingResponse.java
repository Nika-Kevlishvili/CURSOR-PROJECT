package bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.balancingGroupCoordinatorObjection.ChangeOfCbgStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ObjectionToChangeOfCbgListingResponse {

    private Long id;

    private String number;

    private LocalDate dateOfChange;

    private String gridOperator;

    private ChangeOfCbgStatus changeStatus;

    private EntityStatus entityStatus;

    private Integer countPod;

    private LocalDate creationDate;

    public ObjectionToChangeOfCbgListingResponse(ObjectionToChangeOfCbgListingMiddleResponse middleResponse) {
        this.id = middleResponse.getId();
        this.number = middleResponse.getNumber();
        this.dateOfChange = middleResponse.getChangeDate();
        this.gridOperator = middleResponse.getGridOperator();
        this.changeStatus = middleResponse.getChangeStatus();
        this.entityStatus = middleResponse.getEntityStatus();
        this.countPod = middleResponse.getNumberOfPods();
        this.creationDate = middleResponse.getCreateDate();
    }
}
