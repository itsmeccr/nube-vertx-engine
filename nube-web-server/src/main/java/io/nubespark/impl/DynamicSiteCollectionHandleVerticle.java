package io.nubespark.impl;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.nubespark.DynamicCollection;
import io.nubespark.impl.handlers.BaseCollectionHandler;
import io.nubespark.utils.CustomMessage;
import io.nubespark.utils.HttpException;
import io.nubespark.utils.StringUtils;
import io.nubespark.vertx.common.RxRestAPIVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.ext.mongo.MongoClient;

import java.util.List;

import static io.nubespark.constants.Address.DYNAMIC_SITE_COLLECTION_ADDRESS;
import static io.nubespark.utils.CustomMessageResponseHelper.*;

public class DynamicSiteCollectionHandleVerticle extends RxRestAPIVerticle {
    private Logger logger = LoggerFactory.getLogger(DynamicSiteCollectionHandleVerticle.class);
    private MongoClient mongoClient;

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    public void start() {
        super.start();
        mongoClient = MongoClient.createNonShared(vertx, config().getJsonObject("mongo").getJsonObject("config"));
        EventBus eventBus = getVertx().eventBus();

        // Receive message
        eventBus.consumer(DYNAMIC_SITE_COLLECTION_ADDRESS, this::handleRequest);
    }

    private void handleRequest(Message<Object> message) {
        CustomMessage customMessage = (CustomMessage) message.body();
        String url = customMessage.getHeader().getString("url");

        if (url.equals("") && customMessage.getHeader().getString("method").equalsIgnoreCase("GET")) {
            // Getting all values; for example when we need to display all settings
            handleGetAll(message, customMessage);
        } else if (validateUrl(url)) {
            if (validateData(customMessage)) {
                this.handleValidUrl(message, customMessage);
            } else {
                handleForbiddenResponse(message);
            }
        } else {
            handleNotFoundResponse(message);
        }
    }

    @SuppressWarnings("Duplicates")
    private void handleGetAll(Message<Object> message, CustomMessage customMessage) {
        String collection = customMessage.getHeader().getString("collection");
        JsonArray sitesIds = getSitesIds(customMessage);
        String siteId = customMessage.getHeader().getString("Site-Id");

        if (sitesIds.size() > 0) {
            if (sitesIds.contains(siteId)) {
                mongoClient.rxFind(collection, new JsonObject().put("site_id", siteId))
                    .subscribe(response -> {
                        CustomMessage<List<JsonObject>> replyMessage = new CustomMessage<>(
                            null,
                            response,
                            HttpResponseStatus.OK.code());
                        message.reply(replyMessage);
                    }, throwable -> handleException(message, throwable));
            } else {
                handleForbiddenResponse(message);
            }
        } else {
            handleBadRequestResponse(message, "User must be associated with <SiteSetting>");
        }
    }

    private JsonArray getSitesIds(CustomMessage customMessage) {
        JsonObject user = customMessage.getHeader().getJsonObject("user");
        JsonArray sitesIds = user.getJsonArray("sites_ids", new JsonArray());
        if (sitesIds.size() == 0 && StringUtils.isNotNull(user.getString("site_id"))) {
            sitesIds = new JsonArray().add(user.getString("site_id"));
        }
        return sitesIds;
    }

    @SuppressWarnings("Duplicates")
    private void handleException(Message<Object> message, Throwable throwable) {
        logger.info("Cause: " + throwable.getCause());
        logger.info("Message: " + throwable.getMessage());
        HttpException exception = (HttpException) throwable;
        CustomMessage<JsonObject> replyMessage = new CustomMessage<>(
            null,
            new JsonObject().put("message", exception.getMessage()),
            exception.getStatusCode().code());
        message.reply(replyMessage);
    }

    private void handleValidUrl(Message<Object> message, CustomMessage customMessage) {
        String method = customMessage.getHeader().getString("method");
        BaseCollectionHandler baseCollection = DynamicCollection.getCollection(customMessage.getHeader().getString("collection"));
        switch (method.toUpperCase()) {
            case "GET":
                baseCollection.handleGetUrl(message, customMessage, mongoClient);
                break;
            case "POST":
                baseCollection.handlePostUrl(message, customMessage, mongoClient);
                break;
            case "PUT":
                baseCollection.handlePutUrl(message, customMessage, mongoClient);
                break;
            case "DELETE":
                baseCollection.handleDeleteUrl(message, customMessage, mongoClient);
                break;
            default:
                handleNotFoundResponse(message);
                break;
        }
    }

    private boolean validateUrl(String url) {
        return !url.contains("?");
    }

    private boolean validateData(CustomMessage customMessage) {
        return StringUtils.isNotNull(customMessage.getHeader().getString("Site-Id"));
    }
}
