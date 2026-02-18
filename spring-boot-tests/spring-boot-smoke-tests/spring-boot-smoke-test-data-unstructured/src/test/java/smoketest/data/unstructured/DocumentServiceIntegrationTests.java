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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import smoketest.data.unstructured.service.DocumentService;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DocumentServiceIntegrationTests {

	@Autowired
	private DocumentService documentService;

	@Test
	void storeAndRetrieveDocument() {
		Map<String, Object> data = Map.of("name", "Alice", "age", 30, "email", "alice@example.com");
		Map<String, Object> stored = this.documentService.storeDocument("users", data);

		assertThat(stored).containsKey("id");
		assertThat(stored.get("collection")).isEqualTo("users");
		assertThat(stored).containsKey("createdAt");
		assertThat(stored).containsKey("updatedAt");

		Long id = ((Number) stored.get("id")).longValue();
		Optional<Map<String, Object>> retrieved = this.documentService.getDocument("users", id);

		assertThat(retrieved).isPresent();
		@SuppressWarnings("unchecked")
		Map<String, Object> docData = (Map<String, Object>) retrieved.get().get("data");
		assertThat(docData.get("name")).isEqualTo("Alice");
		assertThat(docData.get("email")).isEqualTo("alice@example.com");
	}

	@Test
	void getDocumentNotFoundInWrongCollection() {
		Map<String, Object> stored = this.documentService.storeDocument("col-a", Map.of("k", "v"));
		Long id = ((Number) stored.get("id")).longValue();

		Optional<Map<String, Object>> result = this.documentService.getDocument("col-b", id);
		assertThat(result).isEmpty();
	}

	@Test
	void listDocumentsByCollection() {
		this.documentService.storeDocument("products", Map.of("name", "Widget", "price", 9.99));
		this.documentService.storeDocument("products", Map.of("name", "Gadget", "price", 24.99));
		this.documentService.storeDocument("orders", Map.of("orderId", "ORD-001", "status", "pending"));

		List<Map<String, Object>> products = this.documentService.getDocumentsByCollection("products");
		assertThat(products).hasSize(2);

		List<Map<String, Object>> orders = this.documentService.getDocumentsByCollection("orders");
		assertThat(orders).hasSize(1);
	}

	@Test
	void updateDocument() {
		Map<String, Object> stored = this.documentService.storeDocument("items", Map.of("status", "pending"));
		Long id = ((Number) stored.get("id")).longValue();

		Map<String, Object> updated = this.documentService.updateDocument("items", id,
				Map.of("status", "active", "priority", "high"));

		@SuppressWarnings("unchecked")
		Map<String, Object> updatedData = (Map<String, Object>) updated.get("data");
		assertThat(updatedData.get("status")).isEqualTo("active");
		assertThat(updatedData.get("priority")).isEqualTo("high");
	}

	@Test
	void deleteDocument() {
		Map<String, Object> stored = this.documentService.storeDocument("temp", Map.of("value", "x"));
		Long id = ((Number) stored.get("id")).longValue();

		this.documentService.deleteDocument("temp", id);

		Optional<Map<String, Object>> retrieved = this.documentService.getDocument("temp", id);
		assertThat(retrieved).isEmpty();
	}

	@Test
	void deleteCollection() {
		this.documentService.storeDocument("ephemeral", Map.of("a", 1));
		this.documentService.storeDocument("ephemeral", Map.of("b", 2));
		this.documentService.storeDocument("persistent", Map.of("c", 3));

		this.documentService.deleteCollection("ephemeral");

		assertThat(this.documentService.getDocumentsByCollection("ephemeral")).isEmpty();
		assertThat(this.documentService.getDocumentsByCollection("persistent")).hasSize(1);
	}

	@Test
	void listCollections() {
		this.documentService.storeDocument("alpha", Map.of("k", "v"));
		this.documentService.storeDocument("beta", Map.of("k", "v"));
		this.documentService.storeDocument("gamma", Map.of("k", "v"));

		List<String> collections = this.documentService.listCollections();
		assertThat(collections).contains("alpha", "beta", "gamma");
	}

	@Test
	void documentsWithNestedStructure() {
		Map<String, Object> nested = Map.of("user", Map.of("name", "Bob", "address",
				Map.of("city", "Springfield", "zip", "12345")), "tags", List.of("java", "spring", "nosql"));

		Map<String, Object> stored = this.documentService.storeDocument("complex", nested);
		Long id = ((Number) stored.get("id")).longValue();

		Optional<Map<String, Object>> retrieved = this.documentService.getDocument("complex", id);
		assertThat(retrieved).isPresent();

		@SuppressWarnings("unchecked")
		Map<String, Object> data = (Map<String, Object>) retrieved.get().get("data");
		assertThat(data).containsKey("user");
		assertThat(data).containsKey("tags");
	}

}
