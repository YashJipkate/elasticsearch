/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.watcher.transport.action.delete;

import org.elasticsearch.test.junit.annotations.TestLogging;
import org.elasticsearch.watcher.support.Script;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTests;
import org.elasticsearch.watcher.transport.actions.delete.DeleteWatchResponse;
import org.elasticsearch.watcher.transport.actions.put.PutWatchResponse;
import org.elasticsearch.watcher.transport.actions.service.WatcherServiceResponse;
import org.junit.Test;

import static org.elasticsearch.watcher.actions.ActionBuilders.loggingAction;
import static org.elasticsearch.watcher.client.WatchSourceBuilders.watchBuilder;
import static org.elasticsearch.watcher.condition.ConditionBuilders.scriptCondition;
import static org.elasticsearch.watcher.trigger.TriggerBuilders.schedule;
import static org.elasticsearch.watcher.trigger.schedule.Schedules.interval;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 */
public class ForceDeleteWatchTests extends AbstractWatcherIntegrationTests {

    //Disable time warping for the force delete long running watch test
    protected boolean timeWarped() {
        return false;
    }

    @Override
    protected boolean enableShield() {
        return false;
    }

    @Test @TestLogging("_root:DEBUG")
    public void testForceDelete_LongRunningWatch() throws Exception {
        PutWatchResponse putResponse = watcherClient().preparePutWatch("_name").setSource(watchBuilder()
                .trigger(schedule(interval("3s")))
                .condition(scriptCondition(Script.inline("sleep 5000; return true")))
                .addAction("_action1", loggingAction("executed action: {{ctx.id}}")))
                .get();
        assertThat(putResponse.getId(), equalTo("_name"));
        Thread.sleep(5000);
        DeleteWatchResponse deleteWatchResponse = watcherClient().prepareDeleteWatch("_name").setForce(true).get();
        assertThat(deleteWatchResponse.isFound(), is(true));
        deleteWatchResponse = watcherClient().prepareDeleteWatch("_name").get();
        assertThat(deleteWatchResponse.isFound(), is(false));
        WatcherServiceResponse stopResponse = watcherClient().prepareWatchService().stop().get();
        assertThat(stopResponse.isAcknowledged(), is(true));
        ensureWatcherStopped();
        WatcherServiceResponse startResponse = watcherClient().prepareWatchService().start().get();
        assertThat(startResponse.isAcknowledged(), is(true));
        ensureWatcherStarted();
        deleteWatchResponse = watcherClient().prepareDeleteWatch("_name").get();
        assertThat(deleteWatchResponse.isFound(), is(false));
    }

}
