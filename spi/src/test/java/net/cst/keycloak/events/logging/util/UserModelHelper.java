package net.cst.keycloak.events.logging.util;

import org.keycloak.models.UserModel;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/**
 * @author : mreinhardt
 * @created : 22.06.23
 **/
public class UserModelHelper {

    public static UserModel buildUser(String userId) {
        UserModel user = mock(UserModel.class);
        Map<String, List<String>> userAttributes = new HashMap<>();
        when(user.getAttributes()).thenReturn(userAttributes);
        doAnswer((Answer<Void>) invocation -> {
            // Get the actual arguments
            String key = (String) invocation.getArguments()[0];
            String value = (String) invocation.getArguments()[1];
            // save to map
            userAttributes.put(key, Collections.singletonList(value));
            return null;
        }).when(user).setSingleAttribute(any(),any());
        when(user.getId()).thenReturn(userId);
        return user;
    }

    public static UserModel buildUser() {
        return buildUser(UUID.randomUUID().toString());
    }

}
