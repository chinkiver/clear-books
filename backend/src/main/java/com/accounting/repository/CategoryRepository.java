package com.accounting.repository;

import com.accounting.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserIdAndTypeAndIsActiveTrueOrderBySortOrderAsc(Long userId, String type);

    List<Category> findByUserIdAndTypeAndParentIdAndIsActiveTrueOrderBySortOrderAsc(Long userId, String type, Long parentId);

    List<Category> findByUserIdAndType(Long userId, String type);

    List<Category> findByUserId(Long userId);

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    // 查询是否有子分类
    boolean existsByParentIdAndIsActiveTrue(Long parentId);

    // 查询子分类
    List<Category> findByParentIdAndIsActiveTrueOrderBySortOrderAsc(Long parentId);
}
