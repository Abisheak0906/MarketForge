package com.bajrix.marketplace.controller;

import com.bajrix.marketplace.dto.*;
import com.bajrix.marketplace.service.MarketplaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class SellerController {
    private final MarketplaceService marketplaceService;

    @GetMapping("/listings")
    public ResponseEntity<Page<SellerListingResponse>> getListings(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @PageableDefault(size = 20, sort = {"id"}) Pageable pageable) {
        return ResponseEntity.ok(marketplaceService.getSellerListings(sellerId, pageable));
    }

    @PostMapping("/listings")
    public ResponseEntity<SellerListingResponse> createListing(
            @RequestHeader("X-Seller-Id") Long sellerId,
            @Valid @RequestBody SellerListingRequest request) {
        return ResponseEntity.ok(marketplaceService.createListing(sellerId, request));
    }

    @PatchMapping("/listings/{id}")
    public ResponseEntity<SellerListingResponse> updateListing(
            @PathVariable Long id,
            @RequestHeader("X-Seller-Id") Long sellerId,
            @Valid @RequestBody SellerListingUpdateRequest request) {
        return ResponseEntity.ok(marketplaceService.updateListing(id, sellerId, request));
    }

    @DeleteMapping("/listings/{id}")
    public ResponseEntity<Void> deleteListing(
            @PathVariable Long id,
            @RequestHeader("X-Seller-Id") Long sellerId) {
        marketplaceService.deleteListing(id, sellerId);
        return ResponseEntity.noContent().build();
    }
}
