// Utility function to get the stored JWT token
export const getAuthToken = () => {
  return localStorage.getItem('authToken');
};

// Utility function to remove JWT token
export const removeAuthToken = () => {
  localStorage.removeItem('authToken');
};
