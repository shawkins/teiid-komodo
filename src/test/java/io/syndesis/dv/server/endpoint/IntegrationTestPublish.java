/*
 * Copyright Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags and
 * the COPYRIGHT.txt file distributed with this work.
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

package io.syndesis.dv.server.endpoint;

import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import io.syndesis.dv.metadata.internal.DefaultMetadataInstance;
import io.syndesis.dv.model.ViewDefinition;
import io.syndesis.dv.openshift.SyndesisConnectionMonitor;
import io.syndesis.dv.openshift.SyndesisConnectionSynchronizer;
import io.syndesis.dv.openshift.TeiidOpenShiftClient;
import io.syndesis.dv.server.Application;
import io.syndesis.dv.server.AuthHandlingFilter.OAuthCredentials;
import io.syndesis.dv.server.CredentialsProvider;
import io.syndesis.dv.server.endpoint.IntegrationTestPublish.IntegrationTestConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {IntegrationTestConfiguration.class, Application.class})
@SuppressWarnings("nls")
public class IntegrationTestPublish {

    //inject simple auth bypass
    @TestConfiguration
    static class IntegrationTestConfiguration {
        @Primary
        @Bean
        public CredentialsProvider credentialsProvider() {
            return new CredentialsProvider() {

                @Override
                public OAuthCredentials getCredentials() {
                    return new OAuthCredentials("token", "user");
                }
            };
        }

        /* Stub out the connectivity to syndesis / openshift */
        @MockBean
        private SyndesisConnectionMonitor syndesisConnectionMonitor;
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SyndesisConnectionSynchronizer syndesisConnectionSynchronizer;
    @Autowired
    private TeiidOpenShiftClient teiidOpenShiftClient;

    @Autowired DataSource datasource;

    //for some reason dirtiescontext does not seem to work, so clear manually
    @After public void after() throws Exception {
        try (Connection c = datasource.getConnection();) {
            c.createStatement().execute("delete from data_virtualization");
        }
    }

    @Autowired
    DefaultMetadataInstance metadata;

    @Test
    public void testPublish() throws IOException {
        RestDataVirtualization rdv = new RestDataVirtualization();
        String dvName = "testPublish";
        rdv.setName(dvName);
        rdv.setDescription("description");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/v1/virtualizations", rdv, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        ViewDefinition vd = new ViewDefinition(dvName, "myview");
        vd.setComplete(true);
        vd.setDdl("create view myview as select 1 as col");
        vd.setUserDefined(true);

        restTemplate.exchange(
                "/v1/editors", HttpMethod.PUT,
                new HttpEntity<ViewDefinition>(vd), String.class);

    }
}
