package com.nutriai.repository;

import com.nutriai.entity.MemberLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 会员等级Repository
 */
@Repository
public interface MemberLevelRepository extends JpaRepository<MemberLevel, Long> {
    
    /**
     * 根据等级代码查找
     */
    Optional<MemberLevel> findByLevelCode(String levelCode);
    
    /**
     * 根据等级顺序查找
     */
    Optional<MemberLevel> findByLevelOrder(Integer levelOrder);
    
    /**
     * 查找所需成长值小于等于指定值的最高等级
     */
    Optional<MemberLevel> findFirstByGrowthRequiredLessThanEqualOrderByGrowthRequiredDesc(Integer growth);
    
    /**
     * 按等级顺序升序查询所有等级
     */
    java.util.List<MemberLevel> findAllByOrderByLevelOrderAsc();
}
