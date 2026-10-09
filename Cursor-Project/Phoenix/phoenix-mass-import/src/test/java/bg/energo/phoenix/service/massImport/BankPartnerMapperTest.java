package bg.energo.phoenix.service.massImport;

import bg.energo.phoenix.model.entity.nomenclature.billing.Prefix;
import bg.energo.phoenix.repository.billing.accountingPeriods.AccountingPeriodsRepository;
import bg.energo.phoenix.repository.billing.invoice.InvoiceRepository;
import bg.energo.phoenix.repository.contract.action.ActionRepository;
import bg.energo.phoenix.repository.contract.action.ClaimedPenaltyRepository.ClaimedPenaltyRepository;
import bg.energo.phoenix.repository.contract.billing.ContractBillingGroupRepository;
import bg.energo.phoenix.repository.customer.CustomerRepository;
import bg.energo.phoenix.repository.nomenclature.billing.PrefixRepository;
import bg.energo.phoenix.repository.nomenclature.product.CurrencyRepository;
import bg.energo.phoenix.repository.receivable.customerLiability.CustomerLiabilityRepository;
import bg.energo.phoenix.repository.receivable.deposit.DepositRepository;
import bg.energo.phoenix.repository.receivable.latePaymentFine.LatePaymentFineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * PHN-3938 — bank payment mass import. A "+" in the payment reference must stay in "Purpose of payment" instead of
 * cutting off the text up to the next sub-tag. '?' and '+' are sub-tag separators only in front of a two-digit sub-tag
 * number, and a :86: field uses one of them (UniCredit "835?20...", DSK "020+15+" with one sub-tag per line), so the
 * other symbol is part of the text.
 * <p>
 * Lives in phoenix-mass-import because that is the module which runs the mapper and pins the core-lib version with the fix.
 */
@ExtendWith(MockitoExtension.class)
class BankPartnerMapperTest {

    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private AccountingPeriodsRepository accountingPeriodsRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private PrefixRepository prefixRepository;
    @Mock
    private LatePaymentFineRepository latePaymentFineRepository;
    @Mock
    private DepositRepository depositRepository;
    @Mock
    private CustomerLiabilityRepository customerLiabilityRepository;
    @Mock
    private ContractBillingGroupRepository billingGroupRepository;
    @Mock
    private ActionRepository actionRepository;
    @Mock
    private ClaimedPenaltyRepository claimedPenaltyRepository;

    @InjectMocks
    private BankPartnerMapper mapper;

    @BeforeEach
    void stubPrefixes() {
        Prefix prefix = new Prefix();
        prefix.setName("TST");
        lenient().when(prefixRepository.findById(any())).thenReturn(Optional.of(prefix));
    }

    @Test
    void mapToBankPartnerRecord_keepsPlusInPaymentReference_whenSubTagsAreSeparatedByQuestionMark() {
        BankPartnerRecord record = map(
                ":61:2509250925C147,60NMSC//NONREF",
                ":86:835?206000000001 1000000001?21 13.06.2025 + лихви ел. ен?22ергия?30TESTBGSF?31BG00TEST00000000000001?32ТЕСТ ЕООД"
        );

        assertThat(record.getPaymentPurpose()).isEqualTo("6000000001 1000000001 13.06.2025 + лихви ел. ен ергия");
        assertThat(record.getAccountIdentification()).isEqualTo("BG00TEST00000000000001");
    }

    @Test
    void mapToBankPartnerRecord_keepsPlusFollowedByDigits_whenSubTagsAreSeparatedByQuestionMark() {
        BankPartnerRecord record = map(
                ":61:2601300130C290,39NMSC//NONREF",
                ":86:835?20Ф.1098+1099+1100+1310.19.01?21.26?30TESTBGSF?31BG00TEST00000000000002?32ТЕСТ ЕООД"
        );

        assertThat(record.getPaymentPurpose()).isEqualTo("Ф.1098+1099+1100+1310.19.01 .26");
        assertThat(record.getAccountIdentification()).isEqualTo("BG00TEST00000000000002");
    }

    @Test
    void mapToBankPartnerRecord_keepsPlusAtEndOfSubTag_whenSubTagsAreSeparatedByQuestionMark() {
        BankPartnerRecord record = map(
                ":61:2601300130C290,39NMSC//NONREF",
                ":86:835?20Ф.1098+1099+1100+1310+?211311.19.01.26?30TESTBGSF?31BG00TEST00000000000002?32ТЕСТ ЕООД"
        );

        assertThat(record.getPaymentPurpose()).isEqualTo("Ф.1098+1099+1100+1310+ 1311.19.01.26");
    }

    @Test
    void mapToBankPartnerRecord_removesPlusSeparatorsAtLineEnds_whenSubTagsAreOnSeparateLines() {
        BankPartnerRecord record = map(
                ":61:2503190319CN377,56NTRFNONREF",
                "TC15-TSC81",
                ":NS:190908+",
                ":86:020+15+",
                "20ЕЛ. ЕНЕРГИЯ ФТ,0400000001ФТ040+",
                "210000001, 6000000002,0000000000+",
                "2219032025 09:08+",
                "28+",
                "30+",
                "31BG00TEST00000000000003+",
                "32ТЕСТ ЕООД+",
                "33"
        );

        assertThat(record.getPaymentPurpose())
                .isEqualTo("ЕЛ. ЕНЕРГИЯ ФТ,0400000001ФТ040 0000001, 6000000002,0000000000 19032025 09:08");
        assertThat(record.getAccountIdentification()).isEqualTo("BG00TEST00000000000003");
    }

    @Test
    void mapToBankPartnerRecord_keepsPlusInPaymentReference_whenSubTagsAreOnSeparateLines() {
        BankPartnerRecord record = map(
                ":61:2503190319CN377,56NTRFNONREF",
                ":86:020+15+",
                "206000000001 1000000001+",
                "21 13.06.2025 + лихви ел. ен+",
                "22ергия+",
                "31BG00TEST00000000000001+",
                "32ТЕСТ ЕООД+",
                "33"
        );

        assertThat(record.getPaymentPurpose()).isEqualTo("6000000001 1000000001 13.06.2025 + лихви ел. ен ергия");
        assertThat(record.getAccountIdentification()).isEqualTo("BG00TEST00000000000001");
    }

    private BankPartnerRecord map(String... recordLines) {
        return mapper.mapToBankPartnerRecord(
                mapper.parseBankPartnerRecord(String.join("\n", recordLines)),
                null,
                1L,
                new ArrayList<>(),
                LocalDate.of(2025, 9, 25),
                new ArrayList<>()
        );
    }
}
