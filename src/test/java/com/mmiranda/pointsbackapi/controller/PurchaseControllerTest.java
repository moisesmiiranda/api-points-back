package com.mmiranda.pointsbackapi.controller;

import com.mmiranda.pointsbackapi.dto.PurchaseDto;
import com.mmiranda.pointsbackapi.service.PurchaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseControllerTest {

    @Mock
    private PurchaseService purchaseService;

    @InjectMocks
    private PurchaseController purchaseController;

    private PurchaseDto purchaseDto;

    @BeforeEach
    void setUp() {
        purchaseDto = new PurchaseDto(
            1L,
            1L,
            1L,
            BigDecimal.valueOf(100.00)
        );
    }

    @Test
    void testUpdatePurchaseById() {
        // Arrange
        Long purchaseId = 1L;
        PurchaseDto updateDto = new PurchaseDto(
            1L,
            1L,
            1L,
            BigDecimal.valueOf(150.00)
        );

        when(purchaseService.updatePurchaseById(eq(purchaseId), any(PurchaseDto.class)))
                .thenReturn(updateDto);

        // Act
        PurchaseDto result = purchaseController.updatePurchaseById(purchaseId, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(150.00), result.amount());
        verify(purchaseService, times(1)).updatePurchaseById(eq(purchaseId), any(PurchaseDto.class));
    }

    @Test
    void testUpdatePurchaseByIdPurchaseNotFound() {
        // Arrange
        Long purchaseId = 999L;
        PurchaseDto updateDto = new PurchaseDto(
            1L,
            1L,
            1L,
            BigDecimal.valueOf(150.00)
        );

        when(purchaseService.updatePurchaseById(eq(purchaseId), any(PurchaseDto.class)))
                .thenReturn(null);

        // Act
        PurchaseDto result = purchaseController.updatePurchaseById(purchaseId, updateDto);

        // Assert
        assertEquals(null, result);
        verify(purchaseService, times(1)).updatePurchaseById(eq(purchaseId), any(PurchaseDto.class));
    }

    @Test
    void testUpdatePurchaseByIdWithOnlyAmount() {
        // Arrange
        Long purchaseId = 1L;
        PurchaseDto updateDto = new PurchaseDto(
            null,
            null,
            null,
            BigDecimal.valueOf(200.00)
        );

        PurchaseDto expectedResult = new PurchaseDto(
            1L,
            1L,
            1L,
            BigDecimal.valueOf(200.00)
        );

        when(purchaseService.updatePurchaseById(eq(purchaseId), any(PurchaseDto.class)))
                .thenReturn(expectedResult);

        // Act
        PurchaseDto result = purchaseController.updatePurchaseById(purchaseId, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(200.00), result.amount());
        assertEquals(1L, result.clientId());
        assertEquals(1L, result.establishmentId());
        verify(purchaseService, times(1)).updatePurchaseById(eq(purchaseId), any(PurchaseDto.class));
    }

    @Test
    void testUpdatePurchaseByIdWithClientAndEstablishment() {
        // Arrange
        Long purchaseId = 1L;
        PurchaseDto updateDto = new PurchaseDto(
            null,
            2L,
            2L,
            BigDecimal.valueOf(250.00)
        );

        PurchaseDto expectedResult = new PurchaseDto(
            1L,
            2L,
            2L,
            BigDecimal.valueOf(250.00)
        );

        when(purchaseService.updatePurchaseById(eq(purchaseId), any(PurchaseDto.class)))
                .thenReturn(expectedResult);

        // Act
        PurchaseDto result = purchaseController.updatePurchaseById(purchaseId, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.clientId());
        assertEquals(2L, result.establishmentId());
        assertEquals(BigDecimal.valueOf(250.00), result.amount());
        verify(purchaseService, times(1)).updatePurchaseById(eq(purchaseId), any(PurchaseDto.class));
    }

}

