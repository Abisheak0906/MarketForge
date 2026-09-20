package com.bajrix.marketplace.service;

import com.bajrix.marketplace.dto.*;
import com.bajrix.marketplace.exception.*;
import com.bajrix.marketplace.model.*;
import com.bajrix.marketplace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketplaceService {
    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;
    private final SellerListingRepository listingRepository;

    public Page<ProductResponse> searchProducts(String search, String category, Pageable pageable) {
        String namePattern = (search == null || search.isBlank()) ? "%" : "%" + search.trim() + "%";
        String normalizedCategory = (category == null || category.isBlank()) ? "" : category.trim();
        Page<Product> products = productRepository.search(namePattern, !normalizedCategory.isEmpty(), normalizedCategory, pageable);
        Map<Long, OfferSummary> summaries = loadOfferSummaries(products.getContent().stream().map(Product::getId).toList());
        return products.map(p -> mapToProductResponse(p, summaries.get(p.getId())));
    }

    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToProductResponse(product, loadOfferSummaries(List.of(id)).get(id));
    }

    private Map<Long, OfferSummary> loadOfferSummaries(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return listingRepository.summarizeOffersForProducts(productIds, ListingStatus.ACTIVE, SellerStatus.APPROVED)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> new OfferSummary((BigDecimal) row[1], (Long) row[2])));
    }

    private record OfferSummary(BigDecimal lowestPrice, Long activeSellerCount) {}

    public List<String> getCategories() {
        return productRepository.findDistinctCategories();
    }

    public List<SellerResponse> getSellers() {
        return sellerRepository.findAllByOrderByNameAsc().stream()
                .map(s -> SellerResponse.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .status(s.getStatus())
                        .build())
                .toList();
    }

    public Page<SellerListingResponse> getProductListings(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
        return listingRepository.findByProductIdAndStatusWithDetails(
                        productId, ListingStatus.ACTIVE, SellerStatus.APPROVED, pageable)
                .map(this::mapToListingResponse);
    }

    public Page<SellerListingResponse> getSellerListings(Long sellerId, Pageable pageable) {
        if (!sellerRepository.existsById(sellerId)) {
            throw new ResourceNotFoundException("Seller not found with id: " + sellerId);
        }
        return listingRepository.findBySellerIdWithDetails(sellerId, pageable)
                .map(this::mapToListingResponse);
    }

    @Transactional
    public SellerListingResponse createListing(Long sellerId, SellerListingRequest request) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + sellerId));

        if (seller.getStatus() != SellerStatus.APPROVED) {
            throw new AccessDeniedException("Only approved sellers can create listings. Current status: " + seller.getStatus());
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        listingRepository.findBySellerIdAndProductId(sellerId, product.getId())
                .ifPresent(l -> {
                    throw new DuplicateListingException("Seller already has a listing for this product");
                });

        SellerListing listing = SellerListing.builder()
                .seller(seller)
                .product(product)
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .minimumOrderQuantity(request.getMinimumOrderQuantity())
                .status(request.getStatus() != null ? request.getStatus() : ListingStatus.ACTIVE)
                .build();

        return mapToListingResponse(listingRepository.save(listing));
    }

    @Transactional
    public SellerListingResponse updateListing(Long listingId, Long sellerId, SellerListingUpdateRequest request) {
        SellerListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));

        if (!listing.getSeller().getId().equals(sellerId)) {
            throw new AccessDeniedException("You do not have permission to modify this listing");
        }

        Seller seller = listing.getSeller();
        if (seller.getStatus() != SellerStatus.APPROVED) {
            throw new AccessDeniedException("Only approved sellers can update listings. Current status: " + seller.getStatus());
        }

        if (request.getVersion() != null && !request.getVersion().equals(listing.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(SellerListing.class.getName(), listingId);
        }

        if (request.getPrice() != null) listing.setPrice(request.getPrice());
        if (request.getStockQuantity() != null) listing.setStockQuantity(request.getStockQuantity());
        if (request.getMinimumOrderQuantity() != null) listing.setMinimumOrderQuantity(request.getMinimumOrderQuantity());
        if (request.getStatus() != null) listing.setStatus(request.getStatus());

        return mapToListingResponse(listingRepository.save(listing));
    }

    @Transactional
    public void deleteListing(Long listingId, Long sellerId) {
        SellerListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found with id: " + listingId));

        if (!listing.getSeller().getId().equals(sellerId)) {
            throw new AccessDeniedException("You do not have permission to delete this listing");
        }

        listing.setStatus(ListingStatus.STOPPED);
        listingRepository.save(listing);
    }

    private ProductResponse mapToProductResponse(Product product, OfferSummary summary) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .unit(product.getUnit())
                .lowestPrice(summary != null ? summary.lowestPrice() : null)
                .activeSellerCount(summary != null ? summary.activeSellerCount() : 0L)
                .build();
    }

    private SellerListingResponse mapToListingResponse(SellerListing listing) {
        return SellerListingResponse.builder()
                .id(listing.getId())
                .productId(listing.getProduct().getId())
                .productName(listing.getProduct().getName())
                .sellerId(listing.getSeller().getId())
                .sellerName(listing.getSeller().getName())
                .sellerStatus(listing.getSeller().getStatus())
                .price(listing.getPrice())
                .stockQuantity(listing.getStockQuantity())
                .minimumOrderQuantity(listing.getMinimumOrderQuantity())
                .status(listing.getStatus())
                .version(listing.getVersion())
                .build();
    }
}
