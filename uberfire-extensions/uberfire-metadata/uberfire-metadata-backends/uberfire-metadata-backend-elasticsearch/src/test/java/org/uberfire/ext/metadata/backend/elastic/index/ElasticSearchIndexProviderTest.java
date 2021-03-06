/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.uberfire.ext.metadata.backend.elastic.index;

import java.util.Arrays;
import java.util.HashSet;

import com.google.common.collect.Sets;
import org.apache.lucene.analysis.Analyzer;
import org.elasticsearch.client.transport.TransportClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.uberfire.ext.metadata.backend.elastic.metamodel.ElasticMetaObject;
import org.uberfire.ext.metadata.backend.elastic.metamodel.ElasticMetaProperty;
import org.uberfire.ext.metadata.backend.elastic.provider.ElasticSearchContext;
import org.uberfire.ext.metadata.metamodel.NullMetaModelStore;
import org.uberfire.ext.metadata.model.schema.MetaProperty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ElasticSearchIndexProviderTest {

    private ElasticSearchIndexProvider provider;

    @Mock
    private ElasticSearchContext elasticSearchContext;

    @Mock
    private Analyzer analyzer;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private TransportClient transportClient;

    @Before
    public void setUp() {
        this.provider = spy(new ElasticSearchIndexProvider(new NullMetaModelStore(),
                                                           elasticSearchContext,
                                                           analyzer));

        doReturn(transportClient).when(this.provider).getClient();
    }

    @Test
    public void testEscapeSpecialCharacters() {

        String expected = "(+field:value AND +field2:\\+AAA123=) OR url:git\\:\\/\\/master@path\\/to\\/file";
        String queryString = "(+field:value AND +field2:+AAA123=) OR url:git://master@path/to/file";
        String escapedQueryString = this.provider.escapeSpecialCharacters(queryString);
        assertEquals(expected,
                     escapedQueryString);
    }

    @Test
    public void testCreateElasticTypeKeyword() {
        MetaProperty metaProperty = new ElasticMetaProperty("field",
                                                            "value",
                                                            new HashSet<>(Arrays.asList(String.class)));
        String type = this.provider.createElasticType(metaProperty);
        assertEquals(ElasticSearchIndexProvider.ES_KEYWORD_TYPE,
                     type);
    }

    @Test
    public void testCreateElasticTypeText() {
        MetaProperty metaProperty = new ElasticMetaProperty("field",
                                                            "value",
                                                            new HashSet<>(Arrays.asList(String.class)),
                                                            false,
                                                            true);
        String type = this.provider.createElasticType(metaProperty);
        assertEquals(ElasticSearchIndexProvider.ES_TEXT_TYPE,
                     type);
    }

    @Test
    public void testCreateElasticTypeOther() {
        MetaProperty metaProperty = new ElasticMetaProperty("field",
                                                            "1",
                                                            new HashSet<>(Arrays.asList(Integer.class)),
                                                            false,
                                                            true);
        String type = this.provider.createElasticType(metaProperty);
        assertEquals("integer",
                     type);
    }

    @Test
    public void testPrepareIndex() {
        String index = this.provider.sanitizeIndex("system_ou/plugins");
        assertEquals("system_ou_plugins",
                     index);
    }

    @Test
    public void testCreateIndexRequest() {

        ElasticMetaObject obj = new ElasticMetaObject(() -> "type");
        obj.addProperty(new ElasticMetaProperty("cluster.id",
                                                "system_ou/plugins",
                                                Sets.newHashSet(String.class)));
        obj.addProperty(new ElasticMetaProperty("type",
                                                "plugins",
                                                Sets.newHashSet(String.class)));
        this.provider.createIndexRequest(obj);

        verify(transportClient).prepareIndex(eq("system_ou_plugins"),
                                             eq("plugins"));
    }
}