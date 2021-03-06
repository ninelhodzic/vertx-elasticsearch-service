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
package com.hubrick.vertx.elasticsearch.impl;

import com.hubrick.vertx.elasticsearch.ElasticSearchConfigurator;
import com.hubrick.vertx.elasticsearch.ElasticSearchService;
import com.hubrick.vertx.elasticsearch.TransportClientFactory;
import com.hubrick.vertx.elasticsearch.internal.InternalElasticSearchService;
import com.hubrick.vertx.elasticsearch.model.BaseSortOption;
import com.hubrick.vertx.elasticsearch.model.BaseSuggestOption;
import com.hubrick.vertx.elasticsearch.model.CompletionSuggestOption;
import com.hubrick.vertx.elasticsearch.model.DeleteByQueryOptions;
import com.hubrick.vertx.elasticsearch.model.DeleteOptions;
import com.hubrick.vertx.elasticsearch.model.FieldSortOption;
import com.hubrick.vertx.elasticsearch.model.GetOptions;
import com.hubrick.vertx.elasticsearch.model.IndexOptions;
import com.hubrick.vertx.elasticsearch.model.ScriptSortOption;
import com.hubrick.vertx.elasticsearch.model.SearchOptions;
import com.hubrick.vertx.elasticsearch.model.SearchScrollOptions;
import com.hubrick.vertx.elasticsearch.model.SuggestOptions;
import com.hubrick.vertx.elasticsearch.model.UpdateOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryAction;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.Template;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;

import javax.inject.Inject;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.hubrick.vertx.elasticsearch.impl.ElasticSearchServiceMapper.mapToDeleteByQueryResponse;
import static com.hubrick.vertx.elasticsearch.impl.ElasticSearchServiceMapper.mapToDeleteResponse;
import static com.hubrick.vertx.elasticsearch.impl.ElasticSearchServiceMapper.mapToIndexResponse;
import static com.hubrick.vertx.elasticsearch.impl.ElasticSearchServiceMapper.mapToSearchResponse;
import static com.hubrick.vertx.elasticsearch.impl.ElasticSearchServiceMapper.mapToSuggestResponse;
import static com.hubrick.vertx.elasticsearch.impl.ElasticSearchServiceMapper.mapToUpdateResponse;

/**
 * Default implementation of {@link ElasticSearchService}
 */
public class DefaultElasticSearchService implements InternalElasticSearchService {

    private final Logger log = LoggerFactory.getLogger(DefaultElasticSearchService.class);
    private final TransportClientFactory clientFactory;
    private final ElasticSearchConfigurator configurator;
    protected TransportClient client;

    public static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    @Inject
    public DefaultElasticSearchService(TransportClientFactory clientFactory, ElasticSearchConfigurator configurator) {
        this.clientFactory = clientFactory;
        this.configurator = configurator;
    }

    @Override
    public void start() {

        Settings.setSettingsRequireUnits(configurator.getSettingsRequireUnits());

        Settings settings = Settings.builder()
                .put("cluster.name", configurator.getClusterName())
                .put("client.transport.sniff", configurator.getClientTransportSniff())
                .build();

        client = clientFactory.create(settings);
        configurator.getTransportAddresses().forEach(client::addTransportAddress);

    }

    @Override
    public void stop() {
        client.close();
        client = null;
    }

    @Override
    public void index(String index, String type, JsonObject source, IndexOptions options, Handler<AsyncResult<com.hubrick.vertx.elasticsearch.model.IndexResponse>> resultHandler) {

        IndexRequestBuilder builder = client.prepareIndex(index, type)
                .setSource(source.encode());

        if (options != null) {
            if (options.getId() != null) builder.setId(options.getId());
            if (options.getRouting() != null) builder.setRouting(options.getRouting());
            if (options.getParent() != null) builder.setParent(options.getParent());
            if (options.getOpType() != null) builder.setOpType(options.getOpType());
            if (options.isRefresh() != null) builder.setRefresh(options.isRefresh());
            if (options.getConsistencyLevel() != null) builder.setConsistencyLevel(options.getConsistencyLevel());
            if (options.getVersion() != null) builder.setVersion(options.getVersion());
            if (options.getVersionType() != null) builder.setVersionType(options.getVersionType());
            if (options.getTimestamp() != null) builder.setTimestamp(options.getTimestamp());
            if (options.getTtl() != null) builder.setTTL(options.getTtl());
            if (options.getTimeout() != null) builder.setTimeout(options.getTimeout());
        }

        builder.execute(new ActionListener<IndexResponse>() {
            @Override
            public void onResponse(IndexResponse indexResponse) {
                resultHandler.handle(Future.succeededFuture(mapToIndexResponse(indexResponse)));
            }

            @Override
            public void onFailure(Throwable t) {
                handleFailure(resultHandler, t);
            }
        });

    }

    @Override
    public void update(String index, String type, String id, UpdateOptions options, Handler<AsyncResult<com.hubrick.vertx.elasticsearch.model.UpdateResponse>> resultHandler) {

        UpdateRequestBuilder builder = client.prepareUpdate(index, type, id);

        if (options != null) {
            if (options.getRouting() != null) builder.setRouting(options.getRouting());
            if (options.getParent() != null) builder.setParent(options.getParent());
            if (options.isRefresh() != null) builder.setRefresh(options.isRefresh());
            if (options.getConsistencyLevel() != null) builder.setConsistencyLevel(options.getConsistencyLevel());
            if (options.getVersion() != null) builder.setVersion(options.getVersion());
            if (options.getVersionType() != null) builder.setVersionType(options.getVersionType());
            if (options.getTimeout() != null) builder.setTimeout(options.getTimeout());

            if (options.getRetryOnConflict() != null) builder.setRetryOnConflict(options.getRetryOnConflict());
            if (options.getDoc() != null) builder.setDoc(options.getDoc().encode());
            if (options.getUpsert() != null) builder.setUpsert(options.getUpsert().encode());
            if (options.isDocAsUpsert() != null) builder.setDocAsUpsert(options.isDocAsUpsert());
            if (options.isDetectNoop() != null) builder.setDetectNoop(options.isDetectNoop());
            if (options.isScriptedUpsert() != null) builder.setScriptedUpsert(options.isScriptedUpsert());

            if (options.getScript() != null) {
                if (options.getScriptType() != null) {
                    Map<String, ? extends Object> params = (options.getScriptParams() == null ? null : convertJsonObjectToMap(options.getScriptParams()));
                    builder.setScript(new Script(options.getScript(), options.getScriptType(), options.getScriptLang(), params));
                } else {
                    builder.setScript(new Script(options.getScript()));
                }
            }
            if (!options.getFields().isEmpty()) {
                builder.setFields(options.getFields().toArray(new String[options.getFields().size()]));
            }
        }

        builder.execute(new ActionListener<UpdateResponse>() {
            @Override
            public void onResponse(UpdateResponse updateResponse) {
                resultHandler.handle(Future.succeededFuture(mapToUpdateResponse(updateResponse)));
            }

            @Override
            public void onFailure(Throwable e) {
                handleFailure(resultHandler, e);
            }
        });

    }

    @Override
    public void get(String index, String type, String id, GetOptions options, Handler<AsyncResult<com.hubrick.vertx.elasticsearch.model.GetResponse>> resultHandler) {

        GetRequestBuilder builder = client.prepareGet(index, type, id);

        if (options != null) {
            if (options.getRouting() != null) builder.setRouting(options.getRouting());
            if (options.getParent() != null) builder.setParent(options.getParent());
            if (options.isRefresh() != null) builder.setRefresh(options.isRefresh());
            if (options.getVersion() != null) builder.setVersion(options.getVersion());
            if (options.getVersionType() != null) builder.setVersionType(options.getVersionType());

            if (options.getPreference() != null) builder.setPreference(options.getPreference());
            if (!options.getFields().isEmpty()) {
                builder.setFields(options.getFields().toArray(new String[options.getFields().size()]));
            }
            if (options.isFetchSource() != null) builder.setFetchSource(options.isFetchSource());
            if (!options.getFetchSourceIncludes().isEmpty() || !options.getFetchSourceExcludes().isEmpty()) {
                String[] includes = options.getFetchSourceIncludes().toArray(new String[options.getFetchSourceIncludes().size()]);
                String[] excludes = options.getFetchSourceExcludes().toArray(new String[options.getFetchSourceExcludes().size()]);
                builder.setFetchSource(includes, excludes);
            }
            if (options.isTransformSource() != null) builder.setTransformSource(options.isTransformSource());
            if (options.isRealtime() != null) builder.setRealtime(options.isRealtime());
            if (options.isIgnoreErrorsOnGeneratedFields() != null) {
                builder.setIgnoreErrorsOnGeneratedFields(options.isIgnoreErrorsOnGeneratedFields());
            }
        }

        builder.execute(new ActionListener<GetResponse>() {
            @Override
            public void onResponse(GetResponse getResponse) {
                resultHandler.handle(Future.succeededFuture(mapToUpdateResponse(getResponse)));
            }

            @Override
            public void onFailure(Throwable t) {
                handleFailure(resultHandler, t);
            }
        });

    }

    @Override
    public void search(List<String> indices, SearchOptions options, Handler<AsyncResult<com.hubrick.vertx.elasticsearch.model.SearchResponse>> resultHandler) {

        SearchRequestBuilder builder = client.prepareSearch(indices.toArray(new String[indices.size()]));

        if (options != null) {
            if (!options.getTypes().isEmpty()) {
                builder.setTypes(options.getTypes().toArray(new String[options.getTypes().size()]));
            }
            if (options.getSearchType() != null) builder.setSearchType(options.getSearchType());
            if (options.getScroll() != null) builder.setScroll(options.getScroll());
            if (options.getTimeout() != null) builder.setTimeout(options.getTimeout());
            if (options.getTerminateAfter() != null) builder.setTerminateAfter(options.getTerminateAfter());
            if (options.getRouting() != null) builder.setRouting(options.getRouting());
            if (options.getPreference() != null) builder.setPreference(options.getPreference());
            if (options.getQuery() != null) builder.setQuery(options.getQuery().encode());
            if (options.getPostFilter() != null) builder.setPostFilter(options.getPostFilter().encode());
            if (options.getMinScore() != null) builder.setMinScore(options.getMinScore());
            if (options.getSize() != null) builder.setSize(options.getSize());
            if (options.getFrom() != null) builder.setFrom(options.getFrom());
            if (options.isExplain() != null) builder.setExplain(options.isExplain());
            if (options.isVersion() != null) builder.setVersion(options.isVersion());
            if (options.isFetchSource() != null) builder.setFetchSource(options.isFetchSource());
            if (!options.getFields().isEmpty()) options.getFields().forEach(builder::addField);
            if (options.isTrackScores() != null) builder.setTrackScores(options.isTrackScores());
            if (options.getAggregations() != null) {
                builder.setAggregations(options.getAggregations().encode().getBytes(CHARSET_UTF8));
            }
            if (!options.getSorts().isEmpty()) {
                for (BaseSortOption baseSortOption : options.getSorts()) {
                    switch (baseSortOption.getSortType()) {
                        case FIELD:
                            final FieldSortOption fieldSortOption = (FieldSortOption) baseSortOption;
                            builder.addSort(fieldSortOption.getField(), fieldSortOption.getOrder());
                            break;
                        case SCRIPT:
                            final ScriptSortOption scriptSortOption = (ScriptSortOption) baseSortOption;
                            final Script script = new Script(scriptSortOption.getScript(), ScriptService.ScriptType.INLINE, scriptSortOption.getLang(), convertJsonObjectToMap(scriptSortOption.getParams()));
                            final ScriptSortBuilder scriptSortBuilder = new ScriptSortBuilder(script, scriptSortOption.getType().getValue()).order(scriptSortOption.getOrder());
                            builder.addSort(scriptSortBuilder);
                            break;
                    }
                }
            }
            if (options.getExtraSource() != null) builder.setExtraSource(options.getExtraSource().encode());
            if (options.getTemplateName() != null) {
                if (options.getTemplateType() != null) {
                    Map<String, Object> params = (options.getTemplateParams() == null ? null : options.getTemplateParams().getMap());
                    builder.setTemplate(new Template(options.getTemplateName(), options.getTemplateType(), null, null, params));
                } else {
                    builder.setTemplate(new Template(options.getTemplateName()));
                }
            }
            if (!options.getScriptFields().isEmpty()) {
                options.getScriptFields().entrySet().forEach(scriptFieldEntry -> {
                    final Script script = new Script(scriptFieldEntry.getValue().getScript(), ScriptService.ScriptType.INLINE, scriptFieldEntry.getValue().getLang(), convertJsonObjectToMap(scriptFieldEntry.getValue().getParams()));
                    builder.addScriptField(scriptFieldEntry.getKey(), script);
                });
            }
        }

        builder.execute(new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(SearchResponse searchResponse) {
                resultHandler.handle(Future.succeededFuture(mapToSearchResponse(searchResponse)));
            }

            @Override
            public void onFailure(Throwable t) {
                handleFailure(resultHandler, t);
            }
        });
    }

    @Override
    public void searchScroll(String scrollId, SearchScrollOptions options, Handler<AsyncResult<com.hubrick.vertx.elasticsearch.model.SearchResponse>> resultHandler) {

        SearchScrollRequestBuilder builder = client.prepareSearchScroll(scrollId);

        if (options != null) {
            if (options.getScroll() != null) builder.setScroll(options.getScroll());
        }

        builder.execute(new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(SearchResponse searchResponse) {
                resultHandler.handle(Future.succeededFuture(mapToSearchResponse(searchResponse)));
            }

            @Override
            public void onFailure(Throwable t) {
                handleFailure(resultHandler, t);
            }
        });

    }

    @Override
    public void delete(String index, String type, String id, DeleteOptions options, Handler<AsyncResult<com.hubrick.vertx.elasticsearch.model.DeleteResponse>> resultHandler) {

        DeleteRequestBuilder builder = client.prepareDelete(index, type, id);

        if (options != null) {
            if (options.getRouting() != null) builder.setRouting(options.getRouting());
            if (options.getParent() != null) builder.setParent(options.getParent());
            if (options.isRefresh() != null) builder.setRefresh(options.isRefresh());
            if (options.getConsistencyLevel() != null) builder.setConsistencyLevel(options.getConsistencyLevel());
            if (options.getVersion() != null) builder.setVersion(options.getVersion());
            if (options.getVersionType() != null) builder.setVersionType(options.getVersionType());
            if (options.getTimeout() != null) builder.setTimeout(options.getTimeout());
        }

        builder.execute(new ActionListener<DeleteResponse>() {
            @Override
            public void onResponse(DeleteResponse deleteResponse) {
                resultHandler.handle(Future.succeededFuture(mapToDeleteResponse(deleteResponse)));
            }

            @Override
            public void onFailure(Throwable t) {
                handleFailure(resultHandler, t);
            }
        });

    }

    @Override
    public void suggest(List<String> indices, SuggestOptions options, Handler<AsyncResult<com.hubrick.vertx.elasticsearch.model.SuggestResponse>> resultHandler) {

        final SuggestRequestBuilder builder = client.prepareSuggest(indices.toArray(new String[indices.size()]));

        if (options != null && !options.getSuggestions().isEmpty()) {
            for (Map.Entry<String, BaseSuggestOption> suggestOptionEntry : options.getSuggestions().entrySet()) {
                switch (suggestOptionEntry.getValue().getSuggestionType()) {
                    case COMPLETION:
                        final CompletionSuggestOption completionSuggestOption = (CompletionSuggestOption) suggestOptionEntry.getValue();
                        final CompletionSuggestionBuilder completionBuilder = new CompletionSuggestionBuilder(suggestOptionEntry.getKey());
                        if (completionSuggestOption.getText() != null) {
                            completionBuilder.text(completionSuggestOption.getText());
                        }
                        if (completionSuggestOption.getField() != null) {
                            completionBuilder.field(completionSuggestOption.getField());
                        }
                        if (completionSuggestOption.getSize() != null) {
                            completionBuilder.size(completionSuggestOption.getSize());
                        }

                        builder.addSuggestion(completionBuilder);
                        break;
                }
            }
        }

        builder.execute(new ActionListener<SuggestResponse>() {

            @Override
            public void onResponse(SuggestResponse suggestResponse) {
                resultHandler.handle(Future.succeededFuture(mapToSuggestResponse(suggestResponse)));
            }

            @Override
            public void onFailure(Throwable t) {
                handleFailure(resultHandler, t);
            }
        });

    }

    @Override
    public void deleteByQuery(List<String> indices, JsonObject query, DeleteByQueryOptions options, Handler<AsyncResult<com.hubrick.vertx.elasticsearch.model.DeleteByQueryResponse>> resultHandler) {
        final DeleteByQueryRequestBuilder deleteByQueryRequestBuilder = new DeleteByQueryRequestBuilder(client, DeleteByQueryAction.INSTANCE)
                .setIndices(indices.toArray(new String[indices.size()]));

        if (query != null) {
            deleteByQueryRequestBuilder.setSource("{\"query\": " + query.encode() + "}");
        }

        if (options != null) {
            if (!options.getTypes().isEmpty()) {
                deleteByQueryRequestBuilder.setTypes(options.getTypes().toArray(new String[options.getTypes().size()]));
            }
            if (options.getTimeout() != null) deleteByQueryRequestBuilder.setTimeout(options.getTimeout());
            if (options.getRouting() != null) deleteByQueryRequestBuilder.setRouting(options.getRouting());
        }

        deleteByQueryRequestBuilder.execute(new ActionListener<DeleteByQueryResponse>() {
            @Override
            public void onResponse(DeleteByQueryResponse deleteByQueryResponse) {
                resultHandler.handle(Future.succeededFuture(mapToDeleteByQueryResponse(deleteByQueryResponse)));
            }

            @Override
            public void onFailure(Throwable t) {
                handleFailure(resultHandler, t);
            }
        });
    }

    @Override
    public TransportClient getClient() {
        return client;
    }

    private <T> void handleFailure(final Handler<AsyncResult<T>> resultHandler, final Throwable t) {
        log.error("Error occurred in ElasticSearchService", t);

        if (t instanceof ElasticsearchException) {
            final ElasticsearchException esException = (ElasticsearchException) t;
            resultHandler.handle(Future.failedFuture(esException.getDetailedMessage()));
        } else {
            resultHandler.handle(Future.failedFuture(t));
        }
    }

    private Map<String, Object> convertJsonObjectToMap(JsonObject jsonObject) {

        final Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, Object> jsonObjectEntry : jsonObject) {
            if (jsonObjectEntry.getValue() instanceof JsonArray) {
                map.put(jsonObjectEntry.getKey(), convertJsonArrayToList((JsonArray) jsonObjectEntry.getValue()));
            } else if (jsonObjectEntry.getValue() instanceof JsonObject) {
                map.put(jsonObjectEntry.getKey(), convertJsonObjectToMap((JsonObject) jsonObjectEntry.getValue()));
            } else {
                map.put(jsonObjectEntry.getKey(), jsonObjectEntry.getValue());
            }
        }

        return map;
    }

    private List<Object> convertJsonArrayToList(JsonArray jsonArray) {

        final List<Object> list = new LinkedList<>();
        for (Object jsonArrayEntry : jsonArray) {
            if(jsonArrayEntry instanceof JsonObject) {
                list.add(convertJsonObjectToMap((JsonObject) jsonArrayEntry));
            } else if(jsonArrayEntry instanceof JsonArray) {
                list.add(convertJsonArrayToList((JsonArray) jsonArrayEntry));
            } else {
                list.add(jsonArrayEntry);
            }
        }

        return list;
    }

}
