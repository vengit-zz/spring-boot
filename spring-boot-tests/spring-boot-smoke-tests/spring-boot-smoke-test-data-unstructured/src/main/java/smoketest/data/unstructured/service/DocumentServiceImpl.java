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

package smoketest.data.unstructured.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import smoketest.data.unstructured.domain.Document;
import smoketest.data.unstructured.repository.DocumentRepository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link DocumentService} backed by a relational database.
 * Unstructured data is serialized as JSON strings and stored in a CLOB column,
 * enabling schema-free document storage on top of a traditional SQL database.
 */
@Service
@Transactional
public class DocumentServiceImpl implements DocumentService {

	private final DocumentRepository documentRepository;

	private final JdbcTemplate jdbcTemplate;

	private final ObjectMapper objectMapper;

	public DocumentServiceImpl(DocumentRepository documentRepository, JdbcTemplate jdbcTemplate,
			ObjectMapper objectMapper) {
		this.documentRepository = documentRepository;
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public Map<String, Object> storeDocument(String collectionName, Map<String, Object> data) {
		Document document = new Document();
		document.setCollectionName(collectionName);
		document.setData(toJson(data));
		LocalDateTime now = LocalDateTime.now();
		document.setCreatedAt(now);
		document.setUpdatedAt(now);
		Document saved = this.documentRepository.save(document);
		return toResponse(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Map<String, Object>> getDocument(String collectionName, Long id) {
		return this.documentRepository.findById(id)
			.filter((doc) -> collectionName.equals(doc.getCollectionName()))
			.map(this::toResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Map<String, Object>> getDocumentsByCollection(String collectionName) {
		return this.documentRepository.findByCollectionName(collectionName)
			.stream()
			.map(this::toResponse)
			.toList();
	}

	@Override
	public Map<String, Object> updateDocument(String collectionName, Long id, Map<String, Object> data) {
		Document document = this.documentRepository.findById(id)
			.filter((doc) -> collectionName.equals(doc.getCollectionName()))
			.orElseThrow(() -> new DocumentNotFoundException(id));
		document.setData(toJson(data));
		document.setUpdatedAt(LocalDateTime.now());
		Document updated = this.documentRepository.save(document);
		return toResponse(updated);
	}

	@Override
	public void deleteDocument(String collectionName, Long id) {
		this.documentRepository.findById(id)
			.filter((doc) -> collectionName.equals(doc.getCollectionName()))
			.ifPresent((doc) -> this.documentRepository.deleteById(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> listCollections() {
		return this.jdbcTemplate.queryForList(
				"SELECT DISTINCT COLLECTION_NAME FROM DOCUMENT ORDER BY COLLECTION_NAME", String.class);
	}

	@Override
	public void deleteCollection(String collectionName) {
		this.jdbcTemplate.update("DELETE FROM DOCUMENT WHERE COLLECTION_NAME = ?", collectionName);
	}

	private String toJson(Map<String, Object> data) {
		try {
			return this.objectMapper.writeValueAsString(data);
		}
		catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("Invalid data: cannot serialize to JSON", ex);
		}
	}

	private Map<String, Object> toResponse(Document document) {
		try {
			Map<String, Object> response = new LinkedHashMap<>();
			response.put("id", document.getId());
			response.put("collection", document.getCollectionName());
			response.put("data",
					this.objectMapper.readValue(document.getData(), new TypeReference<Map<String, Object>>() {
					}));
			response.put("createdAt", document.getCreatedAt());
			response.put("updatedAt", document.getUpdatedAt());
			return response;
		}
		catch (JsonProcessingException ex) {
			throw new IllegalStateException("Failed to deserialize stored document data", ex);
		}
	}

}
