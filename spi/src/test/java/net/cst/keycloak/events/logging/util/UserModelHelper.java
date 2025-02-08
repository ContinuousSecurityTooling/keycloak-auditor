package net.cst.keycloak.events.logging.util;

import org.keycloak.models.UserModel;
import org.mockito.stubbing.Answer;

import java.util.*;

import static net.cst.keycloak.audit.model.Constants.LAST_LOGIN_INFIX;
import static net.cst.keycloak.audit.model.Constants.USER_EVENT_PREFIX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


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
        }).when(user).setSingleAttribute(any(), any());
        when(user.getId()).thenReturn(userId);
        return user;
    }

    public static UserModel buildUser(String userId, String lastLogin) {
        UserModel user = buildUser(userId);
        user.getAttributes().put(USER_EVENT_PREFIX.value() + "_" + LAST_LOGIN_INFIX.value(),
                Collections.singletonList(lastLogin));
        return user;
    }

    public static UserModel buildUser() {
        return buildUser(UUID.randomUUID().toString());
    }

}
