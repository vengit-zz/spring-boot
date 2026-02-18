/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package smoketest.data.unstructured;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentControllerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void createAndRetrieveDocument() throws Exception {
		Map<String, Object> payload = Map.of("title", "Spring Boot Guide", "pages", 350);
		String json = this.objectMapper.writeValueAsString(payload);

		MvcResult createResult = this.mockMvc
			.perform(post("/api/collections/books/documents").contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").exists())
			.andExpect(jsonPath("$.collection").value("books"))
			.andExpect(jsonPath("$.data.title").value("Spring Boot Guide"))
			.andReturn();

		@SuppressWarnings("unchecked")
		Map<String, Object> created = this.objectMapper.readValue(createResult.getResponse().getContentAsString(),
				Map.class);
		Long id = ((Number) created.get("id")).longValue();

		this.mockMvc.perform(get("/api/collections/books/documents/" + id))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.title").value("Spring Boot Guide"))
			.andExpect(jsonPath("$.data.pages").value(350));
	}

	@Test
	void listDocumentsInCollection() throws Exception {
		String doc1 = this.objectMapper.writeValueAsString(Map.of("sku", "A001", "qty", 10));
		String doc2 = this.objectMapper.writeValueAsString(Map.of("sku", "B002", "qty", 5));

		this.mockMvc.perform(post("/api/collections/inventory/documents").contentType(MediaType.APPLICATION_JSON)
			.content(doc1)).andExpect(status().isCreated());
		this.mockMvc.perform(post("/api/collections/inventory/documents").contentType(MediaType.APPLICATION_JSON)
			.content(doc2)).andExpect(status().isCreated());

		this.mockMvc.perform(get("/api/collections/inventory/documents"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	void updateDocument() throws Exception {
		String original = this.objectMapper.writeValueAsString(Map.of("status", "draft"));

		MvcResult createResult = this.mockMvc
			.perform(post("/api/collections/articles/documents").contentType(MediaType.APPLICATION_JSON)
				.content(original))
			.andExpect(status().isCreated())
			.andReturn();

		@SuppressWarnings("unchecked")
		Map<String, Object> created = this.objectMapper.readValue(createResult.getResponse().getContentAsString(),
				Map.class);
		Long id = ((Number) created.get("id")).longValue();

		String updated = this.objectMapper.writeValueAsString(Map.of("status", "published", "views", 1000));
		this.mockMvc
			.perform(put("/api/collections/articles/documents/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(updated))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.status").value("published"))
			.andExpect(jsonPath("$.data.views").value(1000));
	}

	@Test
	void deleteDocument() throws Exception {
		String doc = this.objectMapper.writeValueAsString(Map.of("tmp", true));

		MvcResult createResult = this.mockMvc
			.perform(post("/api/collections/temp/documents").contentType(MediaType.APPLICATION_JSON).content(doc))
			.andExpect(status().isCreated())
			.andReturn();

		@SuppressWarnings("unchecked")
		Map<String, Object> created = this.objectMapper.readValue(createResult.getResponse().getContentAsString(),
				Map.class);
		Long id = ((Number) created.get("id")).longValue();

		this.mockMvc.perform(delete("/api/collections/temp/documents/" + id)).andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/collections/temp/documents/" + id)).andExpect(status().isNotFound());
	}

	@Test
	void listCollections() throws Exception {
		String doc = this.objectMapper.writeValueAsString(Map.of("k", "v"));

		this.mockMvc.perform(post("/api/collections/fruits/documents").contentType(MediaType.APPLICATION_JSON)
			.content(doc)).andExpect(status().isCreated());
		this.mockMvc.perform(post("/api/collections/veggies/documents").contentType(MediaType.APPLICATION_JSON)
			.content(doc)).andExpect(status().isCreated());

		this.mockMvc.perform(get("/api/collections"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasItem("fruits")))
			.andExpect(jsonPath("$", hasItem("veggies")));
	}

	@Test
	void deleteCollection() throws Exception {
		String doc = this.objectMapper.writeValueAsString(Map.of("k", "v"));

		this.mockMvc.perform(post("/api/collections/scratch/documents").contentType(MediaType.APPLICATION_JSON)
			.content(doc)).andExpect(status().isCreated());
		this.mockMvc.perform(post("/api/collections/scratch/documents").contentType(MediaType.APPLICATION_JSON)
			.content(doc)).andExpect(status().isCreated());

		this.mockMvc.perform(delete("/api/collections/scratch")).andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/collections/scratch/documents"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void getDocumentReturnsNotFoundForMissingId() throws Exception {
		this.mockMvc.perform(get("/api/collections/unknown/documents/999999")).andExpect(status().isNotFound());
	}

	@Test
	void updateDocumentReturnsNotFoundForMissingId() throws Exception {
		String doc = this.objectMapper.writeValueAsString(Map.of("k", "v"));
		this.mockMvc
			.perform(put("/api/collections/unknown/documents/999999").contentType(MediaType.APPLICATION_JSON)
				.content(doc))
			.andExpect(status().isNotFound());
	}

}
