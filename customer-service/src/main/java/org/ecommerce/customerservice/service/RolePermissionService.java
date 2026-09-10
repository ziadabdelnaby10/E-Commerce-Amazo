package org.ecommerce.customerservice.service;

import org.ecommerce.customerservice.entity.Permission;
import org.ecommerce.customerservice.entity.Role;
import org.ecommerce.customerservice.request.CreatePermissionRequest;
import org.ecommerce.customerservice.request.CreateRoleRequest;

import java.util.List;

public interface RolePermissionService {

    Role getRoleWithName(final String roleName);

    List<Role> getAllRoles();

    Role createRole(final CreateRoleRequest request);

    Role getDefaultUserRole();

    Permission getPermissionWithName(final String permissionName);

    Permission createPermission(final CreatePermissionRequest request);

    Role assignPermissionToRole(final String roleName, final String permissionName);
}
