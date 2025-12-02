package bg.energo.phoenix.repository.billing.billingRun;

import bg.energo.phoenix.model.entity.billing.billingRun.BillingSumFile;
import bg.energo.phoenix.model.entity.documents.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingSumFileRepository extends JpaRepository<BillingSumFile,Long> {

    @Query("""
             select doc from BillingSumFile bsf
             join Document doc on doc.id=bsf.documentId
            where bsf.billingId=:id
            """)
    List<Document> findAllByBillingId(Long id);
}
