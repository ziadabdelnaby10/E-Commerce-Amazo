package org.ecommerce.customerservice.entity;

import lombok.*;
import org.springframework.validation.annotation.Validated;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode
@Validated
public class Address {

    private String street;

    private String houseNumber;

    private String zipCode;
}
