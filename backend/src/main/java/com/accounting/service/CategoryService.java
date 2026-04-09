package com.accounting.service;

import com.accounting.dto.CategoryDto;
import com.accounting.dto.CategoryRequest;
import com.accounting.entity.Category;
import com.accounting.exception.BusinessException;
import com.accounting.exception.ResourceNotFoundException;
import com.accounting.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<CategoryDto> findAll(String type) {
        Long userId = userService.getCurrentUserId();
        List<Category> categories;
        
        if (type != null && !type.isEmpty()) {
            // 查询该类型下所有激活的分类（包括父子）
            categories = categoryRepository.findByUserIdAndTypeAndIsActiveTrueOrderBySortOrderAsc(userId, type);
        } else {
            categories = categoryRepository.findByUserId(userId).stream()
                    .filter(c -> c.getIsActive() != null && c.getIsActive())
                    .sorted(Comparator.comparing(c -> c.getSortOrder() != null ? c.getSortOrder() : 0))
                    .collect(Collectors.toList());
        }
        
        // 构建树形结构
        return buildTree(categories);
    }

    /**
     * 构建分类树（最多2层）
     */
    private List<CategoryDto> buildTree(List<Category> categories) {
        Map<Long, CategoryDto> dtoMap = new HashMap<>();
        List<CategoryDto> rootCategories = new ArrayList<>();
        
        // 先转换为DTO
        for (Category cat : categories) {
            dtoMap.put(cat.getId(), convertToDto(cat));
        }
        
        // 构建层级关系
        for (Category cat : categories) {
            CategoryDto dto = dtoMap.get(cat.getId());
            if (cat.getParentId() == null) {
                // 一级分类
                rootCategories.add(dto);
            } else {
                // 二级分类
                CategoryDto parent = dtoMap.get(cat.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(dto);
                    dto.setParentName(parent.getName());
                }
            }
        }
        
        // 按sortOrder排序
        rootCategories.sort(Comparator.comparing(c -> c.getSortOrder() != null ? c.getSortOrder() : 0));
        
        return rootCategories;
    }

    @Transactional
    public CategoryDto create(CategoryRequest request) {
        Long userId = userService.getCurrentUserId();

        // 检查父分类
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findByIdAndUserId(request.getParentId(), userId)
                    .orElseThrow(() -> new BusinessException("父分类不存在"));
            
            // 检查父分类是否已经是二级分类（最多2层）
            if (parent.getParentId() != null) {
                throw new BusinessException("最多只能有2层分类");
            }
        }

        // 获取当前最大排序值
        int maxSort = getMaxSortOrder(userId, request.getType(), request.getParentId());

        Category category = Category.builder()
                .userId(userId)
                .name(request.getName())
                .type(request.getType())
                .icon(request.getIcon())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : maxSort + 1)
                .parentId(request.getParentId())
                .isActive(true)
                .build();

        categoryRepository.save(category);
        return convertToDto(category);
    }

    @Transactional
    public CategoryDto update(Long id, CategoryRequest request) {
        Long userId = userService.getCurrentUserId();
        Category category = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));

        // 检查父分类
        if (request.getParentId() != null) {
            // 不能选择自己作为父分类
            if (request.getParentId().equals(id)) {
                throw new BusinessException("不能选择自己作为父分类");
            }
            
            Category parent = categoryRepository.findByIdAndUserId(request.getParentId(), userId)
                    .orElseThrow(() -> new BusinessException("父分类不存在"));
            
            // 检查父分类是否已经是二级分类
            if (parent.getParentId() != null) {
                throw new BusinessException("最多只能有2层分类");
            }
            
            // 检查是否选择了自己的子分类作为父分类
            if (isChildCategory(id, request.getParentId())) {
                throw new BusinessException("不能选择子分类作为父分类");
            }
        }

        category.setName(request.getName());
        category.setType(request.getType());
        category.setIcon(request.getIcon());
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        category.setParentId(request.getParentId());

        categoryRepository.save(category);
        return convertToDto(category);
    }

    /**
     * 检查targetId是否是categoryId的子分类
     */
    private boolean isChildCategory(Long categoryId, Long targetId) {
        List<Category> children = categoryRepository.findByParentIdAndIsActiveTrueOrderBySortOrderAsc(categoryId);
        for (Category child : children) {
            if (child.getId().equals(targetId)) {
                return true;
            }
        }
        return false;
    }

    @Transactional
    public void delete(Long id) {
        Long userId = userService.getCurrentUserId();
        Category category = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));

        // 检查是否有子分类
        boolean hasChildren = categoryRepository.existsByParentIdAndIsActiveTrue(id);
        if (hasChildren) {
            throw new BusinessException("该分类下有子分类，请先删除子分类");
        }

        category.setIsActive(false);
        categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public Category findEntityById(Long id) {
        Long userId = userService.getCurrentUserId();
        return categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
    }

    /**
     * 更新排序（支持拖拽排序）
     * @param type 分类类型
     * @param parentId 父分类ID（null表示一级分类）
     * @param ids 排序后的ID列表
     */
    @Transactional
    public void updateSortOrder(String type, Long parentId, List<Long> ids) {
        Long userId = userService.getCurrentUserId();
        
        for (int i = 0; i < ids.size(); i++) {
            Category cat = categoryRepository.findByIdAndUserId(ids.get(i), userId)
                    .orElse(null);
            if (cat != null) {
                cat.setSortOrder(i);
                // 确保层级关系正确
                cat.setParentId(parentId);
                categoryRepository.save(cat);
            }
        }
    }

    /**
     * 获取指定类型的最大排序值
     */
    private int getMaxSortOrder(Long userId, String type, Long parentId) {
        List<Category> list;
        if (parentId == null) {
            list = categoryRepository.findByUserIdAndTypeAndParentIdAndIsActiveTrueOrderBySortOrderAsc(
                    userId, type, null);
        } else {
            list = categoryRepository.findByParentIdAndIsActiveTrueOrderBySortOrderAsc(parentId);
        }
        return list.stream()
                .mapToInt(c -> c.getSortOrder() != null ? c.getSortOrder() : 0)
                .max()
                .orElse(0);
    }

    private CategoryDto convertToDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .icon(category.getIcon())
                .sortOrder(category.getSortOrder())
                .parentId(category.getParentId())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                .build();
    }
}
