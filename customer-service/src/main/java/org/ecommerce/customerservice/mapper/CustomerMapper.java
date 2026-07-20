package org.ecommerce.customerservice.mapper;

import org.ecommerce.customerservice.entity.Customer;
import org.ecommerce.customerservice.request.CustomerRequest;
import org.ecommerce.customerservice.response.CustomerResponse;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CustomerMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true)
    })
    Customer toCustomer(CustomerRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void partialUpdate(CustomerRequest request, @MappingTarget Customer customer);

    CustomerResponse toCustomerResponse(Customer customer);
}
