package com.bajrix.marketplace.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    private Long totalSellers;
    private Long listingsUnderReview;
}
