package org.ecommerce.customerservice.service;

import org.ecommerce.customerservice.entity.Permission;
import org.ecommerce.customerservice.entity.Role;
import org.ecommerce.customerservice.request.CreatePermissionRequest;
import org.ecommerce.customerservice.request.CreateRoleRequest;

import java.util.List;

/**
 * Role and permission administration contract.
 */
public interface RolePermissionService {

    /** @return role entity with permissions loaded */
    Role getRoleWithName(final String roleName);

    /** @return all stored roles */
    List<Role> getAllRoles();

    /** @return newly created role */
    Role createRole(final CreateRoleRequest request);

    /** @return the default role assigned to newly registered customers */
    Role getDefaultUserRole();

    /** @return permission entity for the given name */
    Permission getPermissionWithName(final String permissionName);

    /** @return newly created permission */
    Permission createPermission(final CreatePermissionRequest request);

    /** @return role after permission assignment has been persisted */
    Role assignPermissionToRole(final String roleName, final String permissionName);
}
