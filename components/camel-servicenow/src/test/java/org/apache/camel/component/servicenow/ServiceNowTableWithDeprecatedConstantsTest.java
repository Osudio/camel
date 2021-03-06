/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.camel.component.servicenow;

import java.util.List;

import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.servicenow.model.Incident;
import org.junit.Test;

public class ServiceNowTableWithDeprecatedConstantsTest extends ServiceNowTestSupport {

    @Test
    public void testRetrieveSome() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:servicenow");
        mock.expectedMessageCount(1);

        template().sendBodyAndHeaders(
            "direct:servicenow",
            null,
            new KVBuilder()
                .put(ServiceNowConstants.RESOURCE, "table")
                .put(ServiceNowConstants.ACTION, ServiceNowConstants.ACTION_RETRIEVE)
                .put(ServiceNowConstants.SYSPARM_LIMIT, "10")
                .put(ServiceNowConstants.TABLE, "incident")
                .build()
        );

        mock.assertIsSatisfied();

        Exchange exchange = mock.getExchanges().get(0);
        List<Incident> items = exchange.getIn().getBody(List.class);

        assertNotNull(items);
        assertTrue(items.size() <= 10);
    }

    @Test
    public void testRetrieveSomeWithDefaults() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:servicenow-defaults");
        mock.expectedMessageCount(1);

        template().sendBodyAndHeaders(
            "direct:servicenow-defaults",
            null,
            new KVBuilder()
                .put(ServiceNowConstants.ACTION, ServiceNowConstants.ACTION_RETRIEVE)
                .put(ServiceNowConstants.SYSPARM_LIMIT, "10")
                .build()
        );

        mock.assertIsSatisfied();

        Exchange exchange = mock.getExchanges().get(0);
        List<Incident> items = exchange.getIn().getBody(List.class);

        assertNotNull(items);
        assertTrue(items.size() <= 10);
    }

    @Test
    public void testIncidentWorkflow() throws Exception {

        Incident incident = null;
        String sysId;
        String number;
        MockEndpoint mock = getMockEndpoint("mock:servicenow");

        // ************************
        // Create incident
        // ************************

        {
            mock.reset();
            mock.expectedMessageCount(1);

            incident = new Incident();
            incident.setDescription("my incident");
            incident.setShortDescription("An incident");
            incident.setSeverity(1);
            incident.setImpact(1);

            template().sendBodyAndHeaders(
                "direct:servicenow",
                incident,
                new KVBuilder()
                    .put(ServiceNowConstants.RESOURCE, "table")
                    .put(ServiceNowConstants.ACTION, ServiceNowConstants.ACTION_CREATE)
                    .put(ServiceNowConstants.TABLE, "incident")
                    .build()
            );

            mock.assertIsSatisfied();

            incident = mock.getExchanges().get(0).getIn().getBody(Incident.class);
            sysId = incident.getId();
            number = incident.getNumber();

            LOGGER.info("****************************************************");
            LOGGER.info("* Incident created");
            LOGGER.info("*  sysid  = {}", sysId);
            LOGGER.info("*  number = {}", number);
            LOGGER.info("****************************************************");
        }

        // ************************
        // Search for the incident
        // ************************

        {
            mock.reset();
            mock.expectedMessageCount(1);

            template().sendBodyAndHeaders(
                "direct:servicenow",
                null,
                new KVBuilder()
                    .put(ServiceNowConstants.RESOURCE, "table")
                    .put(ServiceNowConstants.ACTION, ServiceNowConstants.ACTION_RETRIEVE)
                    .put(ServiceNowConstants.TABLE, "incident")
                    .put(ServiceNowConstants.SYSPARM_QUERY, "number=" + number)
                    .build()
            );

            mock.assertIsSatisfied();

            List<Incident> incidents = mock.getExchanges().get(0).getIn().getBody(List.class);
            assertEquals(1, incidents.size());
            assertEquals(number, incidents.get(0).getNumber());
            assertEquals(sysId, incidents.get(0).getId());
        }

        // ************************
        // Modify the incident
        // ************************

        {
            mock.reset();
            mock.expectedMessageCount(1);

            incident = new Incident();
            incident.setDescription("my incident");
            incident.setShortDescription("The incident");
            incident.setSeverity(2);
            incident.setImpact(3);

            template().sendBodyAndHeaders(
                "direct:servicenow",
                incident,
                new KVBuilder()
                    .put(ServiceNowConstants.RESOURCE, "table")
                    .put(ServiceNowConstants.ACTION, ServiceNowConstants.ACTION_MODIFY)
                    .put(ServiceNowConstants.TABLE, "incident")
                    .put(ServiceNowConstants.SYSPARM_ID, sysId)
                    .build()
            );

            mock.assertIsSatisfied();

            incident = mock.getExchanges().get(0).getIn().getBody(Incident.class);
            assertEquals(number, incident.getNumber());
            assertEquals(2, incident.getSeverity());
            assertEquals(3, incident.getImpact());
            assertEquals("The incident", incident.getShortDescription());
        }

        // ************************
        // Retrieve it via query
        // ************************

        {
            mock.reset();
            mock.expectedMessageCount(1);

            template().sendBodyAndHeaders(
                "direct:servicenow",
                null,
                new KVBuilder()
                    .put(ServiceNowConstants.RESOURCE, "table")
                    .put(ServiceNowConstants.ACTION, ServiceNowConstants.ACTION_RETRIEVE)
                    .put(ServiceNowConstants.TABLE, "incident")
                    .put(ServiceNowConstants.SYSPARM_QUERY, "number=" + number)
                    .build()
            );

            mock.assertIsSatisfied();

            List<Incident> incidents = mock.getExchanges().get(0).getIn().getBody(List.class);
            assertEquals(1, incidents.size());
            assertEquals(number, incidents.get(0).getNumber());
            assertEquals(sysId, incidents.get(0).getId());
            assertEquals(2, incidents.get(0).getSeverity());
            assertEquals(3, incidents.get(0).getImpact());
            assertEquals("The incident", incidents.get(0).getShortDescription());
        }

        // ************************
        // Retrieve by sys id
        // ************************

        {
            mock.reset();
            mock.expectedMessageCount(1);

            template().sendBodyAndHeaders(
                "direct:servicenow",
                null,
                new KVBuilder()
                    .put(ServiceNowConstants.RESOURCE, "table")
                    .put(ServiceNowConstants.ACTION, ServiceNowConstants.ACTION_RETRIEVE)
                    .put(ServiceNowConstants.TABLE, "incident")
                    .put(ServiceNowConstants.SYSPARM_ID, sysId)
                    .build()
            );

            mock.assertIsSatisfied();

            incident = mock.getExchanges().get(0).getIn().getBody(Incident.class);
            assertEquals(2, incident.getSeverity());
            assertEquals(3, incident.getImpact());
            assertEquals("The incident", incident.getShortDescription());
            assertEquals(number, incident.getNumber());
        }

        // ************************
        // Delete it
        // ************************

        {
            mock.reset();
            mock.expectedMessageCount(1);

            template().sendBodyAndHeaders(
                "direct:servicenow",
                null,
                new KVBuilder()
                    .put(ServiceNowConstants.RESOURCE, "table")
                    .put(ServiceNowConstants.ACTION, ServiceNowConstants.ACTION_DELETE)
                    .put(ServiceNowConstants.TABLE, "incident")
                    .put(ServiceNowConstants.SYSPARM_ID, sysId)
                    .build()
            );

            mock.assertIsSatisfied();
        }

        // ************************
        // Retrieve by id, should fail
        // ************************

        {
            LOGGER.info("Find the record {}, should fail", sysId);

            try {
                template().sendBodyAndHeaders(
                    "direct:servicenow",
                    null,
                    new KVBuilder()
                        .put(ServiceNowConstants.RESOURCE, "table")
                        .put(ServiceNowConstants.ACTION, ServiceNowConstants.ACTION_RETRIEVE)
                        .put(ServiceNowConstants.SYSPARM_ID, sysId)
                        .put(ServiceNowConstants.TABLE, "incident")
                        .build()
                );

                fail("Record " + number + " should have been deleted");
            } catch (CamelExecutionException e) {
                assertTrue(e.getCause() instanceof ServiceNowException);
                // we are good
            }
        }
    }

    // *************************************************************************
    //
    // *************************************************************************

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                from("direct:servicenow")
                    .to("servicenow:{{env:SERVICENOW_INSTANCE}}"
                        + "?userName={{env:SERVICENOW_USERNAME}}"
                        + "&password={{env:SERVICENOW_PASSWORD}}"
                        //+ "&oauthClientId={{env:SERVICENOW_OAUTH2_CLIENT_ID}}"
                        //+ "&oauthClientSecret={{env:SERVICENOW_OAUTH2_CLIENT_SECRET}}"
                        + "&model.incident=org.apache.camel.component.servicenow.model.Incident")
                    .to("log:org.apache.camel.component.servicenow?level=INFO&showAll=true")
                    .to("mock:servicenow");
                from("direct:servicenow-defaults")
                    .to("servicenow:{{env:SERVICENOW_INSTANCE}}"
                        + "?userName={{env:SERVICENOW_USERNAME}}"
                        + "&password={{env:SERVICENOW_PASSWORD}}"
                        //+ "&oauthClientId={{env:SERVICENOW_OAUTH2_CLIENT_ID}}"
                        //+ "&oauthClientSecret={{env:SERVICENOW_OAUTH2_CLIENT_SECRET}}"
                        + "&model.incident=org.apache.camel.component.servicenow.model.Incident"
                        + "&resource=table"
                        + "&table=incident")
                    .to("log:org.apache.camel.component.servicenow?level=INFO&showAll=true")
                    .to("mock:servicenow-defaults");
            }
        };
    }
}
