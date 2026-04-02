package com.nutriai.repository;

import com.nutriai.entity.Invitation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 邀请记录Repository
 */
@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    
    /**
     * 根据邀请人ID查询
     */
    Page<Invitation> findByInviterIdOrderByInvitedAtDesc(Long inviterId, Pageable pageable);
    
    /**
     * 根据被邀请人ID查询
     */
    Optional<Invitation> findByInviteeId(Long inviteeId);
    
    /**
     * 根据邀请码查询
     */
    Optional<Invitation> findByInvitationCode(String invitationCode);
    
    /**
     * 统计邀请人的邀请数量
     */
    @Query("SELECT COUNT(i) FROM Invitation i WHERE i.inviterId = :inviterId AND i.status = :status")
    Long countByInviterIdAndStatus(@Param("inviterId") Long inviterId, 
                                    @Param("status") String status);
    
    /**
     * 查询成功邀请的列表
     */
    @Query("SELECT i FROM Invitation i WHERE i.inviterId = :inviterId AND i.status = 'ACCEPTED' " +
           "ORDER BY i.acceptedAt DESC")
    List<Invitation> findAcceptedInvitationsByInviter(@Param("inviterId") Long inviterId);
    
    /**
     * 查询待发放奖励的邀请
     */
    List<Invitation> findByStatusAndIsRewardedFalse(String status);

    void deleteAllByInviterIdOrInviteeId(Long inviterId, Long inviteeId);
}
