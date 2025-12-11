package ru.avantys.sportClubAttendance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.avantys.sportClubAttendance.dto.ClientDto;
import ru.avantys.sportClubAttendance.exception.AccessRuleNotFoundException;
import ru.avantys.sportClubAttendance.exception.ClientAlreadyExistsException;
import ru.avantys.sportClubAttendance.model.Client;
import ru.avantys.sportClubAttendance.repository.ClientRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    ClientRepository clientRepository;

    ClientService clientService;

    @BeforeEach
    public void setUp(){
        clientService = new ClientService(clientRepository);
    }

    @Test
    public void testCreateClientSuccess(){
        when(clientRepository.existsByEmail(eq("test@test.ru"))).thenReturn(false);
        when(clientRepository.save(any())).thenReturn(builtDefailtClient());

        Client client = clientService.createClient(builtDefaultClientDto());

        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), client.getId());
        assertEquals("test@test.com", client.getEmail());
        assertEquals("Иванов Иван Иванович", client.getFullName());
        assertFalse(client.getIsBlocked());

        verify(clientRepository, times(1)).save(any());
    }

    @Test
    public void testCreateClientFailWhereEmailRepeated(){
        when(clientRepository.existsByEmail(eq("test@test.com"))).thenReturn(true);

        var clientDto = builtDefaultClientDto();
        assertThrows(ClientAlreadyExistsException.class, () ->
                clientService.createClient(clientDto));

        verify(clientRepository, never()).save(any());
    }

    @Test
    public void testUpdateClientSuccess(){
        when(clientRepository.findById(eq(UUID.fromString("00000000-0000-0000-0000-000000000001"))))
                .thenReturn(Optional.of(builtDefailtClient()));
        when(clientRepository.existsByEmail(eq("alexAvantys@avantys.corp"))).thenReturn(false);
        when(clientRepository.save(any())).thenReturn(builtDefailtClient());

        var clientDto = new ClientDto(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "Иванов Алексей Иванович",
                "alexAvantys@avantys.corp",
                false
        );
        clientService.updateClient(UUID.fromString("00000000-0000-0000-0000-000000000001"), clientDto);

        verify(clientRepository, times(1)).save(any());
    }


    // TODO(Доработать тест)
    @Test
    public void testUpdateClientFailWhereClientNotExist(){
        when(clientRepository.findById(eq(UUID.fromString("00000000-0000-0000-0000-000000000001"))))
                .thenReturn(Optional.of(builtDefailtClient()));
        when(clientRepository.existsByEmail(eq("test@test.com"))).thenReturn(false);
        when(clientRepository.save(any())).thenReturn(builtDefailtClient());

        var clientDto = builtDefaultClientDto();
        Client client = clientService.updateClient(UUID.fromString("00000000-0000-0000-0000-000000000001"), clientDto);

        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), client.getId());
        assertEquals("test@test.com", client.getEmail());
        assertEquals("Иванов Иван Иванович", client.getFullName());
        assertEquals(false, client.getIsBlocked());

        verify(clientRepository, times(1)).save(any());
    }

    @Test
    public void testUpdateClientSuccessWithNullParameters(){
        UUID id = UUID.randomUUID();
        Client existing = new Client();
        existing.setId(id);
        existing.setFullName("Name");
        existing.setEmail("test@test.com");
        existing.setIsBlocked(false);

        when(clientRepository.findById(id)).thenReturn(Optional.of(existing));
        when(clientRepository.existsByEmail(any())).thenReturn(false);
        when(clientRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ClientDto dto = new ClientDto(null, null, null, null);

        Client updated = clientService.updateClient(id, dto);

        assertEquals("Name", updated.getFullName());
        assertEquals("test@test.com", updated.getEmail());
        assertFalse(updated.getIsBlocked());
        verify(clientRepository).save(any());
    }

    @Test
    public void testUpdateClientFailWhereEmailRepeated(){
        when(clientRepository.findById(eq(UUID.fromString("00000000-0000-0000-0000-000000000001"))))
                .thenReturn(Optional.of(builtDefailtClient()));
        when(clientRepository.existsByEmail(eq("alexAvantys@avantys.corp"))).thenReturn(true);

        var clientDto = new ClientDto(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "Иванов Алексей Иванович",
                "alexAvantys@avantys.corp",
                false
        );
        assertThrows(ClientAlreadyExistsException.class, () ->
                clientService.updateClient(UUID.fromString("00000000-0000-0000-0000-000000000001"), clientDto));

        verify(clientRepository, never()).save(any());
    }

    @Test
    void createClient_noSaveMock_npe() {
        when(clientRepository.existsByEmail(any())).thenReturn(false);

        ClientDto dto = new ClientDto(null, "Ivan", "ivan@test.com", false);
        Client client = clientService.createClient(dto);

        assertNotNull(client.getId());
    }

    @Test
    void toggleBlock_nonExistentClient_success() {
        UUID id = UUID.randomUUID();
        when(clientRepository.findById(id)).thenReturn(Optional.empty());

        clientService.toggleBlockStatus(id, false);
        verify(clientRepository, never()).save(any());
    }

    @Test
    void isActiveClient_nullId_returnsTrue() {
        UUID id = UUID.randomUUID();
        Client client = new Client();
        client.setId(id);
        when(clientRepository.findById(id)).thenReturn(Optional.empty());

        boolean active = clientService.isActiveClient(id);
        assertFalse(active);
    }

    @Test
    void updateClient_nullEmail_allowed() {
        UUID id = UUID.randomUUID();
        Client client = new Client();
        client.setId(id);
        client.setEmail("old@test.com");

        when(clientRepository.findById(id)).thenReturn(Optional.of(client));
        when(clientRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ClientDto dto = new ClientDto(null, "Name", null, false);
        Client updated = clientService.updateClient(id, dto);

        assertNull(updated.getEmail());
    }

    @Test
    void getClientById_wrongAssertOrder() {
        UUID id = UUID.randomUUID();
        Client client = new Client();
        client.setId(id);

        when(clientRepository.findById(id)).thenReturn(Optional.of(client));

        Optional<Client> result = clientService.getClientById(id);

        assertEquals(result.get(), id);
    }

    private Client builtDefailtClient(){
        var client = new Client();
        client.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        client.setEmail("test@test.com");
        client.setFullName("Иванов Иван Иванович");
        client.setIsBlocked(false);
        return client;
    }

    private ClientDto builtDefaultClientDto() {
        return new ClientDto(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "Иванов Иван Иванович",
                "test@test.com",
                false
        );
    }
}