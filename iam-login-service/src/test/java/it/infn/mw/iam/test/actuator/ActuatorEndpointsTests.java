/**
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2016-2018
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
package it.infn.mw.iam.test.actuator;

import static java.lang.String.format;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.google.common.collect.Sets;

import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.test.util.WithAnonymousUser;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class})
@WebAppConfiguration
@WithAnonymousUser
public class ActuatorEndpointsTests {

  private static final String ADMIN_USERNAME = "admin";
  private static final String ADMIN_ROLE = "ADMIN";

  private static final String USER_USERNAME = "test";
  private static final String USER_ROLE = "USER";

  private static final String STATUS_UP = "UP";
  private static final String STATUS_DOWN = "DOWN";

  private static final Set<String> SENSITIVE_ENDPOINTS = Sets.newHashSet("/metrics", "/configprops",
      "/env", "/mappings", "/flyway", "/autoconfig", "/beans", "/dump", "/trace");

  @Value("${health.mailProbe.path}")
  private String mailHealthEndpoint;

  @Value("${spring.mail.host}")
  private String mailHost;

  @Value("${spring.mail.port}")
  private Integer mailPort;
  
  @Autowired
  private WebApplicationContext context;
  
  private MockMvc mvc;
  
  

  @Before
  public void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context)
      .apply(springSecurity())
      .alwaysDo(print())
      .build();
  }
  

  @Test
  public void testHealthEndpoint() throws Exception {
    // @formatter:off
    mvc.perform(get("/health"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status", equalTo(STATUS_UP)));
    // @formatter:on
  }

  @Test
  @WithMockUser(username = USER_USERNAME, roles = {USER_ROLE})
  public void testHealthEndpointAsUser() throws Exception {
    // @formatter:off
    mvc.perform(get("/health"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status", equalTo(STATUS_UP)))
      .andExpect(jsonPath("$.diskSpace").doesNotExist())
      .andExpect(jsonPath("$.db").doesNotExist());
    // @formatter:on
  }

  @Test
  @WithMockUser(username = ADMIN_USERNAME, roles = {ADMIN_ROLE})
  public void testHealthEndpointAsAdmin() throws Exception {
    // @formatter:off
    mvc.perform(get("/health"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status", equalTo(STATUS_UP)))
      .andExpect(jsonPath("$.diskSpace.status", equalTo(STATUS_UP)))
      .andExpect(jsonPath("$.db.status", equalTo(STATUS_UP)))
      .andExpect(jsonPath("$.mail").doesNotExist());
    // @formatter:on
  }

  @Test
  public void testInfoEndpoint() throws Exception {
    // @formatter:off
    mvc.perform(get("/info"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.git", notNullValue()))
      .andExpect(jsonPath("$.app", notNullValue()))
      .andExpect(jsonPath("$.app.name", equalTo("IAM Login Service")));
    // @formatter:on
  }

  @Test
  @WithMockUser(username = USER_USERNAME, roles = {USER_ROLE})
  public void testInfoEndpointAsUser() throws Exception {
    // @formatter:off
    mvc.perform(get("/info"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.git", notNullValue()))
      .andExpect(jsonPath("$.app", notNullValue()))
      .andExpect(jsonPath("$.app.name", equalTo("IAM Login Service")));
    // @formatter:on
  }

  @Test
  @WithMockUser(username = ADMIN_USERNAME, roles = {ADMIN_ROLE})
  public void testInfoEndpointAsAdmin() throws Exception {
    // @formatter:off
    mvc.perform(get("/info"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.git", notNullValue()))
      .andExpect(jsonPath("$.app", notNullValue()))
      .andExpect(jsonPath("$.app.name", equalTo("IAM Login Service")));
    // @formatter:on
  }

  @Test
  public void testSensitiveEndpointsAsAnonymous() throws Exception {
    for (String endpoint : SENSITIVE_ENDPOINTS) {
      // @formatter:off
      mvc.perform(get(endpoint))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error", equalTo("unauthorized")))
        .andExpect(jsonPath("$.error_description").exists());
      // @formatter:on
    }
  }

  @Test
  @WithMockUser(username = USER_USERNAME, roles = {USER_ROLE})
  public void testSensitiveEndpointsAsUser() throws Exception {
    for (String endpoint : SENSITIVE_ENDPOINTS) {
      // @formatter:off
      mvc.perform(get(endpoint))
        .andExpect(status().isForbidden());
      // @formatter:on
    }
  }

  @Test
  @WithMockUser(username = ADMIN_USERNAME, roles = {ADMIN_ROLE})
  public void testSensitiveEndpointsAsAdmin() throws Exception {
    for (String endpoint : SENSITIVE_ENDPOINTS) {
      // @formatter:off
      mvc.perform(get(endpoint))
        .andExpect(status().isOk());
      // @formatter:on
    }
  }

  @Test
  public void testMailHealthEndpointWithoutSmtp() throws Exception {
    // @formatter:off
    mvc.perform(get(mailHealthEndpoint))
      .andExpect(status().isServiceUnavailable())
      .andExpect(jsonPath("$.status", equalTo(STATUS_DOWN)))
      .andExpect(jsonPath("$.mail").doesNotExist());
    // @formatter:on
  }

  @Test
  @WithMockUser(username = USER_USERNAME, roles = {USER_ROLE})
  public void testMailHealthEndpointWithoutSmtpAsUser() throws Exception {
    // @formatter:off
    mvc.perform(get(mailHealthEndpoint))
      .andExpect(status().isServiceUnavailable())
      .andExpect(jsonPath("$.status", equalTo(STATUS_DOWN)))
      .andExpect(jsonPath("$.mail").doesNotExist());
    // @formatter:on
  }

  @Test
  @WithMockUser(username = ADMIN_USERNAME, roles = {ADMIN_ROLE})
  public void testMailHealthEndpointWithoutSmtpAsAdmin() throws Exception {
    // @formatter:off
    mvc.perform(get(mailHealthEndpoint))
      .andExpect(status().isServiceUnavailable())
      .andExpect(jsonPath("$.status", equalTo(STATUS_DOWN)))
      .andExpect(jsonPath("$.mail.status", equalTo(STATUS_DOWN)))
      .andExpect(jsonPath("$.mail.location", equalTo(format("%s:%d", mailHost, mailPort))))
      .andExpect(jsonPath("$.mail.error").exists());
    // @formatter:on
  }

  
}
