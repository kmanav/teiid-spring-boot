/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teiid.spring.example;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.apache.olingo.client.api.EdmEnabledODataClient;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.api.domain.ClientEntitySetIterator;
import org.apache.olingo.client.core.ODataClientFactory;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.constants.ODataServiceVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = { Application.class, TestConfiguration.class, RestTemplate.class }, webEnvironment = WebEnvironment.RANDOM_PORT)
public class TestExample {
    @Autowired
    TestRestTemplate web;

    @LocalServerPort
    private int port;

    @Test
    public void test() throws Exception{
        ResponseEntity<String> response = web.getForEntity("http://localhost:" + port + "/CUSTOMER", String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }

    static void olingoClient(String serviceRoot) throws Exception {
        EdmEnabledODataClient client = ODataClientFactory
                .getEdmEnabledClient(serviceRoot);
        ODataServiceVersion version = client.getServiceVersion();
        assertThat(ODataServiceVersion.V40, equalTo(version));
        Edm edm = client.getCachedEdm();
        EdmEntitySet es = edm.getEntityContainer().getEntitySet("CUSTOMER");
        assertThat("CUSTOMER", equalTo(es.getName()));

        URI customersUri = client.newURIBuilder(serviceRoot)
                .appendEntitySetSegment("CUSTOMER").filter("LASTNAME eq 'Smith'").build();

        ODataRetrieveResponse<ClientEntitySetIterator<ClientEntitySet, ClientEntity>> response = client
                .getRetrieveRequestFactory().getEntitySetIteratorRequest(customersUri).execute();

        ClientEntitySetIterator<ClientEntitySet, ClientEntity> iterator = response.getBody();
        assertTrue(iterator.hasNext());
        ClientEntity entity = iterator.next();
        assertNotNull(entity);
        assertThat(entity.getProperty("FIRSTNAME").getValue().asPrimitive().toValue(), equalTo("Joseph"));
    }

    @Test
    public void testMetadata() throws Exception {
        olingoClient("http://localhost:" + port);
        olingoClient("http://localhost:" + port+"/accounts");
    }

    @Test
    public void testRoot() throws Exception{
        ResponseEntity<String> response = web.getForEntity("http://localhost:" + port+"/", String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        response = web.getForEntity("http://localhost:" + port+"/$metadata", String.class);
        assertTrue(response.getBody().contains("http://localhost:" + port+"/static/org.teiid.v1.xml"));
    }

    @Test
    public void testWithModelName() throws Exception{
        ResponseEntity<String> response = web.getForEntity("http://localhost:" + port + "/accounts/CUSTOMER",
                String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }

    @Test
    public void testWithModelName2() throws Exception{
        ResponseEntity<String> response = web.getForEntity("http://localhost:" + port + "/accounts2/CUSTOMER",
                String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }

    @Test
    public void testOpenAPI() throws Exception{
        ResponseEntity<String> response = web.getForEntity("http://localhost:" + port + "/swagger.json",
                String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }

    @Test
    public void testStaticContent() throws Exception{
        ResponseEntity<String> response = web.getForEntity("http://localhost:" + port + "/static/org.teiid.v1.xml",
                String.class);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }
}
