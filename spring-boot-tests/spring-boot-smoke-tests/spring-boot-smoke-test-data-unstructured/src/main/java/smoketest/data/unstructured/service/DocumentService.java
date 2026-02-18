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

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for managing unstructured documents.
 * Documents are organized into named collections and stored as arbitrary JSON data.
 */
public interface DocumentService {

	/**
	 * Stores a new document in the specified collection.
	 * @param collectionName the target collection name
	 * @param data arbitrary key-value data to store as JSON
	 * @return a map representation of the stored document including its generated id
	 */
	Map<String, Object> storeDocument(String collectionName, Map<String, Object> data);

	/**
	 * Retrieves a document by id within a collection.
	 * @param collectionName the collection to search in
	 * @param id the document id
	 * @return an Optional containing the document if found, or empty if not found
	 */
	Optional<Map<String, Object>> getDocument(String collectionName, Long id);

	/**
	 * Retrieves all documents belonging to a collection.
	 * @param collectionName the collection to query
	 * @return list of document representations
	 */
	List<Map<String, Object>> getDocumentsByCollection(String collectionName);

	/**
	 * Updates an existing document with new data.
	 * @param collectionName the collection containing the document
	 * @param id the document id
	 * @param data the new data to replace the existing content
	 * @return a map representation of the updated document
	 * @throws DocumentNotFoundException if no document with the given id exists in the collection
	 */
	Map<String, Object> updateDocument(String collectionName, Long id, Map<String, Object> data);

	/**
	 * Deletes a document by id within a collection.
	 * @param collectionName the collection containing the document
	 * @param id the document id
	 */
	void deleteDocument(String collectionName, Long id);

	/**
	 * Lists all distinct collection names that contain at least one document.
	 * @return sorted list of collection names
	 */
	List<String> listCollections();

	/**
	 * Deletes all documents within a collection.
	 * @param collectionName the collection to delete
	 */
	void deleteCollection(String collectionName);

}
