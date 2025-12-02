package bg.energo.phoenix.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@SuperBuilder
@MappedSuperclass
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Column(name = "system_user_id", nullable = false, length = 50)
    @CreatedBy
    private String systemUserId;

    @CreationTimestamp
    @Column(name = "create_date", nullable = false)
    private LocalDateTime createDate;

    @UpdateTimestamp
    @Column(name = "modify_date")
    private LocalDateTime modifyDate;

    @Column(name = "modify_system_user_id", length = 50)
    @LastModifiedBy
    private String modifySystemUserId;

}

