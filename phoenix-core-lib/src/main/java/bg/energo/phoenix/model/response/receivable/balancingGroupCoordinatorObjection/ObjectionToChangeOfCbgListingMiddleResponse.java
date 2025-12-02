package bg.energo.phoenix.model.response.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.receivable.balancingGroupCoordinatorObjection.ChangeOfCbgStatus;

import java.time.LocalDate;

public interface ObjectionToChangeOfCbgListingMiddleResponse {

    Long getId();

    String getNumber();

    LocalDate getChangeDate();

    String getGridOperator();

    ChangeOfCbgStatus getChangeStatus();

    EntityStatus getEntityStatus();

    Integer getNumberOfPods();

    LocalDate getCreateDate();

}
