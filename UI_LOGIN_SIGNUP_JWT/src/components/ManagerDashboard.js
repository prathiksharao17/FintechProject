import React, { useEffect, useState } from 'react';
import axios from 'axios';

const ManagerDashboard = () => {
  const [leaveRequests, setLeaveRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Fetch the leave requests pending approval when the component mounts
  useEffect(() => {
    const fetchLeaveRequests = async () => {
      try {
        const response = await axios.get('/leave-requests/pending-approval', {
          headers: {
            Authorization: `Bearer ${localStorage.getItem('token')}`, // Make sure to set the token in localStorage after login
          },
        });
        setLeaveRequests(response.data);
        setLoading(false);
      } catch (err) {
        setError('Error fetching leave requests');
        setLoading(false);
      }
    };

    fetchLeaveRequests();
  }, []);

  // Function to approve/reject a leave request
  const updateRequestStatus = async (id, status) => {
    try {
      await axios.put(`/leave-requests/${id}/status`, 
        { status: status, comments: '' }, // Add comment if needed
        {
          headers: {
            Authorization: `Bearer ${localStorage.getItem('token')}`,
          },
        }
      );
      // After updating, refetch the leave requests
      setLeaveRequests(leaveRequests.filter(request => request.id !== id));
    } catch (err) {
      setError('Error updating leave request status');
    }
  };

  // Render the leave requests
  return (
    <div className="manager-dashboard">
      <h1>Manager Dashboard</h1>
      {loading ? (
        <p>Loading...</p>
      ) : error ? (
        <p>{error}</p>
      ) : (
        <div>
          <h2>Pending Leave Requests</h2>
          <ul>
            {leaveRequests.map((request) => (
              <li key={request.id}>
                <div>
                  <p><strong>Employee:</strong> {request.employeeName}</p>
                  <p><strong>Leave Dates:</strong> {request.startDate} to {request.endDate}</p>
                  <p><strong>Reason:</strong> {request.reason}</p>
                  <button onClick={() => updateRequestStatus(request.id, 'APPROVED')}>Approve</button>
                  <button onClick={() => updateRequestStatus(request.id, 'REJECTED')}>Reject</button>
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};

export default ManagerDashboard;
