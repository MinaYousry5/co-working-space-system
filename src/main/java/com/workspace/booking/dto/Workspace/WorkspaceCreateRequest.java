package com.workspace.booking.dto.Workspace;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WorkspaceCreateRequest(
        @Schema(description = "Name of the workspace (must be unique)", example = "Executive Meeting Room")
        @NotBlank(message = "Workspace name is required")
        String workspaceName,

        @Schema(description = "Unique code identifier for the workspace", example = "MR-001")
        @NotBlank(message = "Workspace code is required")
        String workspaceCode,

        @Schema(description = "Detailed description of the workspace including amenities and features",
                example = "Spacious meeting room with 65-inch TV, whiteboard, and conference calling system")
        String description,

        @Schema(description = "Maximum number of people the workspace can accommodate",
                example = "10", minimum = "1")
        @NotNull(message = "Capacity is required")
        @Min(value = 1, message = "Capacity must be at least 1")
        Integer capacity,

        @Schema(description = "Hourly price in the specified currency",
                example = "50.00", required = true)
        @NotNull(message = "Hourly price is required")
        BigDecimal priceHourly,

        @Schema(description = "Daily price in the specified currency (optional)",
                example = "300.00")
        BigDecimal priceDaily,

        @Schema(description = "Monthly price in the specified currency (optional)",
                example = "5000.00")
        BigDecimal priceMonthly,

        @Schema(description = "Currency code (ISO 4217 format)",
                example = "EGP", allowableValues = {"EGP", "USD", "EUR", "GBP"})
        @NotBlank(message = "Currency is required")
        String currency,
        
        @Schema(description = "Is workspace available", example = "YES")
        com.workspace.booking.common.enums.YesNo isAvailable,

        @Schema(description = "ID of the workspace type/category",
                example = "3", required = true)
        @NotNull(message = "Workspace type ID is required")
        Long workspaceTypeId,

        @Schema(description = "Floor number where the workspace is located",
                example = "2", minimum = "0")
        Integer floorNumber,

        @Schema(description = "Room/suite number identifier",
                example = "Suite 5B")
        String roomNumber,

        @Schema(description = "Minimum booking hours required",
                example = "2", minimum = "1", defaultValue = "1")
        Integer minBookingHrs,

        @Schema(description = "Maximum booking hours allowed",
                example = "12", minimum = "1", defaultValue = "720")
        Integer maxBookingHrs,

        @Schema(description = "Hours of advance notice required before booking",
                example = "24", minimum = "0", defaultValue = "0")
        Integer advanceNotice,

        @Schema(description = "Cancellation window in hours (free cancellation before this time)",
                example = "24", minimum = "0", defaultValue = "24")
        Integer cancellationHrs,

        @Schema(description = "Base64 encoded floor plan image",
                example = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
                format = "base64")
        String floorPlanBlob,

        @Schema(description = "MIME type of the floor plan image",
                example = "image/png", allowableValues = {"image/png", "image/jpeg", "image/jpg", "application/pdf"})
        String floorPlanMimeType,

        @Schema(description = "Original filename of the uploaded floor plan",
                example = "floor_plan_level_2.png")
        String floorPlanFilename

) {}

