package dev.jwt.auth.controller;

import dev.jwt.auth.dto.request.ClientRequest;
import dev.jwt.auth.entity.Client;
import dev.jwt.auth.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clients")
@CrossOrigin(origins = "*")
public class ClientController {

    @Autowired
    private ClientService service;

    @GetMapping
    public List<Client> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Client getById(@PathVariable("id") Long id) {
        return service.getById(id);
    }

    @PostMapping
    public Client create(@RequestBody ClientRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public Client update(@PathVariable("id") Long id, @RequestBody ClientRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        service.delete(id);
    }
}
