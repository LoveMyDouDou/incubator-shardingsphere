/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
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
 * </p>
 */

package io.shardingsphere.opentracing.hook;

import io.opentracing.ActiveSpan;
import io.opentracing.ActiveSpan.Continuation;
import io.opentracing.mock.MockSpan;
import io.opentracing.tag.Tags;
import io.shardingsphere.core.executor.sql.execute.threadlocal.ExecutorDataMap;
import io.shardingsphere.core.metadata.datasource.DataSourceMetaData;
import io.shardingsphere.core.spi.executor.SPISQLExecutionHook;
import io.shardingsphere.core.spi.executor.SQLExecutionHook;
import io.shardingsphere.opentracing.constant.ShardingTags;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public final class OpenTracingSQLExecutionHookTest extends BaseOpenTracingHookTest {
    
    private final SQLExecutionHook sqlExecutionHook = new SPISQLExecutionHook();
    
    private ActiveSpan activeSpan;
    
    @Before
    public void setUp() {
        activeSpan = mockActiveSpan();
    }
    
    private ActiveSpan mockActiveSpan() {
        Continuation continuation = mock(Continuation.class);
        ActiveSpan result = mock(ActiveSpan.class);
        when(continuation.activate()).thenReturn(result);
        ExecutorDataMap.getDataMap().put(OpenTracingRootInvokeHook.ACTIVE_SPAN_CONTINUATION, continuation);
        return result;
    }
    
    @After
    public void tearDown() {
        ExecutorDataMap.getDataMap().remove(OpenTracingRootInvokeHook.ACTIVE_SPAN_CONTINUATION);
    }
    
    @Test
    public void assertExecuteSuccessForTrunkThread() {
        DataSourceMetaData dataSourceMetaData = mock(DataSourceMetaData.class);
        when(dataSourceMetaData.getHostName()).thenReturn("localhost");
        when(dataSourceMetaData.getPort()).thenReturn(8888);
        sqlExecutionHook.start("ds_test", "SELECT * FROM XXX;", Arrays.<Object>asList("1", 2), dataSourceMetaData, true);
        sqlExecutionHook.finishSuccess();
        assertThat(getTracer().finishedSpans().size(), is(1));
        MockSpan actual = getTracer().finishedSpans().get(0);
        assertThat(actual.operationName(), is("/Sharding-Sphere/executeSQL/"));
        Map<String, Object> actualTags = actual.tags();
        assertThat(actualTags.get(Tags.COMPONENT.getKey()), CoreMatchers.<Object>is(ShardingTags.COMPONENT_NAME));
        assertThat(actualTags.get(Tags.SPAN_KIND.getKey()), CoreMatchers.<Object>is(Tags.SPAN_KIND_CLIENT));
        assertThat(actualTags.get(Tags.PEER_HOSTNAME.getKey()), CoreMatchers.<Object>is("localhost"));
        assertThat(actualTags.get(Tags.PEER_PORT.getKey()), CoreMatchers.<Object>is(8888));
        assertThat(actualTags.get(Tags.DB_TYPE.getKey()), CoreMatchers.<Object>is("sql"));
        assertThat(actualTags.get(Tags.DB_INSTANCE.getKey()), CoreMatchers.<Object>is("ds_test"));
        assertThat(actualTags.get(Tags.DB_STATEMENT.getKey()), CoreMatchers.<Object>is("SELECT * FROM XXX;"));
        assertThat(actualTags.get(ShardingTags.DB_BIND_VARIABLES.getKey()), CoreMatchers.<Object>is("1,2"));
        verify(activeSpan, times(0)).deactivate();
    }
    
    @Test
    public void assertExecuteSuccessForBranchThread() {
        DataSourceMetaData dataSourceMetaData = mock(DataSourceMetaData.class);
        when(dataSourceMetaData.getHostName()).thenReturn("localhost");
        when(dataSourceMetaData.getPort()).thenReturn(8888);
        sqlExecutionHook.start("ds_test", "SELECT * FROM XXX;", Arrays.<Object>asList("1", 2), dataSourceMetaData, false);
        sqlExecutionHook.finishSuccess();
        assertThat(getTracer().finishedSpans().size(), is(1));
        MockSpan actual = getTracer().finishedSpans().get(0);
        assertThat(actual.operationName(), is("/Sharding-Sphere/executeSQL/"));
        Map<String, Object> actualTags = actual.tags();
        assertThat(actualTags.get(Tags.COMPONENT.getKey()), CoreMatchers.<Object>is(ShardingTags.COMPONENT_NAME));
        assertThat(actualTags.get(Tags.SPAN_KIND.getKey()), CoreMatchers.<Object>is(Tags.SPAN_KIND_CLIENT));
        assertThat(actualTags.get(Tags.PEER_HOSTNAME.getKey()), CoreMatchers.<Object>is("localhost"));
        assertThat(actualTags.get(Tags.PEER_PORT.getKey()), CoreMatchers.<Object>is(8888));
        assertThat(actualTags.get(Tags.DB_TYPE.getKey()), CoreMatchers.<Object>is("sql"));
        assertThat(actualTags.get(Tags.DB_INSTANCE.getKey()), CoreMatchers.<Object>is("ds_test"));
        assertThat(actualTags.get(Tags.DB_STATEMENT.getKey()), CoreMatchers.<Object>is("SELECT * FROM XXX;"));
        assertThat(actualTags.get(ShardingTags.DB_BIND_VARIABLES.getKey()), CoreMatchers.<Object>is("1,2"));
        verify(activeSpan).deactivate();
    }
    
    @Test
    public void assertExecuteFailure() {
        DataSourceMetaData dataSourceMetaData = mock(DataSourceMetaData.class);
        when(dataSourceMetaData.getHostName()).thenReturn("localhost");
        when(dataSourceMetaData.getPort()).thenReturn(8888);
        sqlExecutionHook.start("ds_test", "SELECT * FROM XXX;", Collections.emptyList(), dataSourceMetaData, true);
        sqlExecutionHook.finishFailure(new RuntimeException("SQL execution error"));
        assertThat(getTracer().finishedSpans().size(), is(1));
        MockSpan actual = getTracer().finishedSpans().get(0);
        assertThat(actual.operationName(), is("/Sharding-Sphere/executeSQL/"));
        Map<String, Object> actualTags = actual.tags();
        assertThat(actualTags.get(Tags.COMPONENT.getKey()), CoreMatchers.<Object>is(ShardingTags.COMPONENT_NAME));
        assertThat(actualTags.get(Tags.SPAN_KIND.getKey()), CoreMatchers.<Object>is(Tags.SPAN_KIND_CLIENT));
        assertThat(actualTags.get(Tags.PEER_HOSTNAME.getKey()), CoreMatchers.<Object>is("localhost"));
        assertThat(actualTags.get(Tags.PEER_PORT.getKey()), CoreMatchers.<Object>is(8888));
        assertThat(actualTags.get(Tags.DB_TYPE.getKey()), CoreMatchers.<Object>is("sql"));
        assertThat(actualTags.get(Tags.DB_INSTANCE.getKey()), CoreMatchers.<Object>is("ds_test"));
        assertThat(actualTags.get(Tags.DB_STATEMENT.getKey()), CoreMatchers.<Object>is("SELECT * FROM XXX;"));
        assertThat(actualTags.get(ShardingTags.DB_BIND_VARIABLES.getKey()), CoreMatchers.<Object>is(""));
        assertSpanError(actual, RuntimeException.class, "SQL execution error");
        verify(activeSpan, times(0)).deactivate();
    }
}