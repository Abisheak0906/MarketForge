package com.bajrix.marketplace.controller;

import com.bajrix.marketplace.dto.SellerResponse;
import com.bajrix.marketplace.service.MarketplaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
public class SellersController {
    private final MarketplaceService marketplaceService;

    @GetMapping
    public ResponseEntity<List<SellerResponse>> getSellers() {
        return ResponseEntity.ok(marketplaceService.getSellers());
    }
}
