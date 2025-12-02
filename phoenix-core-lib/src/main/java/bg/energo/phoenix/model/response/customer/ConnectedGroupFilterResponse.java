package bg.energo.phoenix.model.response.customer;


import bg.energo.phoenix.model.enums.customer.Status;

import java.time.LocalDateTime;

public interface ConnectedGroupFilterResponse {



    Long getId();

    String getName();

    String getManagers();

    LocalDateTime getCreated();

    String getType();

    Status getStatus();

    Long getCount();


}
