package com.bajrix.marketplace.controller;

import com.bajrix.marketplace.dto.*;
import com.bajrix.marketplace.service.MarketplaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final MarketplaceService marketplaceService;

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20, sort = {"name", "id"}) Pageable pageable) {
        return ResponseEntity.ok(marketplaceService.searchProducts(search, category, pageable));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(marketplaceService.getCategories());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(marketplaceService.getProduct(id));
    }

    @GetMapping("/{id}/listings")
    public ResponseEntity<Page<SellerListingResponse>> getProductListings(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = {"price", "id"}) Pageable pageable) {
        return ResponseEntity.ok(marketplaceService.getProductListings(id, pageable));
    }
}
