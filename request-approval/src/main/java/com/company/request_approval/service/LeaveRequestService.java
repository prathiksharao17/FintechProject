package com.company.request_approval.service;

import com.company.request_approval.dto.LeaveRequestDto;
import com.company.request_approval.model.LeaveRequest;
import com.company.request_approval.model.User;
import com.company.request_approval.model.UserDetail;
import com.company.request_approval.repository.LeaveRequestRepository;
import com.company.request_approval.repository.UserDetailRepository;
import com.company.request_approval.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final UserDetailRepository userDetailRepository;
    private final UserService userService;

    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository,
                               UserRepository userRepository,
                               UserDetailRepository userDetailRepository,
                               UserService userService) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.userRepository = userRepository;
        this.userDetailRepository = userDetailRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> getAllLeaveRequests() {
        return leaveRequestRepository.findAll().stream()
                .map(this::mapToLeaveRequestDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> getLeaveRequestsByUserId(Long userId) {
        return leaveRequestRepository.findByUserId(userId).stream()
                .map(this::mapToLeaveRequestDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> getLeaveRequestsByUserIdAndStatus(Long userId, String status) {
        return leaveRequestRepository.findByUserIdAndStatus(userId, status).stream()
                .map(this::mapToLeaveRequestDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> getPendingRequestsByManagerId(Long managerId) {
        return leaveRequestRepository.findPendingRequestsByManagerId(managerId).stream()
                .map(this::mapToLeaveRequestDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LeaveRequestDto getLeaveRequestById(Long id) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found with id: " + id));
        return mapToLeaveRequestDto(leaveRequest);
    }

    @Transactional
    public LeaveRequestDto createLeaveRequest(LeaveRequestDto leaveRequestDto) {
        User user = userRepository.findById(leaveRequestDto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + leaveRequestDto.getUserId()));
        
        // Calculate duration in days
        int durationDays = calculateDurationInDays(leaveRequestDto.getStartDate(), leaveRequestDto.getEndDate());
        
        // Check for overlapping leave requests
        List<LeaveRequest> overlappingRequests = leaveRequestRepository.findOverlappingLeaveRequests(
                user.getId(), leaveRequestDto.getStartDate(), leaveRequestDto.getEndDate());
        
        if (!overlappingRequests.isEmpty()) {
            throw new RuntimeException("You already have an approved leave request for this period");
        }
        
        // Check if user has enough leave days left
        if ("ANNUAL".equals(leaveRequestDto.getLeaveType())) {
            UserDetail userDetail = userDetailRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("User detail not found for user id: " + user.getId()));
            
            if (userDetail.getRemainingLeaveAllowance() < durationDays) {
                throw new RuntimeException("Not enough leave days available. You have " + 
                        userDetail.getRemainingLeaveAllowance() + " days left, but requested " + durationDays + " days");
            }
        }
        
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setUser(user);
        leaveRequest.setStartDate(leaveRequestDto.getStartDate());
        leaveRequest.setEndDate(leaveRequestDto.getEndDate());
        leaveRequest.setDurationDays(durationDays);
        leaveRequest.setLeaveType(leaveRequestDto.getLeaveType());
        leaveRequest.setReason(leaveRequestDto.getReason());
        leaveRequest.setStatus("PENDING");
        
        LeaveRequest savedLeaveRequest = leaveRequestRepository.save(leaveRequest);
        return mapToLeaveRequestDto(savedLeaveRequest);
    }

    @Transactional
    public LeaveRequestDto updateLeaveRequest(LeaveRequestDto leaveRequestDto) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestDto.getId())
                .orElseThrow(() -> new RuntimeException("Leave request not found with id: " + leaveRequestDto.getId()));
        
        // Check if the request can be updated (only PENDING requests can be updated)
        if (!"PENDING".equals(leaveRequest.getStatus())) {
            throw new RuntimeException("Only pending requests can be updated");
        }
        
        // Calculate new duration
        int durationDays = calculateDurationInDays(leaveRequestDto.getStartDate(), leaveRequestDto.getEndDate());
        
        // Check for overlapping requests (excluding the current one)
        List<LeaveRequest> overlappingRequests = leaveRequestRepository.findOverlappingLeaveRequestsExcludingCurrent(
                leaveRequestDto.getUserId(), leaveRequestDto.getStartDate(), leaveRequestDto.getEndDate(), leaveRequestDto.getId());
        
        if (!overlappingRequests.isEmpty()) {
            throw new RuntimeException("You already have an approved leave request for this period");
        }
        
        // Check if user has enough leave days left if it's an annual leave
        if ("ANNUAL".equals(leaveRequestDto.getLeaveType())) {
            UserDetail userDetail = userDetailRepository.findByUserId(leaveRequestDto.getUserId())
                    .orElseThrow(() -> new RuntimeException("User detail not found for user id: " + leaveRequestDto.getUserId()));
            
            if (userDetail.getRemainingLeaveAllowance() < durationDays) {
                throw new RuntimeException("Not enough leave days available. You have " + 
                        userDetail.getRemainingLeaveAllowance() + " days left, but requested " + durationDays + " days");
            }
        }
        
        // Update fields
        leaveRequest.setStartDate(leaveRequestDto.getStartDate());
        leaveRequest.setEndDate(leaveRequestDto.getEndDate());
        leaveRequest.setDurationDays(durationDays);
        leaveRequest.setLeaveType(leaveRequestDto.getLeaveType());
        leaveRequest.setReason(leaveRequestDto.getReason());
        
        LeaveRequest updatedLeaveRequest = leaveRequestRepository.save(leaveRequest);
        return mapToLeaveRequestDto(updatedLeaveRequest);
    }

    @Transactional
    public LeaveRequestDto updateLeaveRequestStatus(Long id, String status, String rejectionReason, Long managerId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found with id: " + id));
        
        // Verify manager has permission to update this request
        // This would depend on your data model - implement as needed
        
        String previousStatus = leaveRequest.getStatus();
        leaveRequest.setStatus(status);
        
        if ("REJECTED".equals(status)) {
            leaveRequest.setRejectionReason(rejectionReason);
        }
        
        // If request is being approved and is for annual leave, update the remaining leave allowance
        if ("APPROVED".equals(status) && !"APPROVED".equals(previousStatus) && "ANNUAL".equals(leaveRequest.getLeaveType())) {
            userService.updateLeaveAllowance(leaveRequest.getUser().getId(), leaveRequest.getDurationDays());
        }
        
        // If request was approved but is now being rejected or canceled, refund the leave days
        if (("REJECTED".equals(status) || "CANCELED".equals(status)) && "APPROVED".equals(previousStatus) && "ANNUAL".equals(leaveRequest.getLeaveType())) {
            UserDetail userDetail = userDetailRepository.findByUserId(leaveRequest.getUser().getId())
                    .orElseThrow(() -> new RuntimeException("User detail not found for user id: " + leaveRequest.getUser().getId()));
            
            userDetail.setRemainingLeaveAllowance(userDetail.getRemainingLeaveAllowance() + leaveRequest.getDurationDays());
            userDetailRepository.save(userDetail);
        }
        
        LeaveRequest updatedLeaveRequest = leaveRequestRepository.save(leaveRequest);
        return mapToLeaveRequestDto(updatedLeaveRequest);
    }

    @Transactional
    public boolean deleteLeaveRequest(Long id) {
        try {
            LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Leave request not found with id: " + id));
            
            // If request was approved and is being deleted, refund the leave days
            if ("APPROVED".equals(leaveRequest.getStatus()) && "ANNUAL".equals(leaveRequest.getLeaveType())) {
                UserDetail userDetail = userDetailRepository.findByUserId(leaveRequest.getUser().getId())
                        .orElseThrow(() -> new RuntimeException("User detail not found for user id: " + leaveRequest.getUser().getId()));
                
                userDetail.setRemainingLeaveAllowance(userDetail.getRemainingLeaveAllowance() + leaveRequest.getDurationDays());
                userDetailRepository.save(userDetail);
            }
            
            leaveRequestRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLeaveRequestSummaryForUser(Long userId) {
        Map<String, Object> summary = new HashMap<>();
        
        // Get user details to check leave allowance
        UserDetail userDetail = userDetailRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User detail not found for user id: " + userId));
        
        // Count approved, pending, and rejected requests
        int approvedCount = leaveRequestRepository.countByUserIdAndStatus(userId, "APPROVED");
        int pendingCount = leaveRequestRepository.countByUserIdAndStatus(userId, "PENDING");
        int rejectedCount = leaveRequestRepository.countByUserIdAndStatus(userId, "REJECTED");
        
        // Get upcoming leave requests
        List<LeaveRequestDto> upcomingLeaves = leaveRequestRepository.findUpcomingLeaveRequests(userId, LocalDate.now())
                .stream()
                .map(this::mapToLeaveRequestDto)
                .collect(Collectors.toList());
        
        summary.put("remainingLeaveAllowance", userDetail.getRemainingLeaveAllowance());
        summary.put("approvedRequestsCount", approvedCount);
        summary.put("pendingRequestsCount", pendingCount);
        summary.put("rejectedRequestsCount", rejectedCount);
        summary.put("upcomingLeaves", upcomingLeaves);
        
        return summary;
    }

    private int calculateDurationInDays(LocalDate startDate, LocalDate endDate) {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    private LeaveRequestDto mapToLeaveRequestDto(LeaveRequest leaveRequest) {
        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setId(leaveRequest.getId());
        dto.setUserId(leaveRequest.getUser().getId());
        
        UserDetail userDetail = userDetailRepository.findByUserId(leaveRequest.getUser().getId())
                .orElse(null);
        
        if (userDetail != null) {
            dto.setUserName(userDetail.getFirstName() + " " + userDetail.getLastName());
            dto.setDepartment(userDetail.getDepartment());
            dto.setPosition(userDetail.getPosition());
        }
        
        dto.setStartDate(leaveRequest.getStartDate());
        dto.setEndDate(leaveRequest.getEndDate());
        dto.setDurationDays(leaveRequest.getDurationDays());
        dto.setLeaveType(leaveRequest.getLeaveType());
        dto.setReason(leaveRequest.getReason());
        dto.setStatus(leaveRequest.getStatus());
        dto.setRejectionReason(leaveRequest.getRejectionReason());
        
        return dto;
    }
}