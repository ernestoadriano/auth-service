import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import * as clientService from "../services/clientService";

export default function ClientForm() {
  const { id } = useParams();
  const isEditing = !!id;
  const [name, setName] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    if (isEditing) {
      clientService.getClientById(id).then((client) => setName(client.name));
    }
  }, [id, isEditing]);

  async function handleSubmit(e) {
    e.preventDefault();
    if (isEditing) {
      await clientService.updateClient(id, { name });
    } else {
      await clientService.createClient({ name });
    }
    navigate("/clients");
  }

  return (
    <form onSubmit={handleSubmit}>
      <h2>{isEditing ? "Editar Cliente" : "Novo Cliente"}</h2>
      <input
        placeholder="Nome"
        value={name}
        onChange={(e) => setName(e.target.value)}
        required
      />
      <button type="submit">Guardar</button>
    </form>
  );
}