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

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;


/**
 *
 * @author Florian Otto
 */
@ApplicationScoped
public class ProfileTriggers {

    public static Logger log = Logger.getLogger(ProfileTriggers.class);

    @RestClient
    ProfileRoutines profileService;

    @Scheduled(cron = "{cron.profile-request-outstanding-dids}", concurrentExecution = ConcurrentExecution.SKIP)
    public void requestOutStandingDids(){
        log.info("Sending trigger to request outstanding dids.");
        profileService.requestOutStandingDids();
    }

}
