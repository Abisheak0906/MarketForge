package com.bajrix.marketplace;

import com.bajrix.marketplace.dto.*;
import com.bajrix.marketplace.model.*;
import com.bajrix.marketplace.repository.*;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MarketplaceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private SellerListingRepository listingRepository;

    private Long productId;
    private Long approvedSellerId;
    private Long approvedSeller2Id;
    private Long pendingSellerId;

    @BeforeEach
    void setUp() {
        listingRepository.deleteAll();
        sellerRepository.deleteAll();
        productRepository.deleteAll();

        productId = productRepository.save(Product.builder()
                .name("OPC 53 Grade Cement")
                .category("Cement")
                .unit("Bag")
                .description("High strength cement")
                .build()).getId();

        productRepository.save(Product.builder()
                .name("TMT Steel Rod 12mm")
                .category("Steel")
                .unit("Tonne")
                .description("Fe500D TMT bars")
                .build());

        approvedSellerId = sellerRepository.save(Seller.builder()
                .name("Approved Seller")
                .status(SellerStatus.APPROVED)
                .build()).getId();

        approvedSeller2Id = sellerRepository.save(Seller.builder()
                .name("Second Approved Seller")
                .status(SellerStatus.APPROVED)
                .build()).getId();

        pendingSellerId = sellerRepository.save(Seller.builder()
                .name("Pending Seller")
                .status(SellerStatus.PENDING)
                .build()).getId();
    }

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    private SellerListingRequest validRequest() {
        return SellerListingRequest.builder()
                .productId(productId)
                .price(new BigDecimal("99.99"))
                .stockQuantity(100)
                .minimumOrderQuantity(1)
                .build();
    }

    private Long createListingAndReturnId(Long sellerId, SellerListingRequest request) throws Exception {
        String content = mockMvc.perform(post("/api/seller/listings")
                .header("X-Seller-Id", sellerId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(content).get("id").asLong();
    }

    @Test
    void createListing_ApprovedSeller_Success() throws Exception {
        mockMvc.perform(post("/api/seller/listings")
                .header("X-Seller-Id", approvedSellerId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.sellerId").value(approvedSellerId))
                .andExpect(jsonPath("$.sellerName").value("Approved Seller"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createListing_PendingSeller_Forbidden() throws Exception {
        mockMvc.perform(post("/api/seller/listings")
                .header("X-Seller-Id", pendingSellerId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(validRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createListing_Duplicate_Conflict() throws Exception {
        createListingAndReturnId(approvedSellerId, validRequest());

        mockMvc.perform(post("/api/seller/listings")
                .header("X-Seller-Id", approvedSellerId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(validRequest())))
                .andExpect(status().isConflict());
    }

    @Test
    void createListing_InvalidPrice_BadRequest() throws Exception {
        SellerListingRequest request = validRequest();
        request.setPrice(new BigDecimal("-5.00"));

        mockMvc.perform(post("/api/seller/listings")
                .header("X-Seller-Id", approvedSellerId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createListing_InvalidStock_BadRequest() throws Exception {
        SellerListingRequest request = validRequest();
        request.setStockQuantity(-1);

        mockMvc.perform(post("/api/seller/listings")
                .header("X-Seller-Id", approvedSellerId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createListing_InvalidMoq_BadRequest() throws Exception {
        SellerListingRequest request = validRequest();
        request.setMinimumOrderQuantity(0);

        mockMvc.perform(post("/api/seller/listings")
                .header("X-Seller-Id", approvedSellerId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateListing_WrongSeller_Forbidden() throws Exception {
        Long listingId = createListingAndReturnId(approvedSellerId, validRequest());

        SellerListingUpdateRequest updateRequest = SellerListingUpdateRequest.builder()
                .price(new BigDecimal("88.88"))
                .build();

        mockMvc.perform(patch("/api/seller/listings/" + listingId)
                .header("X-Seller-Id", approvedSeller2Id.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateListing_InvalidPrice_BadRequest() throws Exception {
        Long listingId = createListingAndReturnId(approvedSellerId, validRequest());

        SellerListingUpdateRequest updateRequest = SellerListingUpdateRequest.builder()
                .price(new BigDecimal("-10.00"))
                .build();

        mockMvc.perform(patch("/api/seller/listings/" + listingId)
                .header("X-Seller-Id", approvedSellerId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateListing_StaleVersion_Conflict() throws Exception {
        Long listingId = createListingAndReturnId(approvedSellerId, validRequest());

        SellerListingUpdateRequest firstUpdate = SellerListingUpdateRequest.builder()
                .price(new BigDecimal("88.88"))
                .version(0L)
                .build();

        mockMvc.perform(patch("/api/seller/listings/" + listingId)
                .header("X-Seller-Id", approvedSellerId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(firstUpdate)))
                .andExpect(status().isOk());

        SellerListingUpdateRequest staleUpdate = SellerListingUpdateRequest.builder()
                .price(new BigDecimal("77.77"))
                .version(0L)
                .build();

        mockMvc.perform(patch("/api/seller/listings/" + listingId)
                .header("X-Seller-Id", approvedSellerId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(staleUpdate)))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteListing_SoftDelete_Success() throws Exception {
        Long listingId = createListingAndReturnId(approvedSellerId, validRequest());

        mockMvc.perform(delete("/api/seller/listings/" + listingId)
                .header("X-Seller-Id", approvedSellerId.toString()))
                .andExpect(status().isNoContent());

        SellerListing listing = listingRepository.findById(listingId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(ListingStatus.STOPPED, listing.getStatus());
    }

    @Test
    void stoppedListing_NotVisibleToBuyer() throws Exception {
        Long listingId = createListingAndReturnId(approvedSellerId, validRequest());

        mockMvc.perform(delete("/api/seller/listings/" + listingId)
                .header("X-Seller-Id", approvedSellerId.toString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/" + productId + "/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void pendingSellerListing_NotVisibleToBuyer() throws Exception {
        listingRepository.save(SellerListing.builder()
                .seller(sellerRepository.findById(pendingSellerId).orElseThrow())
                .product(productRepository.findById(productId).orElseThrow())
                .price(new BigDecimal("50.00"))
                .stockQuantity(10)
                .minimumOrderQuantity(1)
                .status(ListingStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/products/" + productId + "/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void buyerProductList_ShowsMultipleSellersAndOfferSummary() throws Exception {
        createListingAndReturnId(approvedSellerId, validRequest());

        SellerListingRequest cheaper = validRequest();
        cheaper.setPrice(new BigDecimal("89.99"));
        createListingAndReturnId(approvedSeller2Id, cheaper);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("OPC 53 Grade Cement"))
                .andExpect(jsonPath("$.content[0].lowestPrice").value(89.99))
                .andExpect(jsonPath("$.content[0].activeSellerCount").value(2))
                .andExpect(jsonPath("$.content[1].name").value("TMT Steel Rod 12mm"))
                .andExpect(jsonPath("$.content[1].activeSellerCount").value(0));

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("OPC 53 Grade Cement"))
                .andExpect(jsonPath("$.lowestPrice").value(89.99))
                .andExpect(jsonPath("$.activeSellerCount").value(2));

        mockMvc.perform(get("/api/products/" + productId + "/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].price").value(89.99))
                .andExpect(jsonPath("$.content[0].sellerName").exists());
    }

    @Test
    void buyerProductSearch_ByNameAndCategory() throws Exception {
        mockMvc.perform(get("/api/products").param("search", "cement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("OPC 53 Grade Cement"));

        mockMvc.perform(get("/api/products").param("category", "Steel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("TMT Steel Rod 12mm"));

        mockMvc.perform(get("/api/products").param("search", "cement").param("category", "Steel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        mockMvc.perform(get("/api/products/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("Cement"))
                .andExpect(jsonPath("$[1]").value("Steel"));
    }

    @Test
    void buyerProductList_Pagination() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "1").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void sellerSeesOwnListings_NotOthers() throws Exception {
        createListingAndReturnId(approvedSellerId, validRequest());

        mockMvc.perform(get("/api/seller/listings")
                .header("X-Seller-Id", approvedSellerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].productName").value("OPC 53 Grade Cement"));

        mockMvc.perform(get("/api/seller/listings")
                .header("X-Seller-Id", approvedSeller2Id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void getSellers_ReturnsAllWithStatus() throws Exception {
        mockMvc.perform(get("/api/sellers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void missingResources_Return404() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/products/999999/listings"))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminSeesSellerTotalsAndListingsUnderReview() throws Exception {
        createListingAndReturnId(approvedSellerId, validRequest());

        listingRepository.save(SellerListing.builder()
                .seller(sellerRepository.findById(pendingSellerId).orElseThrow())
                .product(productRepository.findById(productId).orElseThrow())
                .price(new BigDecimal("410.00"))
                .stockQuantity(1000)
                .minimumOrderQuantity(20)
                .status(ListingStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/admin/sellers/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));

        mockMvc.perform(get("/api/admin/listings/under-review/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        mockMvc.perform(get("/api/admin/sellers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Approved Seller"));

        mockMvc.perform(get("/api/admin/sellers/" + approvedSellerId + "/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));

        mockMvc.perform(get("/api/admin/sellers/" + pendingSellerId + "/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].sellerStatus").value("PENDING"));
    }

    @Test
    void adminSeesStoppedListingsOfSeller() throws Exception {
        Long listingId = createListingAndReturnId(approvedSellerId, validRequest());

        mockMvc.perform(delete("/api/seller/listings/" + listingId)
                .header("X-Seller-Id", approvedSellerId.toString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/sellers/" + approvedSellerId + "/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("STOPPED"));
    }

    @Test
    void adminDashboardReturnsSummaryCounts() throws Exception {
        createListingAndReturnId(approvedSellerId, validRequest());

        listingRepository.save(SellerListing.builder()
                .seller(sellerRepository.findById(pendingSellerId).orElseThrow())
                .product(productRepository.findById(productId).orElseThrow())
                .price(new BigDecimal("410.00"))
                .stockQuantity(1000)
                .minimumOrderQuantity(20)
                .status(ListingStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSellers").value(3))
                .andExpect(jsonPath("$.listingsUnderReview").value(1));
    }

    @Test
    void adminSellerListingsHandleEmptyAndUnknownSeller() throws Exception {
        mockMvc.perform(get("/api/admin/sellers/" + approvedSeller2Id + "/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/admin/sellers/999999/listings"))
                .andExpect(status().isNotFound());
    }
}
