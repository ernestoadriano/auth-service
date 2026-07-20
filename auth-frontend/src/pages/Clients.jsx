import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import * as clientService from "../services/clientService";
import "./Client.css"

export default function Clients() {
  const [clients, setClients] = useState([]);
  const [loading, setLoading] = useState(true);

  async function loadClients() {
    setLoading(true);
    const data = await clientService.getClients();
    setClients(data);
    setLoading(false);
  }

  useEffect(() => {
    loadClients();
  }, []);

  async function handleDelete(id) {
    if (!confirm("Tens a certeza que queres apagar este cliente?")) return;
    await clientService.deleteClient(id);
    loadClients();
  }

  if (loading) return <p>A carregar...</p>;

  return (
    <div>
      <h2>Clientes</h2>
      <Link to="/clients/new">+ Novo Cliente</Link>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Nome</th>
            <th>Ações</th>
          </tr>
        </thead>
        <tbody>
          {clients.map((client) => (
            <tr key={client.id}>
              <td>{client.id}</td>
              <td>{client.name}</td>
              <td>
                <Link to={`/clients/${client.id}/edit`}>Editar</Link>{" "}
                <button onClick={() => handleDelete(client.id)}>Apagar</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}