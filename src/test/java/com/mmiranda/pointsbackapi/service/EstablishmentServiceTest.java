package com.mmiranda.pointsbackapi.service;

import com.mmiranda.pointsbackapi.dto.EstablishmentDto;
import com.mmiranda.pointsbackapi.model.Establishment;
import com.mmiranda.pointsbackapi.repository.EstablishmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class EstablishmentServiceTest {

    @Mock
    private EstablishmentRepository establishmentRepository;

    @InjectMocks
    private EstablishmentService establishmentService;

    private Establishment establishmentTest;

    @BeforeEach
    void setUp() {
        establishmentTest = buildEstablishment();
        assertNotNull(establishmentTest);
    }

    @Test
    void testGetEstablishmentById() {
        // Arrange
        Long establishmentId = 1L;

        when(establishmentRepository.findById(establishmentId))
                .thenReturn(Optional.of(establishmentTest));

        // Act
        EstablishmentDto result = establishmentService.getEstablishmentById(establishmentId);

        // Assert
        assertNotNull(result);
        assertEquals("Test Establishment", result.name());
        assertEquals(10, result.valuePerPoint());
        verify(establishmentRepository, times(1)).findById(establishmentId);
    }

    @Test
    void testGetEstablishmentByIdNotFound() {
        // Arrange
        Long establishmentId = 999L;

        when(establishmentRepository.findById(establishmentId))
                .thenReturn(Optional.empty());

        // Act
        EstablishmentDto result = establishmentService.getEstablishmentById(establishmentId);

        // Assert
        assertNull(result);
        verify(establishmentRepository, times(1)).findById(establishmentId);
    }

    @Test
    void testCreateEstablishment() {
        // Arrange
        EstablishmentDto establishmentDto = EstablishmentDto.toDto(establishmentTest);

        when(establishmentRepository.save(any(Establishment.class)))
                .thenReturn(establishmentTest);

        // Act
        Establishment result = establishmentService.createEstablishment(establishmentDto);

        // Assert
        assertNotNull(result);
        assertEquals("Test Establishment", result.getName());
        assertEquals(10, result.getValuePerPoint());
        verify(establishmentRepository, times(1)).save(any(Establishment.class));
    }

    @Test
    void testListAllEstablishments() {
        // Arrange
        Establishment establishment2 = buildEstablishment();
        establishment2.setId(2L);
        establishment2.setName("Test Establishment 2");

        when(establishmentRepository.findAll())
                .thenReturn(Arrays.asList(establishmentTest, establishment2));

        // Act
        List<Establishment> result = establishmentService.listAllEstablishments();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test Establishment", result.get(0).getName());
        assertEquals("Test Establishment 2", result.get(1).getName());
        verify(establishmentRepository, times(1)).findAll();
    }

    @Test
    void testUpdateEstablishmentById_Success() {
        // Arrange
        Long establishmentId = 1L;
        EstablishmentDto updateDto = new EstablishmentDto(
            "Updated Establishment",
            "updated@example.com",
            "9999999999",
            "99.999.999/9999-99",
            15
        );

        Establishment updatedEstablishment = new Establishment();
        updatedEstablishment.setId(1L);
        updatedEstablishment.setName("Updated Establishment");
        updatedEstablishment.setEmail("updated@example.com");
        updatedEstablishment.setPhone("9999999999");
        updatedEstablishment.setCnpj("99.999.999/9999-99");
        updatedEstablishment.setValuePerPoint(15);

        when(establishmentRepository.findById(establishmentId))
                .thenReturn(Optional.of(establishmentTest));
        when(establishmentRepository.save(any(Establishment.class)))
                .thenReturn(updatedEstablishment);

        // Act
        EstablishmentDto result = establishmentService.updateEstablishmentById(establishmentId, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Establishment", result.name());
        assertEquals("updated@example.com", result.email());
        assertEquals(15, result.valuePerPoint());
        verify(establishmentRepository, times(1)).findById(establishmentId);
        verify(establishmentRepository, times(1)).save(any(Establishment.class));
    }

    @Test
    void testUpdateEstablishmentById_NotFound() {
        // Arrange
        Long establishmentId = 999L;
        EstablishmentDto updateDto = new EstablishmentDto(
            "Updated Establishment",
            "updated@example.com",
            "9999999999",
            "99.999.999/9999-99",
            15
        );

        when(establishmentRepository.findById(establishmentId))
                .thenReturn(Optional.empty());

        // Act
        EstablishmentDto result = establishmentService.updateEstablishmentById(establishmentId, updateDto);

        // Assert
        assertNull(result);
        verify(establishmentRepository, times(1)).findById(establishmentId);
        verify(establishmentRepository, never()).save(any(Establishment.class));
    }

    @Test
    void testUpdateEstablishmentById_UpdateOnlyName() {
        // Arrange
        Long establishmentId = 1L;
        EstablishmentDto updateDto = new EstablishmentDto(
            "Updated Name",
            null,
            null,
            null,
            null
        );

        Establishment updatedEstablishment = new Establishment();
        updatedEstablishment.setId(1L);
        updatedEstablishment.setName("Updated Name");
        updatedEstablishment.setEmail(establishmentTest.getEmail());
        updatedEstablishment.setPhone(establishmentTest.getPhone());
        updatedEstablishment.setCnpj(establishmentTest.getCnpj());
        updatedEstablishment.setValuePerPoint(establishmentTest.getValuePerPoint());

        when(establishmentRepository.findById(establishmentId))
                .thenReturn(Optional.of(establishmentTest));
        when(establishmentRepository.save(any(Establishment.class)))
                .thenReturn(updatedEstablishment);

        // Act
        EstablishmentDto result = establishmentService.updateEstablishmentById(establishmentId, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Name", result.name());
        verify(establishmentRepository, times(1)).findById(establishmentId);
        verify(establishmentRepository, times(1)).save(any(Establishment.class));
    }

    @Test
    void testUpdateEstablishmentById_UpdateOnlyValuePerPoint() {
        // Arrange
        Long establishmentId = 1L;
        EstablishmentDto updateDto = new EstablishmentDto(
            null,
            null,
            null,
            null,
            20
        );

        Establishment updatedEstablishment = new Establishment();
        updatedEstablishment.setId(1L);
        updatedEstablishment.setName(establishmentTest.getName());
        updatedEstablishment.setEmail(establishmentTest.getEmail());
        updatedEstablishment.setPhone(establishmentTest.getPhone());
        updatedEstablishment.setCnpj(establishmentTest.getCnpj());
        updatedEstablishment.setValuePerPoint(20);

        when(establishmentRepository.findById(establishmentId))
                .thenReturn(Optional.of(establishmentTest));
        when(establishmentRepository.save(any(Establishment.class)))
                .thenReturn(updatedEstablishment);

        // Act
        EstablishmentDto result = establishmentService.updateEstablishmentById(establishmentId, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals(20, result.valuePerPoint());
        assertEquals(establishmentTest.getName(), result.name());
        verify(establishmentRepository, times(1)).findById(establishmentId);
        verify(establishmentRepository, times(1)).save(any(Establishment.class));
    }

    @Test
    void testUpdateEstablishmentById_UpdateMultipleFields() {
        // Arrange
        Long establishmentId = 1L;
        EstablishmentDto updateDto = new EstablishmentDto(
            "Updated Establishment",
            "newemail@example.com",
            "8888888888",
            null,
            25
        );

        Establishment updatedEstablishment = new Establishment();
        updatedEstablishment.setId(1L);
        updatedEstablishment.setName("Updated Establishment");
        updatedEstablishment.setEmail("newemail@example.com");
        updatedEstablishment.setPhone("8888888888");
        updatedEstablishment.setCnpj(establishmentTest.getCnpj());
        updatedEstablishment.setValuePerPoint(25);

        when(establishmentRepository.findById(establishmentId))
                .thenReturn(Optional.of(establishmentTest));
        when(establishmentRepository.save(any(Establishment.class)))
                .thenReturn(updatedEstablishment);

        // Act
        EstablishmentDto result = establishmentService.updateEstablishmentById(establishmentId, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Establishment", result.name());
        assertEquals("newemail@example.com", result.email());
        assertEquals("8888888888", result.phone());
        assertEquals(25, result.valuePerPoint());
        verify(establishmentRepository, times(1)).findById(establishmentId);
        verify(establishmentRepository, times(1)).save(any(Establishment.class));
    }

    private Establishment buildEstablishment() {
        Establishment establishment = new Establishment();
        establishment.setId(1L);
        establishment.setName("Test Establishment");
        establishment.setValuePerPoint(10);
        return establishment;
    }
}

