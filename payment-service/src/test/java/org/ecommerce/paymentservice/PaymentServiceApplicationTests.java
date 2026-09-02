package org.ecommerce.paymentservice;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PaymentServiceApplicationTests {

	@Test
	void shouldInstantiateMainApplicationClass() {
		assertNotNull(new PaymentServiceApplication());
	}

}
