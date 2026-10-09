package bg.energo.phoenix.service.massImport.contract.product;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PHN-3654 — product contract mass import, "create new version" (CV) row. Re-listing a POD that is already
 * attached to the contract must be rejected as a duplicate (POD editing is append-only), instead of the row
 * failing later with a misleading consumption-sum error caused by the double-counted estimated total.
 * <p>
 * Pure unit test of the duplicate-detection helper (the surrounding mapper method loads too many collaborators
 * to exercise in isolation; the detection logic is what needs guarding). Lives in phoenix-mass-import because
 * that is the module which pins the core-lib version carrying the fix and runs the mapper.
 */
class ProductContractExcelMapperTest {

    @Test
    void findAlreadyAttachedPodIdentifiers_flagsPodAlreadyOnContract() {
        Set<String> existing = Set.of("32XEVOHUZQZLH7596175439", "32XABC0000000000000001");

        assertThat(ProductContractExcelMapper.findAlreadyAttachedPodIdentifiers(existing, "32XEVOHUZQZLH7596175439"))
                .containsExactly("32XEVOHUZQZLH7596175439");
    }

    @Test
    void findAlreadyAttachedPodIdentifiers_returnsEmptyWhenAllPodsAreNew() {
        Set<String> existing = Set.of("32XEVOHUZQZLH7596175439");

        assertThat(ProductContractExcelMapper.findAlreadyAttachedPodIdentifiers(
                existing, "32XNEW0000000000000001,32XNEW0000000000000002"))
                .isEmpty();
    }

    @Test
    void findAlreadyAttachedPodIdentifiers_trimsWhitespaceAndDeduplicates() {
        Set<String> existing = Set.of("A", "B");

        assertThat(ProductContractExcelMapper.findAlreadyAttachedPodIdentifiers(existing, " A , C , A "))
                .containsExactly("A");
    }
}
