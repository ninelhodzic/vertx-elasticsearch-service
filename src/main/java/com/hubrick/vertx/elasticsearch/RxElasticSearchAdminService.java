/**
 * Copyright (C) 2016 Etaia AS (oss@hubrick.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hubrick.vertx.elasticsearch;

import com.hubrick.vertx.elasticsearch.impl.DefaultRxElasticSearchAdminService;
import com.hubrick.vertx.elasticsearch.model.MappingOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ProxyHelper;
import rx.Observable;

import java.util.Collections;
import java.util.List;

/**
 * @author Emir Dizdarevic
 * @since 1.0.0
 */
public interface RxElasticSearchAdminService {

    static RxElasticSearchAdminService createEventBusProxy(Vertx vertx, String address) {
        return new DefaultRxElasticSearchAdminService(ProxyHelper.createProxy(ElasticSearchAdminService.class, vertx, address));
    }

    default Observable<JsonObject> putMapping(String index, String type, JsonObject source) {
        return putMapping(Collections.singletonList(index), type, source);
    }

    default Observable<JsonObject> putMapping(String index, String type, JsonObject source, MappingOptions options) {
        return putMapping(Collections.singletonList(index), type, source, options);
    }

    default Observable<JsonObject> putMapping(List<String> indices, String type, JsonObject source) {
        return putMapping(indices, type, source, null);
    }

    Observable<JsonObject> putMapping(List<String> indices, String type, JsonObject source, MappingOptions options);

}
