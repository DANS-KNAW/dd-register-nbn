/*
 * Copyright (C) 2026 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
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
 */

package nl.knaw.dans.registernbn;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.util.ClientProxyBuilder;
import nl.knaw.dans.lib.util.inbox.Inbox;
import nl.knaw.dans.registernbn.client.GmhClient;
import nl.knaw.dans.registernbn.client.GmhClientImpl;
import nl.knaw.dans.registernbn.config.DdRegisterNbnConfig;
import nl.knaw.dans.registernbn.core.NbnRegistrationTaskFactory;
import nl.knaw.dans.registernbn.core.PropertiesFileFilter;
import nl.knaw.dans.registernbn.core.CreationTimeComparator;

@Slf4j
public class DdRegisterNbnApplication extends Application<DdRegisterNbnConfig> {

    public static void main(final String[] args) throws Exception {
        new DdRegisterNbnApplication().run(args);
    }

    @Override
    public String getName() {
        return "Dd Register Nbn";
    }

    @Override
    public void initialize(final Bootstrap<DdRegisterNbnConfig> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final DdRegisterNbnConfig configuration, final Environment environment) {
        environment.lifecycle().manage(
            Inbox.builder()
                .inbox(configuration.getNbnRegistration().getInbox().getPath())
                .interval(Math.toIntExact(configuration.getNbnRegistration().getInbox().getPollingInterval().toMilliseconds()))
                .executorService(environment.lifecycle().executorService("nbn-registration-inbox").maxThreads(1).minThreads(1).build())
                .inboxItemComparator(CreationTimeComparator.getInstance())
                .fileFilter(new PropertiesFileFilter())
                .taskFactory(NbnRegistrationTaskFactory.builder()
                    .gmhClient(createGmhClient(configuration))
                    .outboxProcessed(configuration.getNbnRegistration().getOutbox().getProcessed())
                    .outboxFailed(configuration.getNbnRegistration().getOutbox().getFailed())
                    .build())
                .build());
    }

    private GmhClient createGmhClient(DdRegisterNbnConfig configuration) {
        return new GmhClientImpl(new ClientProxyBuilder<nl.knaw.dans.gmh.client.invoker.ApiClient, nl.knaw.dans.gmh.client.resources.UrnNbnIdentifierApi>()
            .apiClient(new nl.knaw.dans.gmh.client.invoker.ApiClient().setBearerToken(configuration.getNbnRegistration().getGmh().getToken()))
            .basePath(configuration.getNbnRegistration().getGmh().getUrl())
            .httpClient(configuration.getNbnRegistration().getGmh().getHttpClient())
            .defaultApiCtor(nl.knaw.dans.gmh.client.resources.UrnNbnIdentifierApi::new)
            .build());
    }
}
