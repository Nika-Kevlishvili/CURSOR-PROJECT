package bg.energo.phoenix.service.salesportal.customer;

import bg.energo.phoenix.exception.ClientException;
import bg.energo.phoenix.model.enums.translation.Language;
import bg.energo.phoenix.model.response.customer.salesportal.SalesPortalCustomerResponse;
import bg.energo.phoenix.service.customer.selection.CustomerVersionSelectionService;
import bg.energo.phoenix.service.customer.selection.SelectedCustomerVersion;
import bg.energo.phoenix.service.salesportal.customer.lookup.CustomerLookupCandidate;
import bg.energo.phoenix.service.salesportal.customer.lookup.CustomerLookupCriteria;
import bg.energo.phoenix.service.salesportal.customer.lookup.CustomerLookupDispatcher;
import bg.energo.phoenix.service.salesportal.customer.lookup.LookupMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Covers the single-endpoint priority mechanism on
 * {@link SalesPortalCustomerLookupService#getSingleCustomer(String, String, String, String, String, String, String, String, LocalDate, Language)}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalesPortalCustomerLookupServiceTest {

    @Mock
    private CustomerLookupDispatcher dispatcher;
    @Mock
    private CustomerVersionSelectionService selectionService;
    @Mock
    private SalesPortalCustomerAssembler assembler;

    @InjectMocks
    private SalesPortalCustomerLookupService service;

    @Test
    void uicWinsByPriorityWhenMultipleKeysProvided() {
        SalesPortalCustomerResponse expected = new SalesPortalCustomerResponse();
        SelectedCustomerVersion selected = SelectedCustomerVersion.withoutDrivingContract(500L, 10L, 1L);
        when(dispatcher.supports(LookupMethod.UIC)).thenReturn(true);
        when(dispatcher.resolve(eq(LookupMethod.UIC), any())).thenReturn(List.of(CustomerLookupCandidate.of(500L)));
        when(selectionService.selectVersion(500L)).thenReturn(selected);
        when(assembler.assemble(selected, Language.BULGARIAN)).thenReturn(expected);

        SalesPortalCustomerResponse actual =
                service.getSingleCustomer("112233", "999", "C-1", "INV-1", null, null, null, null, null, Language.BULGARIAN);

        assertThat(actual).isSameAs(expected);
        ArgumentCaptor<CustomerLookupCriteria> criteria = ArgumentCaptor.forClass(CustomerLookupCriteria.class);
        verify(dispatcher).resolve(eq(LookupMethod.UIC), criteria.capture());
        assertThat(criteria.getValue().value()).isEqualTo("112233");
        verify(dispatcher, never()).resolve(eq(LookupMethod.CUSTOMER_NUMBER), any());
    }

    @Test
    void fallsThroughToCustomerNumberWhenUicBlank() {
        SelectedCustomerVersion selected = SelectedCustomerVersion.withoutDrivingContract(501L, 11L, 2L);
        when(dispatcher.supports(LookupMethod.CUSTOMER_NUMBER)).thenReturn(true);
        when(dispatcher.resolve(eq(LookupMethod.CUSTOMER_NUMBER), any())).thenReturn(List.of(CustomerLookupCandidate.of(501L)));
        when(selectionService.selectVersion(501L)).thenReturn(selected);
        when(assembler.assemble(any(), any())).thenReturn(new SalesPortalCustomerResponse());

        service.getSingleCustomer("   ", "999", null, null, null, null, null, null, null, Language.ENGLISH);

        ArgumentCaptor<CustomerLookupCriteria> criteria = ArgumentCaptor.forClass(CustomerLookupCriteria.class);
        verify(dispatcher).resolve(eq(LookupMethod.CUSTOMER_NUMBER), criteria.capture());
        assertThat(criteria.getValue().value()).isEqualTo("999");
    }

    @Test
    void throwsWhenNoKeyProvided() {
        assertThatThrownBy(() -> service.getSingleCustomer(null, "", "   ", null, null, null, null, null, null, Language.BULGARIAN))
                .isInstanceOf(ClientException.class);
        verifyNoInteractions(dispatcher, selectionService, assembler);
    }

    @Test
    void throwsNotYetAvailableWhenSelectedStrategyMissing() {
        when(dispatcher.supports(LookupMethod.CONTRACT_NUMBER)).thenReturn(false);

        assertThatThrownBy(() -> service.getSingleCustomer(null, null, "C-123", null, null, null, null, null, null, Language.BULGARIAN))
                .isInstanceOf(ClientException.class);

        verify(dispatcher, never()).resolve(any(), any());
    }

    @Test
    void nameWinsWhenUicAndOtherIdentifiersBlank() {
        SelectedCustomerVersion selected = SelectedCustomerVersion.withoutDrivingContract(502L, 12L, 3L);
        when(dispatcher.supports(LookupMethod.NAME_SEARCH)).thenReturn(true);
        when(dispatcher.resolve(eq(LookupMethod.NAME_SEARCH), any())).thenReturn(List.of(CustomerLookupCandidate.of(502L)));
        when(selectionService.selectVersion(502L)).thenReturn(selected);
        when(assembler.assemble(any(), any())).thenReturn(new SalesPortalCustomerResponse());

        service.getSingleCustomer(null, null, null, null, "Иван", "Иванов", "0888123456", null, null, Language.BULGARIAN);

        ArgumentCaptor<CustomerLookupCriteria> criteria = ArgumentCaptor.forClass(CustomerLookupCriteria.class);
        verify(dispatcher).resolve(eq(LookupMethod.NAME_SEARCH), criteria.capture());
        assertThat(criteria.getValue().nameBasedLookupDetails().name()).isEqualTo("Иван");
        assertThat(criteria.getValue().nameBasedLookupDetails().surname()).isEqualTo("Иванов");
        assertThat(criteria.getValue().nameBasedLookupDetails().phone()).isEqualTo("0888123456");
        assertThat(criteria.getValue().nameBasedLookupDetails().email()).isNull();
        assertThat(criteria.getValue().nameBasedLookupDetails().birthDate()).isNull();
    }

    @Test
    void nameLookupPassesAllDetailsToDispatcher() {
        LocalDate birthDate = LocalDate.of(1985, 6, 15);
        SelectedCustomerVersion selected = SelectedCustomerVersion.withoutDrivingContract(503L, 13L, 4L);
        when(dispatcher.supports(LookupMethod.NAME_SEARCH)).thenReturn(true);
        when(dispatcher.resolve(eq(LookupMethod.NAME_SEARCH), any())).thenReturn(List.of(CustomerLookupCandidate.of(503L)));
        when(selectionService.selectVersion(503L)).thenReturn(selected);
        when(assembler.assemble(any(), any())).thenReturn(new SalesPortalCustomerResponse());

        service.getSingleCustomer(null, null, null, null, "Мария", "Петрова", null, "maria@example.com", birthDate, Language.ENGLISH);

        ArgumentCaptor<CustomerLookupCriteria> criteria = ArgumentCaptor.forClass(CustomerLookupCriteria.class);
        verify(dispatcher).resolve(eq(LookupMethod.NAME_SEARCH), criteria.capture());
        assertThat(criteria.getValue().nameBasedLookupDetails().name()).isEqualTo("Мария");
        assertThat(criteria.getValue().nameBasedLookupDetails().surname()).isEqualTo("Петрова");
        assertThat(criteria.getValue().nameBasedLookupDetails().phone()).isNull();
        assertThat(criteria.getValue().nameBasedLookupDetails().email()).isEqualTo("maria@example.com");
        assertThat(criteria.getValue().nameBasedLookupDetails().birthDate()).isEqualTo(birthDate);
    }

    @Test
    void nameSearchNotYetAvailableThrowsClientException() {
        when(dispatcher.supports(LookupMethod.NAME_SEARCH)).thenReturn(false);

        assertThatThrownBy(() -> service.getSingleCustomer(null, null, null, null, "Иван", null, null, null, null, Language.BULGARIAN))
                .isInstanceOf(ClientException.class);

        verify(dispatcher, never()).resolve(any(), any());
    }
}
