package com.company.request_approval.repository;

import com.company.request_approval.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByUserId(Long userId);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.user.manager.id = :managerId AND lr.status = 'PENDING'")
    List<LeaveRequest> findPendingRequestsByManagerId(@Param("managerId") Long managerId);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.user.id = :userId AND lr.status = :status")
    List<LeaveRequest> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.startDate <= :endDate AND lr.endDate >= :startDate AND lr.user.id = :userId AND lr.status = 'APPROVED'")
    List<LeaveRequest> findOverlappingLeaveRequests(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.startDate <= :endDate AND lr.endDate >= :startDate AND lr.user.id = :userId AND lr.status = 'APPROVED' AND lr.id != :leaveRequestId")
    List<LeaveRequest> findOverlappingLeaveRequestsExcludingCurrent(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("leaveRequestId") Long leaveRequestId);
    
    int countByUserIdAndStatus(Long userId, String status);
    
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.user.id = :userId AND lr.startDate >= :currentDate AND lr.status = 'APPROVED' ORDER BY lr.startDate ASC")
    List<LeaveRequest> findUpcomingLeaveRequests(@Param("userId") Long userId, @Param("currentDate") LocalDate currentDate);
}