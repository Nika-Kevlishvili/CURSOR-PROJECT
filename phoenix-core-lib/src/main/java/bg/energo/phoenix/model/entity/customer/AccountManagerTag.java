package bg.energo.phoenix.model.entity.customer;

import bg.energo.phoenix.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "account_manager_tags", schema = "customer")
@NoArgsConstructor
@AllArgsConstructor
public class AccountManagerTag extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_manager_tags_id_gen")
    @SequenceGenerator(name = "account_manager_tags_id_gen", sequenceName = "customer.account_manager_tags_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "portal_tag_id")
    private Long portalTagId;

    @Column(name = "account_manager_id")
    private Long accountManagerId;

    public AccountManagerTag(Long portalTagId, Long accountManagerId) {
        this.portalTagId = portalTagId;
        this.accountManagerId = accountManagerId;
    }
}