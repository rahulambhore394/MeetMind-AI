import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../api/client';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const savedUser = localStorage.getItem('meetmind_user');
    return savedUser ? JSON.parse(savedUser) : null;
  });
  const [token, setToken] = useState(() => localStorage.getItem('meetmind_token'));
  const [loading, setLoading] = useState(false);

  const login = async (email, password) => {
    setLoading(true);
    try {
      const response = await api.post('/auth/login', { email, password });
      const { accessToken, userId, name, email: userEmail } = response.data;
      const userData = { id: userId, name: name || 'User', email: userEmail || email };

      if (accessToken) {
        localStorage.setItem('meetmind_token', accessToken);
        localStorage.setItem('meetmind_user', JSON.stringify(userData));
        setToken(accessToken);
        setUser(userData);
      }
      return { success: true };
    } catch (error) {
      let message = 'Login failed. Please check credentials.';
      const data = error.response?.data;
      if (data?.validationErrors) {
        message = Object.values(data.validationErrors).join('. ');
      } else if (data?.message) {
        message = data.message;
      }
      return { success: false, error: message };
    } finally {
      setLoading(false);
    }
  };

  const register = async (name, email, password) => {
    setLoading(true);
    try {
      await api.post('/auth/register', { name, email, password });
      // Auto login after successful registration to receive JWT accessToken
      return await login(email, password);
    } catch (error) {
      let message = 'Registration failed. Please check inputs.';
      const data = error.response?.data;
      if (data?.validationErrors) {
        message = Object.values(data.validationErrors).join('. ');
      } else if (data?.message) {
        message = data.message;
      }
      return { success: false, error: message };
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('meetmind_token');
    localStorage.removeItem('meetmind_user');
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, token, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
