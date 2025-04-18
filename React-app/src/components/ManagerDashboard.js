import React, { useEffect, useState } from 'react';
import axios from 'axios';

const ManagerDashboard = () => {
  const [pendingLeaveRequests, setPendingLeaveRequests] = useState([]);
  const [message, setMessage] = useState('');
  const token = localStorage.getItem('authToken');  // Assuming the token is stored in localStorage

  useEffect(() => {
    fetchPendingLeaveRequests();
  }, []);

  const fetchPendingLeaveRequests = async () => {
    try {
      const response = await axios.get('http://localhost:8080/leave-requests/pending-approval', {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      setPendingLeaveRequests(response.data);
    } catch (error) {
      setMessage(error.response?.data?.message || 'Failed to fetch pending leave requests');
    }
  };

  const handleApprove = async (leaveRequestId) => {
    try {
      await axios.put(
        `http://localhost:8080/leave-requests/${leaveRequestId}/status`,
        { status: 'approved' },
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );
      setMessage('Leave request approved');
      fetchPendingLeaveRequests();  // Refresh the list
    } catch (error) {
      setMessage(error.response?.data?.message || 'Failed to approve leave request');
    }
  };

  const handleReject = async (leaveRequestId) => {
    const rejectionReason = prompt('Enter rejection reason:');
    if (rejectionReason) {
      try {
        await axios.put(
          `http://localhost:8080/leave-requests/${leaveRequestId}/status`,
          { status: 'rejected', comments: rejectionReason },
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
        );
        setMessage('Leave request rejected');
        fetchPendingLeaveRequests();  // Refresh the list
      } catch (error) {
        setMessage(error.response?.data?.message || 'Failed to reject leave request');
      }
    }
  };

  return (
    <div>
      <h2>Pending Leave Requests</h2>
      {message && <p>{message}</p>}
      <table>
        <thead>
          <tr>
            <th>Employee</th>
            <th>Start Date</th>
            <th>End Date</th>
            <th>Reason</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {pendingLeaveRequests.length > 0 ? (
            pendingLeaveRequests.map((request) => (
              <tr key={request.id}>
                <td>{request.employee.firstName} {request.employee.lastName}</td>
                <td>{new Date(request.startDate).toLocaleDateString()}</td>
                <td>{new Date(request.endDate).toLocaleDateString()}</td>
                <td>{request.reason}</td>
                <td>
                  <button onClick={() => handleApprove(request.id)}>Approve</button>
                  <button onClick={() => handleReject(request.id)}>Reject</button>
                </td>
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan="5">No pending leave requests</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
};

export default ManagerDashboard;
