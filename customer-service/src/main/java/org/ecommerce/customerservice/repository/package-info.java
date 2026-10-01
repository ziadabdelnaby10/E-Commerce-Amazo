/**
 * Spring Data repositories for customer, RBAC, and refresh-token persistence.
 *
 * <p>Repository methods are intentionally named around soft-delete awareness,
 * eager relationship loading for authentication, and token lookup semantics
 * needed by the login and refresh flows.</p>
 */
package org.ecommerce.customerservice.repository;

