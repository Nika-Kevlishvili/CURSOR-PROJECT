package bg.energo.phoenix.repository.contract.action;

import bg.energo.phoenix.model.entity.contract.action.ActionSignableDocuments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionSignableDocumentRepository extends JpaRepository<ActionSignableDocuments, Long> {
}
