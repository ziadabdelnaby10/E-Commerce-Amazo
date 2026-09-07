package org.ecommerce.customerservice.mapper;

import org.ecommerce.customerservice.entity.Role;
import org.ecommerce.customerservice.request.CreateRoleRequest;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface RoleMapper {

    Role toEntity(CreateRoleRequest createRoleRequest);

    CreateRoleRequest toDto(Role role);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Role partialUpdate(CreateRoleRequest createRoleRequest, @MappingTarget Role role);
}