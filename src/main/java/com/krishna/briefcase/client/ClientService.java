package com.krishna.briefcase.client;

import com.krishna.briefcase.client.dto.ClientRequest;
import com.krishna.briefcase.client.dto.ClientResponse;
import com.krishna.briefcase.common.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClientService {

    private final ClientRepository repository;
    private final ClientMapper mapper;

    public ClientService(ClientRepository repository, ClientMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public ClientResponse create(ClientRequest request) {
        Client saved = repository.save(mapper.toEntity(request));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> findAll() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ClientResponse getById(Long id) {
        return mapper.toResponse(getEntity(id));
    }

    /** Entity lookup shared with other features (e.g. case creation). */
    @Transactional(readOnly = true)
    public Client getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Client", id));
    }

    public ClientResponse update(Long id, ClientRequest request) {
        Client client = getEntity(id);
        mapper.update(client, request);
        return mapper.toResponse(client);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw ResourceNotFoundException.of("Client", id);
        }
        repository.deleteById(id);
    }
}
