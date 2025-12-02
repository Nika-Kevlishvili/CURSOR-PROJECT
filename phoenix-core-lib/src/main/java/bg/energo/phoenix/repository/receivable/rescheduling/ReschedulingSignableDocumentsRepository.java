package bg.energo.phoenix.repository.receivable.rescheduling;

import bg.energo.phoenix.model.entity.receivable.rescheduling.ReschedulingSignableDocuments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReschedulingSignableDocumentsRepository extends JpaRepository<ReschedulingSignableDocuments, Long> {
}
