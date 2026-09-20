package com.bajrix.marketplace.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private String category;
    private String unit;
    private BigDecimal lowestPrice;
    private Long activeSellerCount;
}
