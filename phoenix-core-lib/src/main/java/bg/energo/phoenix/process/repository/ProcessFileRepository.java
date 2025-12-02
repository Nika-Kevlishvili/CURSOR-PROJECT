package bg.energo.phoenix.process.repository;

import bg.energo.phoenix.model.response.shared.ShortResponse;
import bg.energo.phoenix.process.model.entity.ProcessFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessFileRepository extends JpaRepository<ProcessFile, Long> {

    @Query(
            """
                    select new bg.energo.phoenix.model.response.shared.ShortResponse(pf.id,pf.name)
                    from ProcessFile pf where pf.processId=:processId
                    """
    )
    List<ShortResponse> getProcessFilesByProcessId(Long processId);

}
