package net.cst.keycloak.events.logging.util;

import org.keycloak.models.ClientModel;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/**
 * @author : mreinhardt
 * @created : 22.06.23
 **/
public class ClientModelHelper {

    public static ClientModel buildClient(String clientId) {
        ClientModel client = mock(ClientModel.class);
        Map<String, String> attributes = new HashMap<>();
        when(client.getAttributes()).thenReturn(attributes);
        doAnswer((Answer<Void>) invocation -> {
            // Get the actual arguments
            String key = (String) invocation.getArguments()[0];
            String value = (String) invocation.getArguments()[1];
            // save to map
            attributes.put(key, value);
            return null;
        }).when(client).setAttribute(any(), any());
        when(client.getId()).thenReturn(clientId);
        return client;
    }

    public static ClientModel buildClient() {
        return buildClient(UUID.randomUUID().toString());
    }

}
