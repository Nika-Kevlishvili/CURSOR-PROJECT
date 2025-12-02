package phoenix.core.customer.model.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Column(name = "system_user_id", nullable = false, length = 10)
    private String systemUserId;

    @Column(name = "create_date", nullable = false)
    private LocalDateTime createDate;

    @Column(name = "modify_date")
    private LocalDateTime modifyDate;

    @Column(name = "modify_system_user_id", length = 10)
    private String modifySystemUserId;

}

