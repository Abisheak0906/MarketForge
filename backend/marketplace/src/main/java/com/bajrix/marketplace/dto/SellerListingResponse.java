package com.bajrix.marketplace.dto;

import com.bajrix.marketplace.model.ListingStatus;
import com.bajrix.marketplace.model.SellerStatus;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerListingResponse {
    private Long id;
    private Long productId;
    private String productName;
    private Long sellerId;
    private String sellerName;
    private SellerStatus sellerStatus;
    private BigDecimal price;
    private Integer stockQuantity;
    private Integer minimumOrderQuantity;
    private ListingStatus status;
    private Long version;
}
