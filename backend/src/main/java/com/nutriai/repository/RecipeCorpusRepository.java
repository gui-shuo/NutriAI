package com.nutriai.repository;

import com.nutriai.entity.RecipeCorpus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeCorpusRepository extends JpaRepository<RecipeCorpus, Long> {

    @Query(value = "SELECT * FROM recipe_corpus WHERE MATCH(name, description) AGAINST(:keyword IN BOOLEAN MODE) LIMIT :limit",
           nativeQuery = true)
    List<RecipeCorpus> fullTextSearch(@Param("keyword") String keyword, @Param("limit") int limit);

    @Query(value = "SELECT * FROM recipe_corpus WHERE name LIKE CONCAT('%', :keyword, '%') LIMIT :limit",
           nativeQuery = true)
    List<RecipeCorpus> searchByName(@Param("keyword") String keyword, @Param("limit") int limit);

    // Browsing queries with capped count to avoid slow COUNT(*) on 1.5M+ rows.
    // Data queries use ORDER BY id for index-based pagination.
    // Count queries return a capped estimate to keep pagination fast.

    @Query(value = "SELECT * FROM recipe_corpus ORDER BY id",
           countQuery = "SELECT 10000",
           nativeQuery = true)
    Page<RecipeCorpus> findAllCapped(Pageable pageable);

    @Query(value = "SELECT * FROM recipe_corpus WHERE category = :category ORDER BY id",
           countQuery = "SELECT LEAST(10000, (SELECT COUNT(*) FROM recipe_corpus WHERE category = :category))",
           nativeQuery = true)
    Page<RecipeCorpus> findByCategoryCapped(@Param("category") String category, Pageable pageable);

    @Query(value = "SELECT * FROM recipe_corpus WHERE name LIKE CONCAT('%', :keyword, '%') OR dish LIKE CONCAT('%', :keyword, '%') ORDER BY id",
           countQuery = "SELECT LEAST(10000, (SELECT COUNT(*) FROM (SELECT 1 FROM recipe_corpus WHERE name LIKE CONCAT('%', :keyword, '%') OR dish LIKE CONCAT('%', :keyword, '%') LIMIT 10000) t))",
           nativeQuery = true)
    Page<RecipeCorpus> searchPagedCapped(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT DISTINCT category FROM recipe_corpus", nativeQuery = true)
    List<String> findAllCategories();

    @Query(value = "SELECT COUNT(*) FROM recipe_corpus", nativeQuery = true)
    long countAll();
}
