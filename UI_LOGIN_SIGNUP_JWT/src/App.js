import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import AuthForm from './components/AuthForm';
import EmployeeDashboard from './components/EmployeeDashboard';
import ManagerDashboard from './components/ManagerDashboard';
import PrivateRoute from './components/PrivateRoute';  // Import the PrivateRoute component
import './App.css';

function App() {
  return (
    <Router>
      <div className="App">
        <Routes>
          <Route path="/" element={<AuthForm />} />
          <Route path="/login" element={<AuthForm />} />
          <Route path="/signup" element={<AuthForm />} />

          {/* Use PrivateRoute to protect the dashboards */}
          {/* Removed ARIA roles and ensured correct structure */}
          <Route path="/dashboard" element={<PrivateRoute role="EMPLOYEE" element={<EmployeeDashboard />} />} />
          <Route path="/manager-dashboard" element={<PrivateRoute role="MANAGER" element={<ManagerDashboard />} />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
