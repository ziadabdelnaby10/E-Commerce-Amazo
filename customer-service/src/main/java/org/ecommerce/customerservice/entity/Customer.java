package org.ecommerce.customerservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode
@Document
public class Customer {

    @Id
    private String id;

    private String firstName;

    private String lastName;

    private String email;

    private Address address;
}
