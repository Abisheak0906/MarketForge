package com.bajrix.marketplace.repository;

import com.bajrix.marketplace.model.ListingStatus;
import com.bajrix.marketplace.model.SellerListing;
import com.bajrix.marketplace.model.SellerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerListingRepository extends JpaRepository<SellerListing, Long> {

    @Query("""
        SELECT sl
        FROM SellerListing sl
        JOIN FETCH sl.product
        JOIN FETCH sl.seller
        WHERE sl.seller.id = :sellerId
    """)
    Page<SellerListing> findBySellerIdWithDetails(
        @Param("sellerId") Long sellerId,
        Pageable pageable
    );

    @Query("""
        SELECT sl
        FROM SellerListing sl
        JOIN FETCH sl.product
        JOIN FETCH sl.seller
        WHERE sl.product.id = :productId
          AND sl.status = :status
          AND sl.seller.status = :sellerStatus
    """)
    Page<SellerListing> findByProductIdAndStatusWithDetails(
        @Param("productId") Long productId,
        @Param("status") ListingStatus status,
        @Param("sellerStatus") SellerStatus sellerStatus,
        Pageable pageable
    );

    Optional<SellerListing> findBySellerIdAndProductId(
        Long sellerId,
        Long productId
    );

    @Query("""
        SELECT l.product.id, MIN(l.price), COUNT(l.id)
        FROM SellerListing l
        WHERE l.product.id IN :productIds
          AND l.status = :status
          AND l.seller.status = :sellerStatus
        GROUP BY l.product.id
    """)
    List<Object[]> summarizeOffersForProducts(
        @Param("productIds") List<Long> productIds,
        @Param("status") ListingStatus status,
        @Param("sellerStatus") SellerStatus sellerStatus
    );

    long countByStatusAndSellerStatus(ListingStatus status, SellerStatus sellerStatus);
}
