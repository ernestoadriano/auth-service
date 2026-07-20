import { createContext, useContext, useState } from "react";
import * as authService from "../services/authService";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [isAuth, setIsAuth] = useState(authService.isAuthenticated());

  async function login(username, password) {
    await authService.login(username, password);
    setIsAuth(true);
  }

  async function register(username, email, password, role) {
    await authService.register(username, email, password, role);
    setIsAuth(true);
  }

  async function logout() {
    await authService.logout();
    setIsAuth(false);
  }

  return (
    <AuthContext.Provider value={{ isAuth, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}