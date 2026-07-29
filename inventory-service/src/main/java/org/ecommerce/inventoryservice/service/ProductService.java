package org.ecommerce.inventoryservice.service;

import org.ecommerce.inventoryservice.model.entity.Product;
import org.ecommerce.inventoryservice.model.entity.ProductStatus;
import org.ecommerce.inventoryservice.model.filter.ProductFilter;
import org.ecommerce.inventoryservice.model.request.ProductRequest;
import org.ecommerce.inventoryservice.model.request.ProductStatusUpdateRequest;
import org.ecommerce.inventoryservice.model.request.ProductUpdateRequest;
import org.ecommerce.inventoryservice.model.response.ProductResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long productId, ProductUpdateRequest request);

    ProductResponse getProduct(Long productId);

    Product findProductById(Long productId);

    Boolean existsProductById(Long productId);

    List<ProductResponse> listProducts(ProductStatus status);

    Page<ProductResponse> listProductsPaginated(ProductFilter filter, int page, int size);

    ProductResponse updateProductStatus(Long productId, ProductStatusUpdateRequest request);
}
