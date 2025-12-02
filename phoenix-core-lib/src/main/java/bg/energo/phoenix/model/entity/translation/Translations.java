package bg.energo.phoenix.model.entity.translation;


import bg.energo.phoenix.model.entity.BaseEntity;
import bg.energo.phoenix.model.enums.translation.Language;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Builder
@Entity
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "translations", schema = "translation")
public class Translations extends BaseEntity {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "value")
    private String value;

    @Column(name = "translated_value")
    private String translatedValue;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "dest_language")
    private Language destLanguage;
}
