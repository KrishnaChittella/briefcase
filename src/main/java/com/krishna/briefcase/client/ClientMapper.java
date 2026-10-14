package com.krishna.briefcase.client;

import com.krishna.briefcase.client.dto.ClientRequest;
import com.krishna.briefcase.client.dto.ClientResponse;
import org.springframework.stereotype.Component;

/**
 * Manual entity<->DTO mapping. Kept deliberately explicit (easy MapStruct swap later).
 */
@Component
public class ClientMapper {

    public Client toEntity(ClientRequest request) {
        Client client = new Client();
        apply(client, request);
        return client;
    }

    public void update(Client client, ClientRequest request) {
        apply(client, request);
    }

    private void apply(Client client, ClientRequest request) {
        client.setName(request.name());
        client.setEmail(request.email());
        client.setPhone(request.phone());
        client.setAddress(request.address());
    }

    public ClientResponse toResponse(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getPhone(),
                client.getAddress(),
                client.getCreatedAt()
        );
    }
}
