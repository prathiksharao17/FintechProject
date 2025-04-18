import React from 'react';
import { Route, Navigate } from 'react-router-dom';

const PrivateRoute = ({ element, role, ...rest }) => {
  const userRole = localStorage.getItem('userRole');

  if (!userRole || userRole !== role) {
    // Redirect them to the login page if they don't have the correct role
    return <Navigate to="/" />;
  }

  return <Route {...rest} element={element} />;
};

export default PrivateRoute;
