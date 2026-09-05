package vn.gpg.unionportal.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

/**
 * API contract for the inventory workspace.  It intentionally does not reuse the broad member
 * DTOs: gift slips only return the recipient fields needed by the inventory form and report.
 */
public final class InventoryModels {
    private InventoryModels() {
    }

    public record ItemRequest(
            @JsonAlias("unitId") Long unionUnitId,
            @NotBlank @Size(max = 60) String itemCode,
            @NotBlank @Size(max = 200) String itemName,
            @Size(max = 120) String category,
            @Size(max = 200) String supplier,
            @Size(max = 40) String unitOfMeasure,
            @Min(0) Integer minimumStock,
            @Size(max = 1000) String note) {
    }

    public record ReceiptRequest(
            @JsonAlias("unitId") Long unionUnitId,
            @NotNull Long itemId,
            @NotNull LocalDate receiptDate,
            @NotNull @Min(1) Integer quantity,
            @Size(max = 200) String supplier,
            @Size(max = 80) String referenceNo,
            @Size(max = 1000) String note) {
    }

    public record GiftIssueRequest(
            @JsonAlias("unitId") Long unionUnitId,
            @NotNull Long itemId,
            @NotNull Long memberId,
            @NotNull LocalDate issueDate,
            @NotNull @Min(1) Integer quantity,
            @Size(max = 200) String programName,
            @Size(max = 80) String referenceNo,
            @Size(max = 1000) String note) {
    }

    public record ItemView(
            Long id,
            Long unionUnitId,
            String unitCode,
            String companyName,
            String itemCode,
            String itemName,
            String category,
            String supplier,
            String unitOfMeasure,
            int minimumStock,
            long receivedQuantity,
            long issuedQuantity,
            long stockQuantity,
            String note,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record ReceiptView(
            Long id,
            Long unionUnitId,
            String unitCode,
            String companyName,
            Long itemId,
            String itemCode,
            String itemName,
            LocalDate receiptDate,
            int quantity,
            String supplier,
            String referenceNo,
            String note,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record GiftIssueView(
            Long id,
            Long unionUnitId,
            String unitCode,
            String companyName,
            Long itemId,
            String itemCode,
            String itemName,
            Long memberId,
            String employeeCode,
            String recipientName,
            String jobTitle,
            String professionalTitle,
            String workplace,
            String email,
            String phone,
            String gender,
            String placeOfBirth,
            String currentResidence,
            LocalDate startWorkDate,
            LocalDate issueDate,
            int quantity,
            String programName,
            String referenceNo,
            String note,
            Instant createdAt,
            Instant updatedAt) {
    }

    /** Lightweight and scoped member payload used by the recipient auto-complete. */
    public record RecipientSuggestionView(
            Long memberId,
            String employeeCode,
            String recipientName,
            Long unionUnitId,
            String unitCode,
            String companyName,
            String jobTitle,
            String professionalTitle,
            String workplace,
            String email,
            String phone,
            String gender,
            String placeOfBirth,
            String currentResidence,
            LocalDate startWorkDate) {
    }

    public record InventorySummary(
            Long unionUnitId,
            String unitCode,
            String companyName,
            long itemCount,
            long totalReceived,
            long totalIssued,
            long stockQuantity,
            long lowStockCount,
            long outOfStockCount) {
    }
}
