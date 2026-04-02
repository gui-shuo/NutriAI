package com.nutriai.repository;

import com.nutriai.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 会员Repository
 */
@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    
    /**
     * 根据用户ID查找会员
     */
    Optional<Member> findByUserId(Long userId);
    
    /**
     * 根据邀请码查找会员
     */
    Optional<Member> findByInvitationCode(String invitationCode);
    
    /**
     * 检查邀请码是否存在
     */
    boolean existsByInvitationCode(String invitationCode);
    
    /**
     * 更新成长值
     */
    @Modifying
    @Query("UPDATE Member m SET m.totalGrowth = m.totalGrowth + :growth, " +
           "m.currentGrowth = m.currentGrowth + :growth WHERE m.id = :memberId")
    void updateGrowth(@Param("memberId") Long memberId, @Param("growth") Integer growth);
    
    /**
     * 更新等级
     */
    @Modifying
    @Query("UPDATE Member m SET m.levelId = :levelId, m.currentGrowth = :currentGrowth " +
           "WHERE m.id = :memberId")
    void updateLevel(@Param("memberId") Long memberId, 
                     @Param("levelId") Long levelId, 
                     @Param("currentGrowth") Integer currentGrowth);
    
    /**
     * 增加邀请人数
     */
    @Modifying
    @Query("UPDATE Member m SET m.invitationCount = m.invitationCount + 1 WHERE m.id = :memberId")
    void incrementInvitationCount(@Param("memberId") Long memberId);

    void deleteByUserId(Long userId);
}
