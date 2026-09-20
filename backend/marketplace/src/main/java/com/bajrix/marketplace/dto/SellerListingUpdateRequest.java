package com.bajrix.marketplace.dto;

import com.bajrix.marketplace.model.ListingStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerListingUpdateRequest {
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stockQuantity;

    @Min(value = 1, message = "MOQ must be at least 1")
    private Integer minimumOrderQuantity;

    private ListingStatus status;

    private Long version;
}
