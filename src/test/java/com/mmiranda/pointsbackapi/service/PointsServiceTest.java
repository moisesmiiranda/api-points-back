package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.exception.InsufficientPointsException;
import com.mmiranda.pointsbackapi.model.Client;
import com.mmiranda.pointsbackapi.model.LedgerEntry;
import com.mmiranda.pointsbackapi.model.PointsEntryType;
import com.mmiranda.pointsbackapi.repository.ClientRepository;
import com.mmiranda.pointsbackapi.repository.LedgerRepository;
import com.mmiranda.pointsbackapi.security.TestAuth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointsServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private LedgerRepository ledgerRepository;

    @InjectMocks
    private PointsService service;

    @BeforeEach
    void setUp() {
        TestAuth.asEstablishmentOwner(1L); // user id 2
    }

    @AfterEach
    void tearDown() {
        TestAuth.clear();
    }

    @Test
    void appliesTheChangeAndRecordsWhoDidItAndTheResultingBalance() {
        Client client = Client.builder().id(1L).points(40).build();
        when(ledgerRepository.save(any(LedgerEntry.class))).thenAnswer(i -> i.getArgument(0));

        LedgerEntry entry = service.apply(client, PointsEntryType.CREDIT, 10, null, null, "why");

        assertEquals(50, client.getPoints());
        assertEquals(10, entry.getPoints());
        assertEquals(50, entry.getBalanceAfter());
        assertEquals(2L, entry.getCreatedBy());
        assertEquals("why", entry.getReason());
        verify(clientRepository).save(client);
    }

    @Test
    void refusesToLeaveTheBalanceNegativeWithoutChangingAnything() {
        Client client = Client.builder().id(1L).points(4).build();

        assertThrows(InsufficientPointsException.class,
                () -> service.apply(client, PointsEntryType.REDEEM, -5, null, null, null));

        assertEquals(4, client.getPoints());
        verify(clientRepository, never()).save(any());
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void allowsSpendingTheExactBalanceAndRejectsAZeroChange() {
        Client client = Client.builder().id(1L).points(4).build();
        when(ledgerRepository.save(any(LedgerEntry.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals(0, service.apply(client, PointsEntryType.REDEEM, -4, null, null, null).getBalanceAfter());
        assertThrows(IllegalArgumentException.class,
                () -> service.apply(client, PointsEntryType.ADJUST, 0, null, null, null));
    }

    @Test
    void aMissingBalanceCountsAsZero() {
        Client client = Client.builder().id(1L).build();
        client.setPoints(null);
        when(ledgerRepository.save(any(LedgerEntry.class))).thenAnswer(i -> i.getArgument(0));

        service.apply(client, PointsEntryType.ADJUST, 7, null, null, "x");

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerRepository).save(captor.capture());
        assertEquals(7, captor.getValue().getBalanceAfter());
    }
}
