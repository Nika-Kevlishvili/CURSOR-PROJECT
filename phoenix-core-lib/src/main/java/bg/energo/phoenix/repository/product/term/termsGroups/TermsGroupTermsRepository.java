package bg.energo.phoenix.repository.product.term.termsGroups;

import bg.energo.phoenix.model.entity.product.term.termsGroups.TermsGroupTerms;
import bg.energo.phoenix.model.enums.product.term.termsGroup.TermGroupStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TermsGroupTermsRepository extends JpaRepository<TermsGroupTerms, Long> {
    Optional<TermsGroupTerms> findByTermIdAndTermGroupStatusIn(Long id, List<TermGroupStatus> statuses);

    Optional<TermsGroupTerms> findByTermGroupDetailIdAndTermGroupStatusIn(Long id, List<TermGroupStatus> statuses);

    List<TermsGroupTerms> findAllByTermGroupDetailIdInAndTermGroupStatusIn(List<Long> groupDetailsId,List<TermGroupStatus> statuses);
}
