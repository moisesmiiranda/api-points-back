package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.dto.EstablishmentDto;
import com.mmiranda.pointsbackapi.service.EstablishmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstablishmentControllerTest {

    @Mock
    private EstablishmentService establishmentService;

    @InjectMocks
    private EstablishmentController establishmentController;

    private EstablishmentDto establishmentDto;

    @BeforeEach
    void setUp() {
        establishmentDto = new EstablishmentDto(
            "Test Establishment",
            "test@example.com",
            "1234567890",
            "12.345.678/0001-90",
            10
        );
    }

    @Test
    void testUpdateEstablishmentById() {
        // Arrange
        Long establishmentId = 1L;
        EstablishmentDto updateDto = new EstablishmentDto(
            "Updated Establishment",
            "updated@example.com",
            "9999999999",
            "99.999.999/9999-99",
            15
        );

        when(establishmentService.updateEstablishmentById(eq(establishmentId), any(EstablishmentDto.class)))
                .thenReturn(updateDto);

        // Act
        EstablishmentDto result = establishmentController.updateEstablishmentById(establishmentId, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Establishment", result.name());
        assertEquals(15, result.valuePerPoint());
        verify(establishmentService, times(1)).updateEstablishmentById(eq(establishmentId), any(EstablishmentDto.class));
    }

    @Test
    void testUpdateEstablishmentByIdNotFound() {
        // Arrange
        Long establishmentId = 999L;
        EstablishmentDto updateDto = new EstablishmentDto(
            "Updated Establishment",
            "updated@example.com",
            "9999999999",
            "99.999.999/9999-99",
            15
        );

        when(establishmentService.updateEstablishmentById(eq(establishmentId), any(EstablishmentDto.class)))
                .thenReturn(null);

        // Act
        EstablishmentDto result = establishmentController.updateEstablishmentById(establishmentId, updateDto);

        // Assert
        assertEquals(null, result);
        verify(establishmentService, times(1)).updateEstablishmentById(eq(establishmentId), any(EstablishmentDto.class));
    }

    @Test
    void testUpdateEstablishmentByIdOnlyName() {
        // Arrange
        Long establishmentId = 1L;
        EstablishmentDto updateDto = new EstablishmentDto(
            "Updated Name",
            null,
            null,
            null,
            null
        );

        EstablishmentDto expectedResult = new EstablishmentDto(
            "Updated Name",
            "test@example.com",
            "1234567890",
            "12.345.678/0001-90",
            10
        );

        when(establishmentService.updateEstablishmentById(eq(establishmentId), any(EstablishmentDto.class)))
                .thenReturn(expectedResult);

        // Act
        EstablishmentDto result = establishmentController.updateEstablishmentById(establishmentId, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Name", result.name());
        assertEquals("test@example.com", result.email());
        verify(establishmentService, times(1)).updateEstablishmentById(eq(establishmentId), any(EstablishmentDto.class));
    }

    @Test
    void testUpdateEstablishmentByIdOnlyValuePerPoint() {
        // Arrange
        Long establishmentId = 1L;
        EstablishmentDto updateDto = new EstablishmentDto(
            null,
            null,
            null,
            null,
            20
        );

        EstablishmentDto expectedResult = new EstablishmentDto(
            "Test Establishment",
            "test@example.com",
            "1234567890",
            "12.345.678/0001-90",
            20
        );

        when(establishmentService.updateEstablishmentById(eq(establishmentId), any(EstablishmentDto.class)))
                .thenReturn(expectedResult);

        // Act
        EstablishmentDto result = establishmentController.updateEstablishmentById(establishmentId, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals(20, result.valuePerPoint());
        assertEquals("Test Establishment", result.name());
        verify(establishmentService, times(1)).updateEstablishmentById(eq(establishmentId), any(EstablishmentDto.class));
    }

    @Test
    void testUpdateEstablishmentByIdMultipleFields() {
        // Arrange
        Long establishmentId = 1L;
        EstablishmentDto updateDto = new EstablishmentDto(
            "Updated Establishment",
            "newemail@example.com",
            "8888888888",
            "88.888.888/8888-88",
            25
        );

        EstablishmentDto expectedResult = new EstablishmentDto(
            "Updated Establishment",
            "newemail@example.com",
            "8888888888",
            "88.888.888/8888-88",
            25
        );

        when(establishmentService.updateEstablishmentById(eq(establishmentId), any(EstablishmentDto.class)))
                .thenReturn(expectedResult);

        // Act
        EstablishmentDto result = establishmentController.updateEstablishmentById(establishmentId, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Establishment", result.name());
        assertEquals("newemail@example.com", result.email());
        assertEquals("8888888888", result.phone());
        assertEquals("88.888.888/8888-88", result.cnpj());
        assertEquals(25, result.valuePerPoint());
        verify(establishmentService, times(1)).updateEstablishmentById(eq(establishmentId), any(EstablishmentDto.class));
    }
}

