package io.nubespark.postgresql.impl;

import io.nubespark.postgresql.PostgreSQLService;
import io.nubespark.vertx.common.BaseService;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.asyncsql.PostgreSQLClient;
import io.vertx.reactivex.ext.sql.SQLClient;
import io.vertx.reactivex.ext.sql.SQLConnection;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;


public class PostgreSQLServiceImpl implements PostgreSQLService, BaseService {
    private final Logger logger = LoggerFactory.getLogger(PostgreSQLService.class);
    private final SQLClient client;

    public PostgreSQLServiceImpl(Vertx vertx, JsonObject config) {
        this.client = PostgreSQLClient.createNonShared(vertx, config);
    }

    @Override
    public Single<PostgreSQLService> initializeService() {
        logger.info("Initializing ...");

        return Single.just(this);
    }

    private SingleObserver<JsonArray> toObserverFromArray(final Handler<AsyncResult<JsonObject>> handler) {
        final AtomicBoolean completed = new AtomicBoolean();
        return new SingleObserver<JsonArray>() {
            public void onSubscribe(@NonNull Disposable d) {
            }

            public void onSuccess(@NonNull JsonArray item) {
                if (completed.compareAndSet(false, true)) {
                    handler.handle(Future.succeededFuture(successMessage(item)));
                }
            }

            public void onError(Throwable error) {
                if (completed.compareAndSet(false, true)) {
                    handler.handle(Future.succeededFuture(failureMessage(error)));
                }
            }
        };
    }

    private Single<SQLConnection> getConnection() {
        return client.rxGetConnection()
            .flatMap(conn -> Single.just(conn)
                .doOnError(throwable -> logger.error("Cannot get connection object."))
                .doFinally(conn::close));
    }

    @Override
    public PostgreSQLService executeQueryWithParams(String sqlQuery, @Nullable JsonArray params, Handler<AsyncResult<JsonObject>> resultHandler) {
        getConnection()
            .flatMap(conn -> {
                if (params == null) {
                    return conn.rxQuery(sqlQuery);
                }
                return conn.rxQueryWithParams(sqlQuery, params);
            })
            .map(result -> new JsonArray(result.getNumRows() > 0 ? result.getRows() : Collections.emptyList()))
            .subscribe(toObserverFromArray(resultHandler));

        return this;
    }

    @Override
    public PostgreSQLService executeQuery(String query, Handler<AsyncResult<JsonObject>> resultHandler) {
        return executeQueryWithParams(query, null, resultHandler);
    }

    private JsonObject failureMessage(Throwable t) {
        return new JsonObject()
            .put("status", "FAILED")
            .put("error", t.getMessage());
    }

    private JsonObject successMessage(JsonArray jsonArray) {
        return new JsonObject()
            .put("status", "OK")
            .put("message", jsonArray);
    }
}
