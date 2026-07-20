package dev.jwt.auth.service;

import dev.jwt.auth.dto.request.ClientRequest;
import dev.jwt.auth.entity.Client;
import dev.jwt.auth.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    public List<Client> getAll() {
        return clientRepository.findAll();
    }

    public Client getById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));
    }

    public Client create(ClientRequest request) {
        Client client = new Client();
        client.setName(request.name());
        return clientRepository.save(client);
    }

    public Client update(Long id, ClientRequest request) {
        Client client = getById(id);
        if (request.name() != null) {
            client.setName(request.name());
        }

        return clientRepository.save(client);
    }

    public void delete(Long id) {
        Client client = getById(id);
        clientRepository.delete(client);
    }
}
