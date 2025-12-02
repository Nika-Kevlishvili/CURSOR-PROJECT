package bg.energo.phoenix.model.entity;

import bg.energo.phoenix.model.enums.SystemMessageType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "system_messages", schema = "nomenclature")
public class SystemMessage extends BaseEntity {
    @Id
    @Column(name = "id")
    @SequenceGenerator(name = "system_messages_id_seq", sequenceName = "nomenclature.system_messages_id_seq",allocationSize =1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "system_messages_id_seq")
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "title_translated")
    private String titleTransliterated;

    @Column(name = "name")
    private String name;

    @Column(name = "message_text")
    private String messageText;

    @Column(name = "message_text_translated")
    private String messageTextTransliterated;

    @Column(name = "ok_button_text")
    private String okButtonText;

    @Column(name = "ok_button_translated_text")
    private String okButtonTextTransliterated;

    @Column(name = "no_button_text")
    private String noButtonText;

    @Column(name = "no_button_translated_text")
    private String noButtonTextTransliterated;

    @Column(name = "cancel_button_text")
    private String cancelButtonText;

    @Column(name = "cancel_button_translated_text")
    private String cancelButtonTextTransliterated;

    @Column(name = "message_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private SystemMessageType systemMessageType;

    @Column(name = "key_name")
    private String key;

    @Column(name = "ok_enabled")
    private Boolean okEnabled;

    @Column(name = "no_enabled")
    private Boolean noEnabled;

    @Column(name = "cancel_enabled")
    private Boolean cancelEnabled;
}
