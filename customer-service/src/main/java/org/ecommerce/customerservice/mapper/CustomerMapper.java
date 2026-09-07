package org.ecommerce.customerservice.mapper;

import org.ecommerce.customerservice.entity.Customer;
import org.ecommerce.customerservice.request.CustomerRequest;
import org.ecommerce.customerservice.response.CustomerResponse;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CustomerMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "passwordHash", ignore = true),
            @Mapping(target = "isActive", ignore = true),
            @Mapping(target = "isEmailVerified", ignore = true),
            @Mapping(target = "emailVerifiedAt", ignore = true),
            @Mapping(target = "lastLoginAt", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "deletedAt", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "roles", ignore = true)
    })
    Customer toCustomer(CustomerRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "passwordHash", ignore = true),
            @Mapping(target = "roles", ignore = true),
            @Mapping(target = "version", ignore = true),
            @Mapping(target = "deletedAt", ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "emailVerifiedAt", ignore = true),
            @Mapping(target = "lastLoginAt", ignore = true),
            @Mapping(target = "isActive", ignore = true),
            @Mapping(target = "isEmailVerified", ignore = true)
    })
    void partialUpdate(CustomerRequest request, @MappingTarget Customer customer);

    CustomerResponse toCustomerResponse(Customer customer);
}
