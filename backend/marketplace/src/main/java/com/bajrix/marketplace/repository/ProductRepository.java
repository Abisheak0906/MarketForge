package com.bajrix.marketplace.repository;

import com.bajrix.marketplace.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
        SELECT p
        FROM Product p
        WHERE lower(p.name) LIKE lower(:namePattern)
          AND (:hasCategory = false OR p.category = :category)
        """)
    Page<Product> search(
        @Param("namePattern") String namePattern,
        @Param("hasCategory") boolean hasCategory,
        @Param("category") String category,
        Pageable pageable
    );

    @Query("SELECT DISTINCT p.category FROM Product p ORDER BY p.category")
    List<String> findDistinctCategories();
}
