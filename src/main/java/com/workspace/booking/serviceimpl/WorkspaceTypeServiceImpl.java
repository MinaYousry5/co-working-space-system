package com.workspace.booking.serviceimpl;

import com.workspace.booking.dto.workspacetype.WorkspaceTypeRequest;
import com.workspace.booking.dto.workspacetype.WorkspaceTypeResponse;
import com.workspace.booking.entity.workspace.WorkspaceType;
import com.workspace.booking.repository.WorkspaceTypeRepository;
import com.workspace.booking.service.WorkspaceTypeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import com.workspace.booking.repository.WorkspaceRepository;

@Service
@RequiredArgsConstructor
public class WorkspaceTypeServiceImpl implements WorkspaceTypeService {
    private final WorkspaceTypeRepository repository;
    private final WorkspaceRepository workspaceRepository;

    @Override
    @Transactional
    public WorkspaceTypeResponse create(WorkspaceTypeRequest request) {
        WorkspaceType entity = WorkspaceType.builder()
                .typeCode(request.getTypeCode())
                .typeName(request.getTypeName())
                .description(request.getDescription())
                .iconName(request.getIconName())
                .isActive(request.getIsActive())
                .build();

        return mapToResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public WorkspaceTypeResponse update(Long id, WorkspaceTypeRequest request) {
        WorkspaceType entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("WorkspaceType not found with id: " + id));

        entity.setTypeCode(request.getTypeCode());
        entity.setTypeName(request.getTypeName());
        entity.setDescription(request.getDescription());
        entity.setIconName(request.getIconName());
        entity.setIsActive(request.getIsActive());

        WorkspaceType updated = repository.save(entity);
        
        if (com.workspace.booking.common.enums.YesNo.N.equals(request.getIsActive())) {
            workspaceRepository.updateIsAvailableByWorkspaceTypeId(updated.getId(), com.workspace.booking.common.enums.YesNo.N);
        }

        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceTypeResponse getById(Long id) {
        return repository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new EntityNotFoundException("WorkspaceType not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceTypeResponse> getAll() {
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Cannot delete: WorkspaceType not found");
        }
        repository.deleteById(id);
    }

    private WorkspaceTypeResponse mapToResponse(WorkspaceType entity) {
        return WorkspaceTypeResponse.builder()
                .id(entity.getId())
                .typeCode(entity.getTypeCode())
                .typeName(entity.getTypeName())
                .description(entity.getDescription())
                .iconName(entity.getIconName())
                .isActive(entity.getIsActive())
                .createdOn(entity.getCreatedOn())
                .createdBy(entity.getCreatedBy())
                .build();
    }


}
