package gov.cdc.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { 
		"logging.fluentd.host=fluentd", 
		"logging.fluentd.port=24224",
		"proxy.hostname=localhost",
		"security.oauth2.resource.user-info-uri=",
		"security.oauth2.protected=",
		"security.oauth2.client.client-id=",
		"security.oauth2.client.client-secret=",
		"ssl.verifying.disable=false"})
@AutoConfigureMockMvc
public class CombinerApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;
	@Autowired
	private MockMvc mvc;
	private JacksonTester<JsonNode> json;
	private String baseUrlPath = "/api/1.0/";
	private String config;
	
	@Before
	public void setup() {
		ObjectMapper objectMapper = new ObjectMapper();
		JacksonTester.initFields(this, objectMapper);
		
		// Define the object URL
		System.setProperty("OBJECT_URL", "http://fdns-ms-object:8083");
	}
	
	@Test
	public void indexPage() {
		ResponseEntity<String> response = this.restTemplate.getForEntity("/", String.class);
		assertThat(response.getStatusCodeValue()).isEqualTo(200);
		assertThat(response.getBody(), CoreMatchers.containsString("FDNS Combiner Microservice"));
	}
	
	@Test
	public void indexAPI() {
		ResponseEntity<String> response = this.restTemplate.getForEntity(baseUrlPath, String.class);
		assertThat(response.getStatusCodeValue()).isEqualTo(200);
		assertThat(response.getBody(), CoreMatchers.containsString("version"));
	}
	
	@Test
	public void createAndUpdateConfiguration() throws Exception {
		int nbOfCalls = 2;
		config = UUID.randomUUID().toString();
		
		HttpMethod[] methods = { HttpMethod.POST, HttpMethod.PUT };
		for (HttpMethod httpMethod : methods) {
			for (int i = 0; i < nbOfCalls; i++) {
				ResponseEntity<JsonNode> response = restTemplate.exchange(
						baseUrlPath + "/config/{config}", 
						httpMethod, 
						getEntity(getResourceAsString("junit/config/case-id-extractor.json"), MediaType.APPLICATION_JSON),
						JsonNode.class,
						config);
				JsonContent<JsonNode> body = this.json.write(response.getBody());
				assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
				assertThat(body).hasJsonPathValue("@.success");
				assertThat(body).extractingJsonPathBooleanValue("@.success").isEqualTo(true);
				assertThat(body).hasJsonPathValue("@.config");
				assertThat(body).extractingJsonPathStringValue("@.config").isEqualTo(config);
			}
		}
	}
	
	@Test
	public void getRuleSet() throws Exception {
		// Be sure that we created the configuration
		createAndUpdateConfiguration();
		
		ResponseEntity<JsonNode> response = restTemplate.exchange(
				baseUrlPath + "/config/{config}", 
				HttpMethod.GET, 
				null,
				JsonNode.class,
				config);
		JsonContent<JsonNode> body = this.json.write(response.getBody());
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(body).hasJsonPathValue("@._id");
		assertThat(body).extractingJsonPathStringValue("@._id").isEqualTo(config);
	}

	@Test
	public void flattenJsonToJson() throws Exception {
		createAndUpdateConfiguration();
		
		MockMultipartFile file = new MockMultipartFile("file", "hl7.json", "application/json", getResourceAsString("junit/hl7/hl7.json").getBytes());
		MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.fileUpload(baseUrlPath + "/json/flatten");
		mvc.perform(builder.file(file)).andExpect(status().isOk());
	}
	
	@Test
	public void flattenJsonToCsv() throws Exception {
		createAndUpdateConfiguration();
		
		MockMultipartFile file = new MockMultipartFile("file", "hl7.json", "application/json", getResourceAsString("junit/hl7/hl7.json").getBytes());
		MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.fileUpload(baseUrlPath + "/csv/flatten");
		mvc.perform(builder.file(file)).andExpect(status().isOk());
	}
	
	@Test
	public void flattenJsonToXlsx() throws Exception {
		createAndUpdateConfiguration();
		
		MockMultipartFile file = new MockMultipartFile("file", "hl7.json", "application/json", getResourceAsString("junit/hl7/hl7.json").getBytes());
		MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.fileUpload(baseUrlPath + "/xlsx/flatten");
		mvc.perform(builder.file(file)).andExpect(status().isOk());
	}

	@Test
	public void transformJsonToCsv() throws Exception {
		createAndUpdateConfiguration();
		
		MockMultipartFile file = new MockMultipartFile("file", "hl7.json", "application/json", getResourceAsString("junit/hl7/hl7.json").getBytes());
		MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.fileUpload(baseUrlPath + "/csv/" + config);
		mvc.perform(builder.file(file)).andExpect(status().isOk());
	}
	
	@Test
	public void transformJsonToXlsx() throws Exception {
		createAndUpdateConfiguration();
		
		MockMultipartFile file = new MockMultipartFile("file", "hl7.json", "application/json", getResourceAsString("junit/hl7/hl7.json").getBytes());
		MockMultipartHttpServletRequestBuilder builder = MockMvcRequestBuilders.fileUpload(baseUrlPath + "/xlsx/" + config);
		mvc.perform(builder.file(file)).andExpect(status().isOk());
	}

	private String getResourceAsString(String path) throws IOException {
		return IOUtils.toString(getResource(path));
	}

	private InputStream getResource(String path) throws IOException {
		return getClass().getClassLoader().getResourceAsStream(path);
	}

	private HttpEntity<String> getEntity(String content, MediaType mediaType) throws IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(mediaType);
		HttpEntity<String> entity = new HttpEntity<String>(content, headers);
		return entity;
	}
	
}
