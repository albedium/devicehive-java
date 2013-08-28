package com.devicehive.websockets;

import com.devicehive.json.GsonFactory;
import com.devicehive.json.providers.JsonEncoder;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.utils.LogExecutionTime;
import com.devicehive.websockets.handlers.DeviceMessageHandlers;
import com.devicehive.websockets.util.SessionMonitor;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;

@ServerEndpoint(value = "/websocket/device", encoders = {JsonEncoder.class})
@LogExecutionTime
public class DeviceEndpoint extends Endpoint {

    private static final Logger logger = LoggerFactory.getLogger(DeviceEndpoint.class);

    @Inject
    private DeviceMessageHandlers deviceMessageHandlers;
    @EJB
    private SubscriptionManager subscriptionManager;
    @EJB
    private SessionMonitor sessionMonitor;

    @OnOpen
    public void onOpen(Session session) {
        logger.debug("[onOpen] session id {}", session.getId());
        WebsocketSession.createCommandsSubscriptionsLock(session);
        WebsocketSession.createQueueLock(session);
        sessionMonitor.registerSession(session);
    }

    @OnMessage(maxMessageSize = MAX_MESSAGE_SIZE)
    public JsonObject onMessage(Reader reader, Session session) throws InvocationTargetException, IllegalAccessException {
        logger.debug("[onMessage] session id {}", session.getId());
        return processMessage(deviceMessageHandlers, reader, session);
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.debug("[onClose] session id {}, close reason is {}", session.getId(), closeReason);
        subscriptionManager.getCommandSubscriptionStorage().removeBySession(session.getId());
    }

    @OnError
    public void onError(Throwable exception, Session session) {
        logger.error("[onError]", exception);
    }

}
