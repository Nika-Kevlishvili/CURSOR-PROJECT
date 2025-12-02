package bg.energo.phoenix.service.signing.qes.entities;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "qes_alias",schema = "template")
public class QesAlias extends BaseEntity {

    @Id
    private String id;

    @Column(name = "alias")
    private String alias;
}
