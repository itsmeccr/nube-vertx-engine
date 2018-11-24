package io.nubespark.impl.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.nubespark.Role;
import io.nubespark.utils.CustomMessage;
import io.nubespark.utils.HttpException;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.mongo.MongoClient;

import static io.nubespark.utils.CustomMessageResponseHelper.handleBadRequestResponse;
import static io.nubespark.utils.CustomMessageResponseHelper.handleForbiddenResponse;

public class MenuCollectionHandler extends BaseCollectionHandler {
    public void handlePutUrl(Message<Object> message, CustomMessage customMessage, MongoClient mongoClient) {
        String role = customMessage.getHeader().getJsonObject("user").getString("role");
        String collection = customMessage.getHeader().getString("collection");
        String url = customMessage.getHeader().getString("url");
        String siteId = customMessage.getHeader().getString("Site-Id");
        JsonArray sitesIds = getSitesIds(customMessage);
        if (sitesIds.size() == 0) {
            handleBadRequestResponse(message, "User must be associated with <SiteSetting>");
        } else if (!role.equals(Role.GUEST.toString())) {
            if (sitesIds.contains(siteId)) {
                String[] urls = url.split("/");
                if (url.endsWith("/") ? urls.length == 4 : urls.length == 3) {
                    String id = urls[0];
                    String menuId = urls[1];
                    String childMenuId = urls[2];
                    handleMenuPut(message, customMessage, mongoClient, collection, id, siteId, menuId, childMenuId);
                } else if (url.endsWith("/") ? urls.length == 3 : urls.length == 2) {
                    String id = urls[0];
                    String menuId = urls[1];
                    handleMenuPut(message, customMessage, mongoClient, collection, id, siteId, menuId);
                } else if (url.endsWith("/") ? urls.length == 2 : urls.length == 1) {
                    String id = urls[0];
                    handlePutDocument(message, customMessage, mongoClient, collection, id, siteId);
                }
            } else {
                handleForbiddenResponse(message);
            }
        } else {
            handleForbiddenResponse(message);
        }
    }

    private void handleMenuPut(Message<Object> message, CustomMessage customMessage, MongoClient mongoClient, String collection, String id, String siteId, String menuId) {
        JsonObject body = (JsonObject) customMessage.getBody();
        body.put("id", menuId);
        mongoClient.rxUpdate(collection, new JsonObject().put("site_id", siteId).put("id", id).put("menu.id", menuId),
            new JsonObject().put("$set", new JsonObject().put("menu.$", body)))
            .subscribe(() -> {
                CustomMessage<JsonObject> replyMessage = new CustomMessage<>(
                    null,
                    new JsonObject(),
                    HttpResponseStatus.OK.code());
                message.reply(replyMessage);
            }, throwable -> {
                HttpException exception = (HttpException) throwable;
                CustomMessage<JsonObject> replyMessage = new CustomMessage<>(
                    null,
                    new JsonObject().put("message", exception.getMessage()),
                    exception.getStatusCode().code());
                message.reply(replyMessage);
            });
    }

    private void handleMenuPut(Message<Object> message, CustomMessage customMessage, MongoClient mongoClient, String collection, String id, String siteId, String menuId, String childMenuId) {
        JsonObject body = (JsonObject) customMessage.getBody();
        body.put("id", childMenuId);
        mongoClient.rxUpdate(collection, new JsonObject().put("site_id", siteId).put("id", id).put("menu.id", menuId).put("menu.children.id", childMenuId),
            new JsonObject().put("$set", new JsonObject().put("menu.0.children.$", body)))
            .subscribe(() -> {
                CustomMessage<JsonObject> replyMessage = new CustomMessage<>(
                    null,
                    new JsonObject(),
                    HttpResponseStatus.OK.code());
                message.reply(replyMessage);
            }, throwable -> {
                HttpException exception = (HttpException) throwable;
                CustomMessage<JsonObject> replyMessage = new CustomMessage<>(
                    null,
                    new JsonObject().put("message", exception.getMessage()),
                    exception.getStatusCode().code());
                message.reply(replyMessage);
            });
    }

    public void handleDeleteUrl(Message<Object> message, CustomMessage customMessage, MongoClient mongoClient) {
        String collection = customMessage.getHeader().getString("collection");
        String url = customMessage.getHeader().getString("url");
        String siteId = customMessage.getHeader().getString("Site-Id");
        JsonArray sitesIds = getSitesIds(customMessage);
        if (sitesIds.size() > 0) {
            if (sitesIds.contains(siteId)) {
                String[] urls = url.split("/");
                if (url.endsWith("/") ? urls.length == 4 : urls.length == 3) {
                    String id = urls[0];
                    String menuId = urls[1];
                    String childMenuId = urls[2];
                    handleMenuDelete(message, customMessage, mongoClient, collection, id, siteId, menuId, childMenuId);
                } else if (url.endsWith("/") ? urls.length == 3 : urls.length == 2) {
                    String id = urls[0];
                    String menuId = urls[1];
                    handleMenuDelete(message, customMessage, mongoClient, collection, id, siteId, menuId);
                } else if (url.endsWith("/") ? urls.length == 2 : urls.length == 1) {
                    String id = urls[0];
                    handleDeleteDocument(message, mongoClient, collection, id, siteId);
                }
            } else {
                handleForbiddenResponse(message);
            }
        } else {
            handleBadRequestResponse(message, "User must be associated with <SiteSetting>");
        }
    }

    private void handleMenuDelete(Message<Object> message, CustomMessage customMessage, MongoClient mongoClient, String collection, String id, String siteId, String menuId) {
        JsonObject body = (JsonObject) customMessage.getBody();
        body.put("id", menuId);
        mongoClient.rxUpdate(collection, new JsonObject().put("site_id", siteId).put("id", id).put("menu.id", menuId),
            new JsonObject().put("$pull", new JsonObject().put("menu", new JsonObject().put("id", menuId))))
            .subscribe(() -> {
                CustomMessage<JsonObject> replyMessage = new CustomMessage<>(
                    null,
                    new JsonObject(),
                    HttpResponseStatus.OK.code());
                message.reply(replyMessage);
            }, throwable -> {
                HttpException exception = (HttpException) throwable;
                CustomMessage<JsonObject> replyMessage = new CustomMessage<>(
                    null,
                    new JsonObject().put("message", exception.getMessage()),
                    exception.getStatusCode().code());
                message.reply(replyMessage);
            });
    }

    private void handleMenuDelete(Message<Object> message, CustomMessage customMessage, MongoClient mongoClient, String collection, String id, String siteId, String menuId, String childMenuId) {
        JsonObject body = (JsonObject) customMessage.getBody();
        body.put("id", childMenuId);
        mongoClient.rxUpdate(collection, new JsonObject().put("site_id", siteId).put("id", id).put("menu.id", menuId).put("menu.children.id", childMenuId),
            new JsonObject().put("$pull", new JsonObject().put("menu.0.children", new JsonObject().put("id", childMenuId))))
            .subscribe(() -> {
                CustomMessage<JsonObject> replyMessage = new CustomMessage<>(
                    null,
                    new JsonObject(),
                    HttpResponseStatus.OK.code());
                message.reply(replyMessage);
            }, throwable -> {
                HttpException exception = (HttpException) throwable;
                CustomMessage<JsonObject> replyMessage = new CustomMessage<>(
                    null,
                    new JsonObject().put("message", exception.getMessage()),
                    exception.getStatusCode().code());
                message.reply(replyMessage);
            });
    }
}
