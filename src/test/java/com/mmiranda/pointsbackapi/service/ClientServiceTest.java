package com.mmiranda.pointsbackapi.service;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import com.mmiranda.pointsbackapi.dto.AdjustPointsRequestDto;
import com.mmiranda.pointsbackapi.dto.ImportClientRequestDto;
import com.mmiranda.pointsbackapi.dto.LedgerEntryDto;
import com.mmiranda.pointsbackapi.dto.PageDto;
import com.mmiranda.pointsbackapi.dto.RedeemPreviewDto;
import com.mmiranda.pointsbackapi.model.LedgerEntry;
import com.mmiranda.pointsbackapi.model.PointsEntryType;
import com.mmiranda.pointsbackapi.model.RewardMode;
import com.mmiranda.pointsbackapi.repository.LedgerRepository;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import com.mmiranda.pointsbackapi.exception.ConflictException;
import com.mmiranda.pointsbackapi.exception.ForbiddenException;
import com.mmiranda.pointsbackapi.model.EstablishmentGroup;
import com.mmiranda.pointsbackapi.model.Person;
import com.mmiranda.pointsbackapi.repository.PersonRepository;
import java.util.Optional;
import com.mmiranda.pointsbackapi.exception.InsufficientPointsException;
import com.mmiranda.pointsbackapi.exception.ResourceNotFoundException;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.repository.EstablishmentRepository;
import com.mmiranda.pointsbackapi.security.TestAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mmiranda.pointsbackapi.dto.ClientDto;
import com.mmiranda.pointsbackapi.model.Client;
import com.mmiranda.pointsbackapi.repository.ClientRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class ClientServiceTest {

    @Mock
    private ClientRepository repository;

    @Mock
    private EstablishmentRepository establishmentRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private LedgerRepository ledgerRepository;

    @InjectMocks
    private ClientService service;

    private Client clientTest;
    private Establishment establishmentTest;

    @BeforeEach
    void setUp() {
        establishmentTest = buildEstablishment();
        clientTest = buildClient();
        TestAuth.asPlatformAdmin();
        // The real PointsService on the mocked repositories, so balance changes are exercised for real
        ReflectionTestUtils.setField(service, "pointsService", new PointsService(repository, ledgerRepository));
        assertNotNull(clientTest);
    }

    @AfterEach
    void tearDown() {
        TestAuth.clear();
    }

    @Test
    void testGetClientById() {
        // Arrange
        Long clientId = 1L;

        // Act
        when(repository.findById(clientId))
        .thenReturn(java.util.Optional.
            of(clientTest));

        ClientDto result = service.getClientById(clientId);

        // Assert
        assertNotNull(result);
        assertEquals("Test Client", result.name());
        assertEquals("test@example.com", result.email());
        assertEquals(1L, result.establishmentId());
    }

    @Test
    void testFailGetClientById() {
        // Arrange
        Long clientId = 1L;
        // Act
        when(repository.findById(clientId))
        .thenReturn(java.util.Optional.empty());

        // Assert
        assertThrows(ResourceNotFoundException.class, () -> service.getClientById(clientId));
    }

    @Test
    void testGetClientByIdForbiddenForOtherEstablishment() {
        // Arrange
        TestAuth.clear();
        TestAuth.asEstablishmentStaff(2L);
        Long clientId = 1L;

        when(repository.findByIdAndEstablishmentId(clientId, 2L))
                .thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThrows(ForbiddenException.class, () -> service.getClientById(clientId));
    }

    @Test
    void testGetClientByIdScopedToOwnEstablishment() {
        // Arrange
        TestAuth.clear();
        TestAuth.asEstablishmentStaff(1L);
        Long clientId = 1L;

        when(repository.findByIdAndEstablishmentId(clientId, 1L))
                .thenReturn(java.util.Optional.of(clientTest));

        // Act
        ClientDto result = service.getClientById(clientId);

        // Assert
        assertNotNull(result);
        assertEquals("Test Client", result.name());
    }

    @Test
    void testCreateClient() {
        // Arrange
        ClientDto clientDto = ClientDto.toDto(clientTest);
        when(establishmentRepository.findById(1L)).thenReturn(java.util.Optional.of(establishmentTest));
        when(personRepository.save(any(Person.class))).thenAnswer(i -> i.getArgument(0));
        when(repository.save(any(Client.class))).thenReturn(clientTest);

        // Act
        ClientDto result = service.createClient(clientDto);

        // Assert
        assertNotNull(result);
        assertEquals("Test Client", result.name());
        assertEquals("test@example.com", result.email());
    }

    @Test
    void testCreateClientRequiresEstablishmentForAdmin() {
        // Arrange
        ClientDto clientDto = new ClientDto(1L, "No Establishment", "x@example.com", null, null, null, null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> service.createClient(clientDto));
    }

    @Test
    void testCreateClientForbiddenForDifferentEstablishment() {
        // Arrange
        TestAuth.clear();
        TestAuth.asEstablishmentStaff(2L);
        ClientDto clientDto = new ClientDto(null, "New Client", "new@example.com", null, null, null, 1L);

        // Act & Assert
        assertThrows(ForbiddenException.class, () -> service.createClient(clientDto));
    }

    @Test
    void testCreateClientDefaultsToCallerEstablishment() {
        // Arrange
        TestAuth.clear();
        TestAuth.asEstablishmentStaff(1L);
        ClientDto clientDto = new ClientDto(null, "New Client", "new@example.com", null, "529.982.247-25", null, null);

        when(establishmentRepository.findById(1L)).thenReturn(java.util.Optional.of(establishmentTest));
        when(personRepository.save(any(Person.class))).thenAnswer(i -> i.getArgument(0));
        when(repository.save(any(Client.class))).thenReturn(clientTest);

        // Act
        ClientDto result = service.createClient(clientDto);

        // Assert
        assertNotNull(result);
        verify(repository, times(1)).save(any(Client.class));
    }

    @Test
    void testListAllClients() {
        // Arrange
        Client client2 = Client.builder()
                .id(2L)
                .name("Test Client 2")
                .email("test2@example.com")
                .phone("0987654321")
                .person(com.mmiranda.pointsbackapi.model.Person.builder().cpf("98765432100").build())
                .points(200)
                .establishment(establishmentTest)
                .build();

        when(repository.findAll()).thenReturn(Arrays.asList(clientTest, client2));

        // Act
        List<ClientDto> result = service.listAllClients();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test Client", result.get(0).name());
        assertEquals("Test Client 2", result.get(1).name());
        verify(repository, times(1)).findAll();
    }

    @Test
    void testListAllClientsScopedForNonAdmin() {
        // Arrange
        TestAuth.clear();
        TestAuth.asEstablishmentOwner(1L);

        when(repository.findAllByEstablishmentId(1L)).thenReturn(List.of(clientTest));

        // Act
        List<ClientDto> result = service.listAllClients();

        // Assert
        assertEquals(1, result.size());
        verify(repository, times(1)).findAllByEstablishmentId(1L);
        verify(repository, times(0)).findAll();
    }

    @Test
    void testAdjustPointsChangesTheBalanceAndWritesALedgerEntry() {
        Long clientId = 1L;
        clientTest.setPoints(100);
        when(repository.findByIdForUpdate(clientId)).thenReturn(Optional.of(clientTest));

        ClientDto result = service.adjustPoints(clientId, new AdjustPointsRequestDto(50, "Welcome bonus"));

        assertEquals(150, result.points());
        ArgumentCaptor<LedgerEntry> entry = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerRepository).save(entry.capture());
        assertEquals(PointsEntryType.ADJUST, entry.getValue().getType());
        assertEquals(50, entry.getValue().getPoints());
        assertEquals(150, entry.getValue().getBalanceAfter());
        assertEquals("Welcome bonus", entry.getValue().getReason());
    }

    @Test
    void testAdjustPointsClientNotFound() {
        when(repository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.adjustPoints(999L, new AdjustPointsRequestDto(50, "x")));

        verify(ledgerRepository, times(0)).save(any());
    }

    @Test
    void testAdjustPointsForbiddenForOtherEstablishment() {
        TestAuth.clear();
        TestAuth.asEstablishmentOwner(2L);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(clientTest));

        assertThrows(ForbiddenException.class,
                () -> service.adjustPoints(1L, new AdjustPointsRequestDto(10, "x")));
    }

    @Test
    void testAdjustPointsTreatsMissingBalanceAsZero() {
        clientTest.setPoints(null);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(clientTest));

        assertEquals(50, service.adjustPoints(1L, new AdjustPointsRequestDto(50, "x")).points());
    }

    @Test
    void testAdjustPointsRejectsNegativeBalanceAndZero() {
        clientTest.setPoints(10);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(clientTest));

        assertThrows(InsufficientPointsException.class,
                () -> service.adjustPoints(1L, new AdjustPointsRequestDto(-11, "x")));
        assertThrows(IllegalArgumentException.class,
                () -> service.adjustPoints(1L, new AdjustPointsRequestDto(0, "x")));

        assertEquals(10, clientTest.getPoints());
        verify(ledgerRepository, times(0)).save(any());
    }

    @Test
    void testAdjustPointsAllowsRemovingTheExactBalance() {
        clientTest.setPoints(10);
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(clientTest));

        assertEquals(0, service.adjustPoints(1L, new AdjustPointsRequestDto(-10, "Expired")).points());
    }

    @Test
    void testStatementReturnsTheLedgerPageForAVisibleClient() {
        clientTest.setPoints(5);
        LedgerEntry entry = LedgerEntry.builder().id(9L).client(clientTest).type(PointsEntryType.CREDIT)
                .points(5).balanceAfter(5).createdBy(2L).build();
        when(repository.findById(1L)).thenReturn(Optional.of(clientTest));
        when(ledgerRepository.findByClientIdOrderByIdDesc(1L, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(entry), PageRequest.of(0, 20), 1));

        PageDto<LedgerEntryDto> page = service.statement(1L, 0, 20);

        assertEquals(1, page.totalElements());
        assertEquals(PointsEntryType.CREDIT, page.content().get(0).type());
        assertEquals(5, page.content().get(0).balanceAfter());
    }

    @Test
    void testStatementClampsPageAndSize() {
        when(repository.findById(1L)).thenReturn(Optional.of(clientTest));
        when(ledgerRepository.findByClientIdOrderByIdDesc(1L, PageRequest.of(0, 100)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        service.statement(1L, -3, 100000);

        verify(ledgerRepository).findByClientIdOrderByIdDesc(1L, PageRequest.of(0, 100));
    }

    @Test
    void testStatementIsForbiddenForAnotherEstablishment() {
        TestAuth.clear();
        TestAuth.asEstablishmentStaff(2L);
        when(repository.findByIdAndEstablishmentId(1L, 2L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> service.statement(1L, 0, 20));
    }

    @Test
    void testRedeemPreviewCapsPointsByBalanceAndByTheMaximumShare() {
        establishmentTest.setRewardMode(RewardMode.DISCOUNT);
        establishmentTest.setPointsToCurrencyRate(new BigDecimal("0.1000"));
        establishmentTest.setMaxDiscountPercent(50);
        clientTest.setEstablishment(establishmentTest);
        when(repository.findById(1L)).thenReturn(Optional.of(clientTest));

        // Balance is the limit: 300 points, but the cap would allow 500
        clientTest.setPoints(300);
        RedeemPreviewDto small = service.redeemPreview(1L, new BigDecimal("100.00"));
        assertEquals(300, small.maxPoints());
        assertEquals(new BigDecimal("30.00"), small.maxDiscount());
        assertEquals(new BigDecimal("30.00"), small.balanceValue());

        // The cap is the limit: 50% of 100.00 is 50.00, i.e. 500 points
        clientTest.setPoints(1000);
        RedeemPreviewDto capped = service.redeemPreview(1L, new BigDecimal("100.00"));
        assertEquals(500, capped.maxPoints());
        assertEquals(new BigDecimal("50.00"), capped.maxDiscount());
        assertEquals(RewardMode.DISCOUNT, capped.rewardMode());
    }

    @Test
    void testRedeemPreviewOffersNothingInCatalogMode() {
        establishmentTest.setRewardMode(RewardMode.CATALOG);
        clientTest.setEstablishment(establishmentTest);
        clientTest.setPoints(null);
        when(repository.findById(1L)).thenReturn(Optional.of(clientTest));

        RedeemPreviewDto preview = service.redeemPreview(1L, new BigDecimal("100.00"));

        assertEquals(0, preview.maxPoints());
        assertEquals(0, preview.balance());
    }

    @Test
    void testCreateAlwaysStartsWithAZeroBalance() {
        ClientDto dto = new ClientDto(null, "Rich", null, null, "529.982.247-25", 5000, 1L);
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishmentTest));
        when(personRepository.findByCpf("52998224725")).thenReturn(Optional.of(person(5L, "52998224725")));
        when(repository.existsByPersonIdAndEstablishmentId(5L, 1L)).thenReturn(false);
        when(repository.save(any(Client.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals(0, service.createClient(dto).points());
    }

    @Test
    void testUpdateClientAllFields() {
        // Arrange
        Long clientId = 1L;
        ClientDto updateDto = new ClientDto(
                clientId,
                "Updated Client",
                "updated@example.com",
                "9999999999",
                "529.982.247-25",
                500,
                null
        );

        when(repository.findByIdForUpdate(clientId)).thenReturn(java.util.Optional.of(clientTest));
        when(personRepository.findByCpf("52998224725")).thenReturn(Optional.of(person(7L, "52998224725")));
        when(repository.existsByPersonIdAndEstablishmentId(7L, 1L)).thenReturn(false);
        when(repository.save(any(Client.class))).thenReturn(clientTest);

        // Act
        ClientDto result = service.updateClient(clientId, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals(7L, clientTest.getPerson().getId());
        verify(repository, times(1)).findByIdForUpdate(clientId);
        verify(repository, times(1)).save(any(Client.class));
    }

    @Test
    void testUpdateClientPartialFields() {
        // Arrange
        Long clientId = 1L;
        ClientDto updateDto = new ClientDto(
                clientId,
                "Updated Client",
                null,
                null,
                null,
                null,
                null
        );

        when(repository.findByIdForUpdate(clientId)).thenReturn(java.util.Optional.of(clientTest));
        when(repository.save(any(Client.class))).thenReturn(clientTest);

        // Act
        ClientDto result = service.updateClient(clientId, updateDto);

        // Assert
        assertNotNull(result);
        verify(repository, times(1)).findByIdForUpdate(clientId);
        verify(repository, times(1)).save(any(Client.class));
    }

    @Test
    void testUpdateClientNotFound() {
        // Arrange
        Long clientId = 999L;
        ClientDto updateDto = new ClientDto(
                clientId,
                "Updated Client",
                "updated@example.com",
                "9999999999",
                "999.999.999-99",
                500,
                null
        );

        when(repository.findByIdForUpdate(clientId)).thenReturn(java.util.Optional.empty());

        // Act
        assertThrows(ResourceNotFoundException.class, () -> service.updateClient(clientId, updateDto));

        // Assert
        verify(repository, times(1)).findByIdForUpdate(clientId);
        verify(repository, times(0)).save(any());
    }

    // ------------------------------------------------------------------ person identity (mvp-02)

    @Test
    void testCreateClientReusesExistingPersonForTheSameCpf() {
        ClientDto dto = new ClientDto(null, "Maria", "m@example.com", "1199998888", "529.982.247-25", null, 1L);
        Person existing = person(5L, "52998224725");
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishmentTest));
        when(personRepository.findByCpf("52998224725")).thenReturn(Optional.of(existing));
        when(repository.existsByPersonIdAndEstablishmentId(5L, 1L)).thenReturn(false);
        when(repository.save(any(Client.class))).thenAnswer(i -> i.getArgument(0));

        ClientDto result = service.createClient(dto);

        assertEquals("529.982.247-25", result.cpf());
        assertEquals("Maria", result.name());
        assertEquals(0, result.points());
        verify(personRepository, times(0)).save(any());
    }

    @Test
    void testCreateClientRejectsAnAccountThatAlreadyExistsInTheEstablishment() {
        ClientDto dto = new ClientDto(null, "Maria", null, null, "52998224725", null, 1L);
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishmentTest));
        when(personRepository.findByCpf("52998224725")).thenReturn(Optional.of(person(5L, "52998224725")));
        when(repository.existsByPersonIdAndEstablishmentId(5L, 1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.createClient(dto));

        verify(repository, times(0)).save(any());
    }

    @Test
    void testSearchClientsNormalizesCpfAndPhoneAndScopesNonAdminToOwnEstablishment() {
        TestAuth.clear();
        TestAuth.asEstablishmentStaff(1L);
        when(repository.search(1L, "52998224725", "1199998888")).thenReturn(List.of(clientTest));

        List<ClientDto> result = service.searchClients("529.982.247-25", "(11) 9999-8888", 99L);

        assertEquals(1, result.size());
        verify(repository).search(1L, "52998224725", "1199998888");
    }

    @Test
    void testSearchClientsLetsAdminChooseTheEstablishmentOrSearchAll() {
        when(repository.search(null, "52998224725", null)).thenReturn(List.of());
        when(repository.search(2L, null, "1199998888")).thenReturn(List.of());

        assertEquals(0, service.searchClients("52998224725", "  ", null).size());
        assertEquals(0, service.searchClients(null, "1199998888", 2L).size());
    }

    @Test
    void testSearchClientsRequiresAtLeastOneFilter() {
        assertThrows(IllegalArgumentException.class, () -> service.searchClients(null, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.searchClients(" ", "abc", null));
    }

    @Test
    void testImportFromGroupCopiesContactDataWithZeroPoints() {
        TestAuth.clear();
        TestAuth.asEstablishmentStaff(1L);
        joinSharingGroup(establishmentTest);
        Person person = person(5L, "52998224725");
        Client source = Client.builder().id(50L).person(person).name("Maria Source").email("src@example.com")
                .phone("1188887777").points(300).establishment(establishmentInGroup(2L)).build();
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishmentTest));
        when(personRepository.findByCpf("52998224725")).thenReturn(Optional.of(person));
        when(repository.findSharedAccounts(5L, 9L, 1L)).thenReturn(List.of(source));
        when(repository.existsByPersonIdAndEstablishmentId(5L, 1L)).thenReturn(false);
        when(repository.save(any(Client.class))).thenAnswer(i -> i.getArgument(0));

        ClientDto result = service.importFromGroup(new ImportClientRequestDto("529.982.247-25", null));

        assertEquals("Maria Source", result.name());
        assertEquals("1188887777", result.phone());
        assertEquals(0, result.points());
        assertEquals(1L, result.establishmentId());
    }

    @Test
    void testImportFromGroupRequiresTheCallersEstablishmentToShare() {
        TestAuth.clear();
        TestAuth.asEstablishmentStaff(1L);
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishmentTest));

        // not in any group
        assertThrows(ForbiddenException.class,
                () -> service.importFromGroup(new ImportClientRequestDto("52998224725", null)));

        // in a group but opted out of sharing
        establishmentTest.setGroup(EstablishmentGroup.builder().id(9L).name("Chain").build());
        establishmentTest.setShareClients(false);
        assertThrows(ForbiddenException.class,
                () -> service.importFromGroup(new ImportClientRequestDto("52998224725", null)));
    }

    @Test
    void testImportFromGroupIsNotFoundWhenNobodyInTheGroupSharesThatPerson() {
        TestAuth.clear();
        TestAuth.asEstablishmentStaff(1L);
        joinSharingGroup(establishmentTest);
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishmentTest));

        // unknown person
        when(personRepository.findByCpf("52998224725")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.importFromGroup(new ImportClientRequestDto("52998224725", null)));

        // known person, but no sharing account in the group: same answer
        when(personRepository.findByCpf("52998224725")).thenReturn(Optional.of(person(5L, "52998224725")));
        when(repository.findSharedAccounts(5L, 9L, 1L)).thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class,
                () -> service.importFromGroup(new ImportClientRequestDto("52998224725", null)));
    }

    @Test
    void testImportFromGroupRejectsAnAccountThatAlreadyExistsLocally() {
        TestAuth.clear();
        TestAuth.asEstablishmentStaff(1L);
        joinSharingGroup(establishmentTest);
        Person person = person(5L, "52998224725");
        Client source = Client.builder().id(50L).person(person).name("Src").establishment(establishmentInGroup(2L)).build();
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishmentTest));
        when(personRepository.findByCpf("52998224725")).thenReturn(Optional.of(person));
        when(repository.findSharedAccounts(5L, 9L, 1L)).thenReturn(List.of(source));
        when(repository.existsByPersonIdAndEstablishmentId(5L, 1L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.importFromGroup(new ImportClientRequestDto("52998224725", null)));
    }

    @Test
    void testUpdateClientKeepsThePersonWhenTheCpfIsUnchanged() {
        clientTest.setPerson(person(3L, "52998224725"));
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(clientTest));
        when(repository.save(any(Client.class))).thenReturn(clientTest);

        service.updateClient(1L, new ClientDto(null, null, null, null, "529.982.247-25", null, null));

        verify(personRepository, times(0)).findByCpf(any());
        assertEquals(3L, clientTest.getPerson().getId());
    }

    @Test
    void testUpdateClientRejectsMovingToACpfAlreadyRegisteredInTheEstablishment() {
        clientTest.setPerson(person(3L, "52998224725"));
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(clientTest));
        when(personRepository.findByCpf("11144477735")).thenReturn(Optional.of(person(8L, "11144477735")));
        when(repository.existsByPersonIdAndEstablishmentId(8L, 1L)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.updateClient(1L, new ClientDto(null, null, null, null, "111.444.777-35", null, null)));

        assertEquals(3L, clientTest.getPerson().getId());
    }

    private static Person person(Long id, String cpf) {
        return Person.builder().id(id).cpf(cpf).build();
    }

    private static Establishment establishmentInGroup(Long id) {
        Establishment establishment = new Establishment();
        establishment.setId(id);
        joinSharingGroup(establishment);
        return establishment;
    }

    private static void joinSharingGroup(Establishment establishment) {
        establishment.setGroup(EstablishmentGroup.builder().id(9L).name("Chain").build());
        establishment.setShareClients(true);
    }

    public Client buildClient() {
        return Client.builder()
                .id(1L)
                .name("Test Client")
                .email("test@example.com")
                .phone("1234567890")
                .person(com.mmiranda.pointsbackapi.model.Person.builder().cpf("12345678900").build())
                .points(100)
                .establishment(establishmentTest)
                .build();
    }

    public Establishment buildEstablishment() {
        Establishment establishment = new Establishment();
        establishment.setId(1L);
        establishment.setName("Test Establishment");
        establishment.setValuePerPoint(10);
        return establishment;
    }
}
