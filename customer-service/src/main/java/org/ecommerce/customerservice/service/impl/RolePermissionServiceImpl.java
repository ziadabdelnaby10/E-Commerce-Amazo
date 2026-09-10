package org.ecommerce.customerservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.ecommerce.customerservice.entity.Permission;
import org.ecommerce.customerservice.entity.Role;
import org.ecommerce.customerservice.exception.DefaultRoleMissingException;
import org.ecommerce.customerservice.exception.DuplicatePermissionException;
import org.ecommerce.customerservice.exception.DuplicateRoleException;
import org.ecommerce.customerservice.exception.PermissionNotFoundException;
import org.ecommerce.customerservice.exception.RoleNotFoundException;
import org.ecommerce.customerservice.repository.PermissionRepository;
import org.ecommerce.customerservice.repository.RoleRepository;
import org.ecommerce.customerservice.request.CreatePermissionRequest;
import org.ecommerce.customerservice.request.CreateRoleRequest;
import org.ecommerce.customerservice.service.RolePermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RolePermissionServiceImpl implements RolePermissionService {

    private static final String DEFAULT_USER_ROLE = "ROLE_USER";

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public Role getRoleWithName(String roleName) {
        String normalizedRoleName = normalizeRoleName(roleName);
        return roleRepository.findByNameIgnoreCase(normalizedRoleName)
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + normalizedRoleName));
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional
    public Role createRole(CreateRoleRequest request) {
        String normalizedRoleName = normalizeRoleName(request.name());
        if (roleRepository.existsByNameIgnoreCase(normalizedRoleName)) {
            throw new DuplicateRoleException("Role already exists: " + normalizedRoleName);
        }

        Role role = Role.builder()
                .name(normalizedRoleName)
                .description(trimToNull(request.description()))
                .build();
        return roleRepository.save(role);
    }

    @Override
    public Role getDefaultUserRole() {
        return roleRepository.findByNameIgnoreCase(DEFAULT_USER_ROLE)
                .orElseThrow(() -> new DefaultRoleMissingException("Default role is missing: " + DEFAULT_USER_ROLE));
    }

    @Override
    public Permission getPermissionWithName(String permissionName) {
        String normalizedPermissionName = normalizePermissionName(permissionName);
        return permissionRepository.findByNameIgnoreCase(normalizedPermissionName)
                .orElseThrow(() -> new PermissionNotFoundException("Permission not found: " + normalizedPermissionName));
    }

    @Override
    @Transactional
    public Permission createPermission(CreatePermissionRequest request) {
        String normalizedPermissionName = normalizePermissionName(request.name());
        if (permissionRepository.existsByNameIgnoreCase(normalizedPermissionName)) {
            throw new DuplicatePermissionException("Permission already exists: " + normalizedPermissionName);
        }

        Permission permission = Permission.builder()
                .name(normalizedPermissionName)
                .description(trimToNull(request.description()))
                .category(request.category().trim().toLowerCase(Locale.ROOT))
                .build();
        return permissionRepository.save(permission);
    }

    @Override
    @Transactional
    public Role assignPermissionToRole(String roleName, String permissionName) {
        Role role = getRoleWithName(roleName);
        Permission permission = getPermissionWithName(permissionName);
        role.getPermissions().add(permission);
        return roleRepository.save(role);
    }

    private String normalizeRoleName(String roleName) {
        String sanitized = roleName.trim().toUpperCase(Locale.ROOT);
        return sanitized.startsWith("ROLE_") ? sanitized : "ROLE_" + sanitized;
    }

    private String normalizePermissionName(String permissionName) {
        return permissionName.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

