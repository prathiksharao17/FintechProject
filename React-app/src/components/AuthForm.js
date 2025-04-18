import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './AuthForm.css';

const AuthForm = () => {
  const [isSignup, setIsSignup] = useState(true);
  const [isLoading, setIsLoading] = useState(true);
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    role: 'EMPLOYEE',
    firstName: '',
    lastName: '',
    department: '',
    position: '',
    phoneNumber: ''
  });
  const [message, setMessage] = useState('');

  useEffect(() => {
    // Simulate checking if everything is loaded
    const timer = setTimeout(() => {
      setIsLoading(false);
    }, 500);
    
    return () => clearTimeout(timer);
  }, []);

  const handleChange = (e) => {
    setFormData(prev => ({
      ...prev,
      [e.target.name]: e.target.value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage('');
    
    try {
      setIsLoading(true);
      const url = isSignup
        ? 'http://localhost:8080/api/auth/signup'
        : 'http://localhost:8080/api/auth/login';

      const payload = isSignup
        ? formData
        : {
            email: formData.email,
            password: formData.password
          };

      const res = await axios.post(url, payload);

      if (!isSignup) {
        const { token, role } = res.data;

        if (token) {
          localStorage.setItem('authToken', token);
          localStorage.setItem('userRole', role);
          setMessage('Login successful!');

          if (role === 'MANAGER') {
            window.location.href = '/manager-dashboard';
          } else {
            window.location.href = '/dashboard';
          }
        } else {
          setMessage('Login failed: No token received.');
        }
      } else {
        setMessage(res.data.message || 'Signup successful!');
        // Reset form after successful signup
        if (!res.data.error) {
          setFormData({
            email: '',
            password: '',
            role: 'EMPLOYEE',
            firstName: '',
            lastName: '',
            department: '',
            position: '',
            phoneNumber: ''
          });
        }
      }
    } catch (err) {
      console.error('Auth error:', err);
      setMessage(err.response?.data?.message || 'Something went wrong');
    } finally {
      setIsLoading(false);
    }
  };

  const toggleMode = () => {
    setIsSignup(!isSignup);
    setMessage('');
  };

  if (isLoading && !message) {
    return <div className="loading-container">Loading form...</div>;
  }

  return (
    <div className="auth-container">
      <h2>{isSignup ? 'Sign Up' : 'Login'}</h2>
      
      {message && <div className={message.includes('successful') ? 'success-message' : 'error-message'}>{message}</div>}
      
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <input
            type="email"
            name="email"
            placeholder="Email"
            value={formData.email}
            onChange={handleChange}
            required
            className="form-input"
          />
        </div>
        
        <div className="form-group">
          <input
            type="password"
            name="password"
            placeholder="Password"
            value={formData.password}
            onChange={handleChange}
            required
            className="form-input"
          />
        </div>

        {isSignup && (
          <>
            <div className="form-group">
              <input
                type="text"
                name="firstName"
                placeholder="First Name"
                value={formData.firstName}
                onChange={handleChange}
                required
                className="form-input"
              />
            </div>
            
            <div className="form-group">
              <input
                type="text"
                name="lastName"
                placeholder="Last Name"
                value={formData.lastName}
                onChange={handleChange}
                required
                className="form-input"
              />
            </div>
            
            <div className="form-group">
              <input
                type="text"
                name="phoneNumber"
                placeholder="Phone Number"
                value={formData.phoneNumber}
                onChange={handleChange}
                className="form-input"
              />
            </div>
            
            <div className="form-group">
              <input
                type="text"
                name="department"
                placeholder="Department"
                value={formData.department}
                onChange={handleChange}
                className="form-input"
              />
            </div>
            
            <div className="form-group">
              <input
                type="text"
                name="position"
                placeholder="Position"
                value={formData.position}
                onChange={handleChange}
                className="form-input"
              />
            </div>
            
            <div className="form-group">
              <select 
                name="role" 
                onChange={handleChange} 
                value={formData.role}
                className="form-select"
              >
                <option value="MANAGER">Manager</option>
                <option value="EMPLOYEE">Employee</option>
              </select>
            </div>
          </>
        )}

        <button type="submit" className="form-button" disabled={isLoading}>
          {isLoading ? 'Processing...' : (isSignup ? 'Sign Up' : 'Login')}
        </button>
      </form>

      <div className="form-footer">
        <button onClick={toggleMode} className="toggle-btn">
          {isSignup ? 'Already a user? Login' : 'New here? Sign Up'}
        </button>
      </div>
    </div>
  );
};

export default AuthForm;