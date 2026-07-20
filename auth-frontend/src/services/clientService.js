import api from "../api/axios";

export async function getClients() {
  const { data } = await api.get("/clients");
  return data;
}

export async function getClientById(id) {
  const { data } = await api.get(`/clients/${id}`);
  return data;
}

export async function createClient(client) {
  const { data } = await api.post("/clients", client);
  return data;
}

export async function updateClient(id, client) {
  const { data } = await api.put(`/clients/${id}`, client);
  return data;
}

export async function deleteClient(id) {
  await api.delete(`/clients/${id}`);
}