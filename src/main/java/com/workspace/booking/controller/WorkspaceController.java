package com.workspace.booking.controller;

import com.workspace.booking.common.ApiResponse;
import com.workspace.booking.dto.Workspace.WorkspaceAvailabilityResponse;
import com.workspace.booking.dto.Workspace.WorkspaceCreateRequest;
import com.workspace.booking.dto.Workspace.WorkspaceResponse;
import com.workspace.booking.dto.Workspace.WorkspaceSearchRequest;
import com.workspace.booking.dto.booking.ResourceResponse;
import com.workspace.booking.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {
    private final WorkspaceService service;

    @PostMapping
    @Operation(summary = "Create a new workspace",
            description = "Creates a new workspace with the provided details")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> create(@Valid @RequestBody WorkspaceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.create(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get workspace by ID",
            description = "Retrieves detailed information of a specific workspace")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @GetMapping
    @Operation(summary = "Get all workspaces",
            description = "Retrieves a list of all available workspaces")
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @GetMapping("/available")
    @Operation(summary = "Get workspaces available for booking",
            description = "Returns workspaces that can be booked in the provided date/time range")
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> getAvailableForBooking(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDatetime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDatetime) {
        return ResponseEntity.ok(ApiResponse.success(service.getAvailableForBooking(startDatetime, endDatetime)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update workspace",
            description = "Updates an existing workspace with the provided details")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody WorkspaceCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete workspace",
            description = "Soft deletes or permanently removes a workspace")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Workspace deleted successfully"));
    }

}

