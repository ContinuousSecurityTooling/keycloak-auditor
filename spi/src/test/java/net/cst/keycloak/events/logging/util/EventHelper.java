package net.cst.keycloak.events.logging.util;

import org.keycloak.events.Event;
import org.keycloak.events.EventType;

import java.util.UUID;


/**
 * @author : mreinhardt
 * @created : 22.06.23
 **/
public class EventHelper {

    public static Event buildEvent(EventType type, String realmId, String clientId, String userId) {
        Event event = new Event();
        event.setClientId(clientId);
        event.setUserId(userId);
        event.setType(type);
        event.setRealmId(realmId);
        return event;
    }

    public static Event buildUserLoginEvent() {
        return buildEvent(EventType.LOGIN, UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    public static Event buildClientLoginEvent() {
        return buildEvent(EventType.CLIENT_LOGIN, UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

}
