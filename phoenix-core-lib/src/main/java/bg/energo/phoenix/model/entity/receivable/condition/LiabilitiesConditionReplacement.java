package bg.energo.phoenix.model.entity.receivable.condition;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Entity
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@Table(name = "liabilities_condition_replacemets", schema = "receivable")
public class LiabilitiesConditionReplacement {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "condition_text")
    private String conditionText;

    @Column(name = "replacement_text")
    private String replacementText;

    @Column(name = "is_key")
    private Boolean isKey;
}
