package org.ecommerce.inventoryservice.mapper;

import lombok.RequiredArgsConstructor;
import org.ecommerce.inventoryservice.model.entity.Product;
import org.ecommerce.inventoryservice.model.entity.StockLevel;
import org.ecommerce.inventoryservice.model.request.ProductRequest;
import org.ecommerce.inventoryservice.model.request.ProductUpdateRequest;
import org.ecommerce.inventoryservice.model.response.ProductResponse;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {StockMapper.class})
//@RequiredArgsConstructor
public interface ProductMapper {

//    protected final StockMapper stockMapper;


    // ── Request → Entity ─────────────────────────────────────────────────────

    /**
     * Creates a new Product from the incoming request.
     * Trims sku/name, applies reorder defaults, seeds timestamps and version.
     * Callers must set status, createdAt, updatedAt and version after calling this.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "sku", expression = "java(request.sku().trim())")
    @Mapping(target = "name", expression = "java(request.name().trim())")
    public abstract Product toEntity(ProductRequest request);

    /**
     * Partially updates an existing Product entity from a request.
     * Null values in the request are not applied (IGNORE strategy).
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "sku", expression = "java(request.sku()  != null ? request.sku().trim()  : null)")
    @Mapping(target = "name", expression = "java(request.name() != null ? request.name().trim() : null)")
    public abstract void partialUpdate(ProductUpdateRequest request, @MappingTarget Product product);

    // ── Entity → Response ────────────────────────────────────────────────────

    /**
     * Maps a Product entity to a ProductResponse.
     */
    @Mapping(target = "stockLevel", source = "stockLevel")
    @Mapping(target = "id", source = "product.id")
    @Mapping(target = "createdAt", source = "product.createdAt")
    @Mapping(target = "updatedAt", source = "product.updatedAt")
    @Mapping(target = "version", source = "product.version")
    public abstract ProductResponse toResponse(Product product, StockLevel stockLevel);

    @Mapping(target = "stockLevel", ignore = true)
    public abstract ProductResponse toResponse(Product product);
}
