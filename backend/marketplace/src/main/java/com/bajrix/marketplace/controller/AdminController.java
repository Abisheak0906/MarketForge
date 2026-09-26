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
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final MarketplaceService marketplaceService;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboard() {
        return ResponseEntity.ok(marketplaceService.getAdminDashboard());
    }

    @GetMapping("/sellers/count")
    public ResponseEntity<Long> getTotalSellers() {
        return ResponseEntity.ok(marketplaceService.getTotalSellers());
    }

    @GetMapping("/sellers")
    public ResponseEntity<List<SellerResponse>> getSellers() {
        return ResponseEntity.ok(marketplaceService.getSellers());
    }

    @GetMapping("/sellers/{sellerId}/listings")
    public ResponseEntity<Page<SellerListingResponse>> getSellerListings(
            @PathVariable Long sellerId,
            @PageableDefault(size = 20, sort = {"id"}) Pageable pageable) {
        return ResponseEntity.ok(marketplaceService.getSellerListings(sellerId, pageable));
    }

    @GetMapping("/listings/under-review/count")
    public ResponseEntity<Long> getListingsUnderReview() {
        return ResponseEntity.ok(marketplaceService.getListingsUnderReview());
    }
}
