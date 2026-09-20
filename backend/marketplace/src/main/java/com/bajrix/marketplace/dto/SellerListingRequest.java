package com.bajrix.marketplace.dto;

import com.bajrix.marketplace.model.ListingStatus;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerListingRequest {
    @NotNull
    private Long productId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull
    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stockQuantity;

    @NotNull
    @Min(value = 1, message = "MOQ must be at least 1")
    private Integer minimumOrderQuantity;

    private ListingStatus status;
}
