import React, { useState } from 'react';
import axios from 'axios';
import './AuthForm.css';

const AuthForm = () => {
  const [isSignup, setIsSignup] = useState(true);
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

  const handleChange = (e) => {
    setFormData(prev => ({
      ...prev,
      [e.target.name]: e.target.value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const url = isSignup
      ? 'http://localhost:8080/api/auth/signup'
      : 'http://localhost:8080/api/auth/login';

    try {
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
      }
    } catch (err) {
      setMessage(err.response?.data?.message || 'Something went wrong');
    }
  };

  return (
    <div className="auth-container">
      <h2>{isSignup ? 'Sign Up' : 'Login'}</h2>
      <form onSubmit={handleSubmit}>
        <input
          type="email"
          name="email"
          placeholder="Email"
          onChange={handleChange}
          required
        />
        <input
          type="password"
          name="password"
          placeholder="Password"
          onChange={handleChange}
          required
        />

        {isSignup && (
          <>
            <input
              type="text"
              name="firstName"
              placeholder="First Name"
              onChange={handleChange}
              required
            />
            <input
              type="text"
              name="lastName"
              placeholder="Last Name"
              onChange={handleChange}
              required
            />
            <input
              type="text"
              name="phoneNumber"
              placeholder="Phone Number"
              onChange={handleChange}
            />
            <input
              type="text"
              name="department"
              placeholder="Department"
              onChange={handleChange}
            />
            <input
              type="text"
              name="position"
              placeholder="Position"
              onChange={handleChange}
            />
            <select name="role" onChange={handleChange} defaultValue="EMPLOYEE">
              <option value="MANAGER">Manager</option>
              <option value="EMPLOYEE">Employee</option>
            </select>
          </>
        )}

        <button type="submit">
          {isSignup ? 'Sign Up' : 'Login'}
        </button>
      </form>

      <p>{message}</p>
      <button onClick={() => setIsSignup(!isSignup)}>
        {isSignup ? 'Already a user? Login' : 'New here? Sign Up'}
      </button>
    </div>
  );
};

export default AuthForm;
