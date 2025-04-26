/****************************************************************************
 * Copyright 2022 ecsec GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************/

package eu.gaiax.notarization.scheduler;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;


/**
 *
 * @author Florian Otto
 */
@ApplicationScoped
public class RequestProcessingTriggers {

    public static Logger log = Logger.getLogger(RequestProcessingTriggers.class);

    @RestClient
    RequestProcessingRoutines requestProcessing;

    @Inject MeterRegistry registry;

    @Scheduled(cron = "{cron.prune-terminated}", concurrentExecution = ConcurrentExecution.SKIP)
    public void pruneTerminatedSesssions(){
        log.debug("Sending trigger to prune terminated sessions.");
        try {
            requestProcessing.pruneTerminatedSessions();
            registry.counter("schedule.prune-terminated.success").increment();
        } catch(ProcessingException ex) {
            log.warn("Could not remotely trigger pruning of terminated sessions", ex);
            registry.counter("schedule.prune-terminated.failure").increment();
        }
    }

    @Scheduled(cron = "{cron.prune-timeout}", concurrentExecution = ConcurrentExecution.SKIP)
    public void pruneTimeoutSessions(){
        log.debug("Sending trigger to prune timed out sessions.");
        try {
            requestProcessing.pruneTimeoutSessions();
            registry.counter("schedule.prune-timeout.success").increment();
        } catch(ProcessingException ex) {
            log.warn("Could not remotely trigger pruning of timeout sessions", ex);
            registry.counter("schedule.prune-timeout.failure").increment();
        }
    }

    @Scheduled(cron = "{cron.prune-submission-timeout}", concurrentExecution = ConcurrentExecution.SKIP)
    public void pruneSubmissTimeoutSessions(){
        log.debug("Sending trigger to prune sessions without submission within timeout.");
        try {
            requestProcessing.pruneSubmissTimeoutSessions();
            registry.counter("schedule.prune-submission-timeout.success").increment();
        } catch(ProcessingException ex) {
            log.warn("Could not remotely trigger pruning of timeout sessions", ex);
            registry.counter("schedule.prune-submission-timeout.failure").increment();
        }
    }

    @Scheduled(cron = "{cron.prune-http-audit-logs}", concurrentExecution = ConcurrentExecution.SKIP)
    public void pruneHttpAuditLogs(){
        log.debug("Sending trigger to prune http audit logs.");
        try {
            requestProcessing.pruneAuditLogs();
            registry.counter("schedule.prune-audit-logs.success").increment();
        } catch(ProcessingException ex) {
            log.warn("Could not remotely trigger pruning of http audit logs.", ex);
            registry.counter("schedule.prune-audit-logs.failure").increment();
        }
    }

}
