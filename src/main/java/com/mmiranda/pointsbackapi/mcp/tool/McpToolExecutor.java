package com.mmiranda.pointsbackapi.mcp.tool;

import com.mmiranda.pointsbackapi.dto.ClientDto;
import com.mmiranda.pointsbackapi.dto.EstablishmentDto;
import com.mmiranda.pointsbackapi.dto.PurchaseDto;
import com.mmiranda.pointsbackapi.service.ClientService;
import com.mmiranda.pointsbackapi.service.EstablishmentService;
import com.mmiranda.pointsbackapi.service.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class McpToolExecutor {
    @Autowired
    private ClientService clientService;

    @Autowired
    private EstablishmentService establishmentService;

    @Autowired
    private PurchaseService purchaseService;

    public Object executeTool(String toolName, Map<String, Object> params) throws Exception {
        return switch (toolName) {
            // Client tools
            case "list_all_clients" -> clientService.listAllClients();
            case "get_client" -> clientService.getClientById(getLongParam(params, "id"));
            case "create_client" -> clientService.createClient(extractClientDto(params));
            case "update_client" -> clientService.updateClient(
                getLongParam(params, "id"),
                extractClientDto(params)
            );
            case "add_points_to_client" -> clientService.addPoints(
                getLongParam(params, "id"),
                getIntParam(params, "points")
            );

                // Establishment tools
                case "list_all_establishments" -> establishmentService.listAllEstablishments();
                case "get_establishment" -> establishmentService.getEstablishmentById(getLongParam(params, "id"));
                case "create_establishment" -> establishmentService.createEstablishment(extractEstablishmentDto(params));
                case "update_establishment" -> establishmentService.updateEstablishmentById(
                    getLongParam(params, "id"),
                    extractEstablishmentDto(params)
                );

            // Purchase tools
            case "list_all_purchases" -> purchaseService.listAllPurchases();
            case "get_purchase" -> purchaseService.getPurchaseById(getLongParam(params, "id"));
            case "create_purchase" -> {
                purchaseService.registerPurchase(extractPurchaseDto(params));
                yield Map.of("status", "success", "message", "Purchase created successfully");
            }
            case "update_purchase" -> purchaseService.updatePurchaseById(
                getLongParam(params, "id"),
                extractPurchaseDto(params)
            );

            default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
        };
    }

    private ClientDto extractClientDto(Map<String, Object> params) {
        return new ClientDto(
            (String) params.get("name"),
            (String) params.get("email"),
            (String) params.get("phone"),
            (String) params.get("cpf"),
            params.get("points") != null ? ((Number) params.get("points")).intValue() : null
        );
    }

    private EstablishmentDto extractEstablishmentDto(Map<String, Object> params) {
        return new EstablishmentDto(
            (String) params.get("name"),
            (String) params.get("email"),
            (String) params.get("phone"),
            (String) params.get("cnpj"),
            params.get("valuePerPoint") != null ? ((Number) params.get("valuePerPoint")).intValue() : null
        );
    }

    private PurchaseDto extractPurchaseDto(Map<String, Object> params) {
        return new PurchaseDto(
            null,
            params.get("client_id") != null ? ((Number) params.get("client_id")).longValue() : null,
            params.get("establishment_id") != null ? ((Number) params.get("establishment_id")).longValue() : null,
            params.get("amount") != null ? new java.math.BigDecimal(params.get("amount").toString()) : null
        );
    }

    private Long getLongParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return ((Number) value).longValue();
    }

    private int getIntParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return ((Number) value).intValue();
    }
}






