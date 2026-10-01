/**
 * Inbound request models accepted by the customer-service HTTP API.
 *
 * <p>These records capture the external contract for customer registration,
 * authentication, token refresh, and RBAC administration workflows. Bean
 * Validation annotations are applied directly on record components so invalid
 * payloads are rejected before service-layer execution.</p>
 */
package org.ecommerce.customerservice.request;

