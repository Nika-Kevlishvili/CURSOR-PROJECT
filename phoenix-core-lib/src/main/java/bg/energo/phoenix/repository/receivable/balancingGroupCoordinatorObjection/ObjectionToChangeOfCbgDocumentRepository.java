package bg.energo.phoenix.repository.receivable.balancingGroupCoordinatorObjection;

import bg.energo.phoenix.model.entity.receivable.balancingGroupCoordinatorObjection.ObjectionToChangeOfCbgDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ObjectionToChangeOfCbgDocumentRepository extends JpaRepository<ObjectionToChangeOfCbgDocument, Long> {

    @Query(value = """
            select objectionFiles.id + 1
                                 from receivable.objection_to_change_of_cbg_doc_templates objectionFiles
                                 order by objectionFiles.id desc
                                 limit 1
            """, nativeQuery = true)
    Long getNextIdValue();

}
