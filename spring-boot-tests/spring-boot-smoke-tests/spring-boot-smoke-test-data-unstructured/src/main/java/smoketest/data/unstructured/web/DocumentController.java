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

package smoketest.data.unstructured.web;

import java.util.List;
import java.util.Map;

import smoketest.data.unstructured.service.DocumentNotFoundException;
import smoketest.data.unstructured.service.DocumentService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the unstructured document store API.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET    /api/collections}                              - List all collections</li>
 *   <li>{@code DELETE /api/collections/{collection}}                 - Delete an entire collection</li>
 *   <li>{@code POST   /api/collections/{collection}/documents}       - Create a document</li>
 *   <li>{@code GET    /api/collections/{collection}/documents}       - List documents in a collection</li>
 *   <li>{@code GET    /api/collections/{collection}/documents/{id}}  - Retrieve a document</li>
 *   <li>{@code PUT    /api/collections/{collection}/documents/{id}}  - Update a document</li>
 *   <li>{@code DELETE /api/collections/{collection}/documents/{id}}  - Delete a document</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class DocumentController {

	private final DocumentService documentService;

	public DocumentController(DocumentService documentService) {
		this.documentService = documentService;
	}

	@GetMapping("/collections")
	public List<String> listCollections() {
		return this.documentService.listCollections();
	}

	@DeleteMapping("/collections/{collection}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteCollection(@PathVariable String collection) {
		this.documentService.deleteCollection(collection);
	}

	@PostMapping("/collections/{collection}/documents")
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, Object> createDocument(@PathVariable String collection,
			@RequestBody Map<String, Object> data) {
		return this.documentService.storeDocument(collection, data);
	}

	@GetMapping("/collections/{collection}/documents")
	public List<Map<String, Object>> listDocuments(@PathVariable String collection) {
		return this.documentService.getDocumentsByCollection(collection);
	}

	@GetMapping("/collections/{collection}/documents/{id}")
	public ResponseEntity<Map<String, Object>> getDocument(@PathVariable String collection,
			@PathVariable Long id) {
		return this.documentService.getDocument(collection, id)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	@PutMapping("/collections/{collection}/documents/{id}")
	public ResponseEntity<Map<String, Object>> updateDocument(@PathVariable String collection,
			@PathVariable Long id, @RequestBody Map<String, Object> data) {
		try {
			return ResponseEntity.ok(this.documentService.updateDocument(collection, id, data));
		}
		catch (DocumentNotFoundException ex) {
			return ResponseEntity.notFound().build();
		}
	}

	@DeleteMapping("/collections/{collection}/documents/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteDocument(@PathVariable String collection, @PathVariable Long id) {
		this.documentService.deleteDocument(collection, id);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, String> handleIllegalArgument(IllegalArgumentException ex) {
		return Map.of("error", ex.getMessage());
	}

}
