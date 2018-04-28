package it.infn.mw.iam.test.api.account.search;

import static it.infn.mw.iam.api.account.search.AccountSearchController.ACCOUNT_SEARCH_ENDPOINT;
import static it.infn.mw.iam.api.account.search.AccountSearchController.ITEMS_PER_PAGE;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import it.infn.mw.iam.IamLoginService;
import it.infn.mw.iam.api.common.ListResponseDTO;
import it.infn.mw.iam.api.common.OffsetPageable;
import it.infn.mw.iam.api.scim.converter.UserConverter;
import it.infn.mw.iam.api.scim.model.ScimUser;
import it.infn.mw.iam.persistence.model.IamAccount;
import it.infn.mw.iam.persistence.repository.IamAccountRepository;
import it.infn.mw.iam.test.core.CoreControllerTestSupport;
import it.infn.mw.iam.test.util.WithMockOAuthUser;
import it.infn.mw.iam.test.util.oauth.MockOAuth2Filter;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {IamLoginService.class, CoreControllerTestSupport.class})
@WebAppConfiguration
public class AccountSearchControllerTest {

  public static final String APPLICATION_JSON_CONTENT_TYPE = "application/json";

  @Autowired
  private WebApplicationContext context;

  private MockMvc mvc;

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private MockOAuth2Filter mockOAuth2Filter;

  @Autowired
  private IamAccountRepository accountRepository;

  @Autowired
  private UserConverter userConverter;

  @Before
  public void setup() {
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mockOAuth2Filter.cleanupSecurityContext();
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).alwaysDo(print())
        .build();
  }

  @After
  public void teardown() {
    mockOAuth2Filter.cleanupSecurityContext();
  }

  @Test
  @WithMockOAuthUser(user = "admin", authorities = {"ROLE_ADMIN"})
  public void getFirstPageOfAllUsers() throws JsonParseException, JsonMappingException,
      UnsupportedEncodingException, IOException, Exception {

    long expectedSize = accountRepository.count();

    ListResponseDTO<ScimUser> response = mapper.readValue(
        mvc.perform(get(ACCOUNT_SEARCH_ENDPOINT).contentType(APPLICATION_JSON_CONTENT_TYPE))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
        new TypeReference<ListResponseDTO<ScimUser>>() {});
    assertThat(response.getTotalResults(), equalTo(expectedSize));
    assertThat(response.getResources().size(), equalTo(ITEMS_PER_PAGE));
    assertThat(response.getStartIndex(), equalTo(1));
    assertThat(response.getItemsPerPage(), equalTo(ITEMS_PER_PAGE));
  }

  @Test
  @WithMockOAuthUser(user = "admin", authorities = {"ROLE_ADMIN"})
  public void getSecondPageOfAllUsers() throws JsonParseException, JsonMappingException,
      UnsupportedEncodingException, IOException, Exception {

    long expectedSize = accountRepository.count();

    ListResponseDTO<ScimUser> response = mapper.readValue(
        mvc.perform(get(ACCOUNT_SEARCH_ENDPOINT).contentType(APPLICATION_JSON_CONTENT_TYPE)
            .param("startIndex", String.valueOf(ITEMS_PER_PAGE))).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(),
        new TypeReference<ListResponseDTO<ScimUser>>() {});
    assertThat(response.getTotalResults(), equalTo(expectedSize));
    assertThat(response.getResources().size(), equalTo(ITEMS_PER_PAGE));
    assertThat(response.getStartIndex(), equalTo(ITEMS_PER_PAGE));
    assertThat(response.getItemsPerPage(), equalTo(ITEMS_PER_PAGE));
  }

  @Test
  @WithMockOAuthUser(user = "admin", authorities = {"ROLE_ADMIN"})
  public void getUsersWithCustomStartIndexAndCount() throws JsonParseException,
      JsonMappingException, UnsupportedEncodingException, IOException, Exception {

    long expectedSize = accountRepository.count();
    int startIndex = 3;
    int count = 2;

    ListResponseDTO<ScimUser> response = mapper.readValue(
        mvc.perform(get(ACCOUNT_SEARCH_ENDPOINT).contentType(APPLICATION_JSON_CONTENT_TYPE)
            .param("startIndex", String.valueOf(startIndex)).param("count", String.valueOf(count)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
        new TypeReference<ListResponseDTO<ScimUser>>() {});
    assertThat(response.getTotalResults(), equalTo(expectedSize));
    assertThat(response.getResources().size(), equalTo(count));
    assertThat(response.getStartIndex(), equalTo(startIndex));
    assertThat(response.getItemsPerPage(), equalTo(count));
  }

  @Test
  @WithMockOAuthUser(user = "admin", authorities = {"ROLE_ADMIN"})
  public void getCountOfAllUsers() throws JsonParseException, JsonMappingException,
      UnsupportedEncodingException, IOException, Exception {

    long expectedSize = accountRepository.count();

    ListResponseDTO<ScimUser> response = mapper.readValue(mvc
        .perform(get(ACCOUNT_SEARCH_ENDPOINT).contentType(APPLICATION_JSON_CONTENT_TYPE)
            .param("count", "0"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
        new TypeReference<ListResponseDTO<ScimUser>>() {});
    assertThat(response.getTotalResults(), equalTo(expectedSize));
    assertThat(response.getResources(), equalTo(null));
    assertThat(response.getStartIndex(), equalTo(null));
    assertThat(response.getItemsPerPage(), equalTo(null));
  }

  @Test
  @WithMockOAuthUser(user = "admin", authorities = {"ROLE_ADMIN"})
  public void getFirstFilteredPageOfUsers() throws JsonParseException, JsonMappingException,
      UnsupportedEncodingException, IOException, Exception {

    OffsetPageable op = new OffsetPageable(0, 10);
    Page<IamAccount> page = accountRepository.findByFilter("%admin%", op);

    ListResponseDTO<ScimUser> response = mapper.readValue(mvc
        .perform(get(ACCOUNT_SEARCH_ENDPOINT).contentType(APPLICATION_JSON_CONTENT_TYPE)
            .param("filter", "admin"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
        new TypeReference<ListResponseDTO<ScimUser>>() {});

    assertThat(response.getResources().size(), equalTo(2));
    assertThat(response.getStartIndex(), equalTo(1));
    assertThat(response.getItemsPerPage(), equalTo(2));

    List<ScimUser> expectedUsers = Lists.newArrayList();
    page.getContent().forEach(u -> expectedUsers.add(userConverter.dtoFromEntity(u)));
    assertThat(response.getResources().containsAll(expectedUsers), equalTo(true));
  }

  @Test
  @WithMockOAuthUser(user = "admin", authorities = {"ROLE_ADMIN"})
  public void getCountOfFilteredUsers() throws JsonParseException, JsonMappingException,
      UnsupportedEncodingException, IOException, Exception {

    long expectedSize = accountRepository.countByFilter("%admin%");

    ListResponseDTO<ScimUser> response =
        mapper.readValue(
            mvc.perform(get(ACCOUNT_SEARCH_ENDPOINT).contentType(APPLICATION_JSON_CONTENT_TYPE)
                .param("count", "0").param("filter", "admin")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(),
            new TypeReference<ListResponseDTO<ScimUser>>() {});
    assertThat(response.getTotalResults(), equalTo(expectedSize));
    assertThat(response.getResources(), equalTo(null));
    assertThat(response.getStartIndex(), equalTo(null));
    assertThat(response.getItemsPerPage(), equalTo(null));
  }
}
