package com.bajrix.marketplace.dto;

import com.bajrix.marketplace.model.SellerStatus;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerResponse {
    private Long id;
    private String name;
    private SellerStatus status;
}
