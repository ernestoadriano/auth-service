import api from "../api/axios";

export async function login(username, password) {
  const { data } = await api.post("/auth/login", { username, password });
  localStorage.setItem("accessToken", data.accessToken);
  localStorage.setItem("refreshToken", data.refreshToken);
  return data;
}

export async function register(username, email, password) {
  const { data } = await api.post("/auth/register", { username, email, password });
  localStorage.setItem("accessToken", data.accessToken);
  localStorage.setItem("refreshToken", data.refreshToken);
  return data;
}

export async function logout() {
  const refreshToken = localStorage.getItem("refreshToken");
  try {
    await api.post("/auth/logout", { refreshToken });
  } finally {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
  }
}

export function isAuthenticated() {
  return !!localStorage.getItem("accessToken");
}