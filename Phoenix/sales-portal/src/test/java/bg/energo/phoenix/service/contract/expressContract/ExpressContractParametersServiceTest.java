package bg.energo.phoenix.service.contract.expressContract;

import bg.energo.phoenix.model.entity.contract.InterestRate.InterestRate;
import bg.energo.phoenix.model.entity.contract.product.ProductContract;
import bg.energo.phoenix.model.entity.contract.product.ProductContractDetails;
import bg.energo.phoenix.model.entity.nomenclature.customer.Bank;
import bg.energo.phoenix.model.enums.contract.InterestRate.InterestRateStatus;
import bg.energo.phoenix.model.enums.nomenclature.NomenclatureItemStatus;
import bg.energo.phoenix.model.request.contract.express.ExpressContractBankingDetails;
import bg.energo.phoenix.model.request.contract.express.ExpressContractParameters;
import bg.energo.phoenix.repository.contract.product.ProductContractRepository;
import bg.energo.phoenix.repository.contract.product.ProductContractVersionTypeRepository;
import bg.energo.phoenix.repository.contract.service.ServiceContractsRepository;
import bg.energo.phoenix.repository.interestRate.InterestRateRepository;
import bg.energo.phoenix.repository.nomenclature.contract.CampaignRepository;
import bg.energo.phoenix.repository.nomenclature.contract.ContractVersionTypesRepository;
import bg.energo.phoenix.repository.nomenclature.customer.BankRepository;
import bg.energo.phoenix.repository.product.product.ProductDetailsRepository;
import bg.energo.phoenix.repository.product.product.ProductRepository;
import bg.energo.phoenix.repository.product.service.ServiceAdditionalParamsRepository;
import bg.energo.phoenix.repository.product.service.ServiceDetailsRepository;
import bg.energo.phoenix.repository.product.service.ServiceRepository;
import bg.energo.phoenix.security.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpressContractParametersServiceTest {

    private static final Long BANK_ID = 7001L;
    private static final Long INTEREST_RATE_ID = 9001L;

    @Mock private ServiceDetailsRepository serviceDetailsRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private BankRepository bankRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductDetailsRepository productDetailsRepository;
    @Mock private ProductContractRepository productContractRepository;
    @Mock private InterestRateRepository interestRateRepository;
    @Mock private ProductContractVersionTypeRepository contractVersionTypeRepository;
    @Mock private ContractVersionTypesRepository versionTypesRepository;
    @Mock private ServiceContractsRepository serviceContractsRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private ServiceAdditionalParamsRepository serviceAdditionalParamsRepository;
    @Mock private PermissionService permissionService;

    @InjectMocks private ExpressContractParametersService service;

    @BeforeEach
    void setUp() {
        InterestRate interestRate = new InterestRate();
        interestRate.setId(INTEREST_RATE_ID);
        when(interestRateRepository.findByIsDefaultAndStatus(true, InterestRateStatus.ACTIVE))
                .thenReturn(Optional.of(interestRate));
        when(campaignRepository.findByIdAndStatusIn(any(), anyList())).thenReturn(Optional.empty());
        when(productRepository.findByIdAndProductStatusIn(any(), anyList())).thenReturn(Optional.empty());
        when(productDetailsRepository.findByProductIdAndVersion(any(), any())).thenReturn(Optional.empty());
    }

    /** Banking details are only needed for direct debit, so the minimal Sales Portal payload omits them. */
    @Test
    void createProductContractDetail_acceptsOmittedBankingDetails() {
        ExpressContractParameters parameters = parameters();
        parameters.setBankingDetails(null);
        List<String> messages = new ArrayList<>();

        ProductContractDetails details = service.createProductContractDetail(parameters, new ProductContract(), 1L, messages);

        assertThat(details.getBankId()).isNull();
        assertThat(details.getIban()).isNull();
        assertThat(details.getDirectDebit()).isNull();
        assertThat(messages).noneMatch(message -> message.contains("bankingDetails"));
    }

    @Test
    void createProductContractDetail_appliesProvidedBankingDetails() {
        ExpressContractParameters parameters = parameters();
        parameters.setBankingDetails(new ExpressContractBankingDetails(true, BANK_ID, "BG80BNBG96611020345678"));
        Bank bank = new Bank();
        bank.setId(BANK_ID);
        when(bankRepository.findByIdAndStatus(eq(BANK_ID), anyList())).thenReturn(Optional.of(bank));
        List<String> messages = new ArrayList<>();

        ProductContractDetails details = service.createProductContractDetail(parameters, new ProductContract(), 1L, messages);

        assertThat(details.getBankId()).isEqualTo(BANK_ID);
        assertThat(details.getIban()).isEqualTo("BG80BNBG96611020345678");
        assertThat(details.getDirectDebit()).isTrue();
    }

    @Test
    void createProductContractDetail_reportsUnknownBank() {
        ExpressContractParameters parameters = parameters();
        parameters.setBankingDetails(new ExpressContractBankingDetails(true, BANK_ID, "BG80BNBG96611020345678"));
        when(bankRepository.findByIdAndStatus(eq(BANK_ID), anyList())).thenReturn(Optional.empty());
        List<String> messages = new ArrayList<>();

        service.createProductContractDetail(parameters, new ProductContract(), 1L, messages);

        assertThat(messages).contains("expressContractParameters.bankingDetails.bankId-Bank not found!;");
    }

    private static ExpressContractParameters parameters() {
        ExpressContractParameters parameters = new ExpressContractParameters();
        parameters.setProductId(1001L);
        parameters.setProductVersionId(1L);
        return parameters;
    }
}
