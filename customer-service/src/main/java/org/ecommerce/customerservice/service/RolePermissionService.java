package org.ecommerce.customerservice.service;

import org.ecommerce.customerservice.entity.Role;
import org.ecommerce.customerservice.request.CreateRoleRequest;

import java.util.List;

public interface RolePermissionService {

    Role getRoleWithName(final String roleName);

    List<Role> getAllRoles();

    Role createRole(final CreateRoleRequest request);
}
