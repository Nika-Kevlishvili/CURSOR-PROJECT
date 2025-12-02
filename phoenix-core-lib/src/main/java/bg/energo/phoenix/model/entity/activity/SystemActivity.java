package bg.energo.phoenix.model.entity.activity;

import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.entity.EntityStatus;
import bg.energo.phoenix.model.enums.activity.SystemActivityConnectionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "activity", schema = "activity")
public class SystemActivity extends BaseEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "activity_number")
    private Long activityNumber;

    @Column(name = "activity_id")
    private Long activityId;

    @Column(name = "sub_activity_id")
    private Long subActivityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fields", columnDefinition = "jsonb")
    private List<SystemActivityJsonField> fields;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private EntityStatus status;

    @Column(name = "connection_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private SystemActivityConnectionType connectionType;

}
