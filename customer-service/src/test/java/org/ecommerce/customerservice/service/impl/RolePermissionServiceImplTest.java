package org.ecommerce.customerservice.service.impl;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolePermissionServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private RolePermissionServiceImpl rolePermissionService;

    @Test
    void createRole_shouldNormalizeAndSaveRole() {
        CreateRoleRequest request = new CreateRoleRequest("admin", " Admin role ");
        Role savedRole = Role.builder().id(1L).name("ROLE_ADMIN").description("Admin role").build();

        when(roleRepository.existsByNameIgnoreCase("ROLE_ADMIN")).thenReturn(false);
        when(roleRepository.save(org.mockito.ArgumentMatchers.any(Role.class))).thenReturn(savedRole);

        Role result = rolePermissionService.createRole(request);

        assertThat(result).isEqualTo(savedRole);
    }

    @Test
    void createRole_shouldThrowOnDuplicate() {
        CreateRoleRequest request = new CreateRoleRequest("ROLE_ADMIN", "Admin role");
        when(roleRepository.existsByNameIgnoreCase("ROLE_ADMIN")).thenReturn(true);

        assertThatThrownBy(() -> rolePermissionService.createRole(request))
                .isInstanceOf(DuplicateRoleException.class)
                .hasMessage("Role already exists: ROLE_ADMIN");
    }

    @Test
    void getDefaultUserRole_shouldThrowWhenMissing() {
        when(roleRepository.findByNameIgnoreCase("ROLE_USER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rolePermissionService.getDefaultUserRole())
                .isInstanceOf(DefaultRoleMissingException.class)
                .hasMessage("Default role is missing: ROLE_USER");
    }

    @Test
    void getRoleWithName_shouldThrowWhenNotFound() {
        when(roleRepository.findByNameIgnoreCase("ROLE_MANAGER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rolePermissionService.getRoleWithName("manager"))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessage("Role not found: ROLE_MANAGER");
    }

    @Test
    void createPermission_shouldNormalizeAndSavePermission() {
        CreatePermissionRequest request = new CreatePermissionRequest("view_orders", " Can view orders ", "Orders");
        Permission savedPermission = Permission.builder()
                .id(1L)
                .name("VIEW_ORDERS")
                .description("Can view orders")
                .category("orders")
                .build();

        when(permissionRepository.existsByNameIgnoreCase("VIEW_ORDERS")).thenReturn(false);
        when(permissionRepository.save(org.mockito.ArgumentMatchers.any(Permission.class))).thenReturn(savedPermission);

        Permission result = rolePermissionService.createPermission(request);

        assertThat(result).isEqualTo(savedPermission);
    }

    @Test
    void createPermission_shouldThrowOnDuplicate() {
        CreatePermissionRequest request = new CreatePermissionRequest("VIEW_ORDERS", "Can view orders", "orders");
        when(permissionRepository.existsByNameIgnoreCase("VIEW_ORDERS")).thenReturn(true);

        assertThatThrownBy(() -> rolePermissionService.createPermission(request))
                .isInstanceOf(DuplicatePermissionException.class)
                .hasMessage("Permission already exists: VIEW_ORDERS");
    }

    @Test
    void getPermissionWithName_shouldThrowWhenNotFound() {
        when(permissionRepository.findByNameIgnoreCase("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rolePermissionService.getPermissionWithName("unknown"))
                .isInstanceOf(PermissionNotFoundException.class)
                .hasMessage("Permission not found: UNKNOWN");
    }

    @Test
    void assignPermissionToRole_shouldAddPermissionAndPersistRole() {
        Role role = Role.builder().id(1L).name("ROLE_ADMIN").permissions(new HashSet<>()).build();
        Permission permission = Permission.builder().id(2L).name("VIEW_ORDERS").build();

        when(roleRepository.findByNameIgnoreCase("ROLE_ADMIN")).thenReturn(Optional.of(role));
        when(permissionRepository.findByNameIgnoreCase("VIEW_ORDERS")).thenReturn(Optional.of(permission));
        when(roleRepository.save(role)).thenReturn(role);

        Role result = rolePermissionService.assignPermissionToRole("admin", "view_orders");

        assertThat(result.getPermissions()).contains(permission);
        verify(roleRepository).save(role);
    }
}

