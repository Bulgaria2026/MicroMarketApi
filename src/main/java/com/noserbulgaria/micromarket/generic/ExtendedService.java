package com.noserbulgaria.micromarket.generic;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Generic service interface providing CRUD operations for extended entities.
 * This service acts as an abstraction layer between controllers and repositories,
 * providing common business logic and data transformation capabilities.
 *
 * @param <E> The entity type extending ExtendedEntity
 * @param <D> The DTO type extending ExtendedDto
 */
public interface ExtendedService<E extends ExtendedEntity, D extends ExtendedDto> {

  /**
   * Creates a new entity from the provided DTO.
   *
   * @param dto the data transfer object containing entity data
   * @return the created entity as a DTO
   */
  D create(D dto);

  /**
   * Retrieves an entity by its ID.
   *
   * @param id the entity ID
   * @return an Optional containing the entity DTO if found
   */
  Optional<D> findById(UUID id);

  /**
   * Retrieves all entities.
   *
   * @return a list of all entity DTOs
   */
  List<D> findAll();

  /**
   * Retrieves a paginated list of all entities.
   *
   * @param pageable the pagination configuration
   * @return a page of entity DTOs
   */
  Page<D> findAll(Pageable pageable);

  /**
   * Retrieves entities matching the provided specification with pagination support.
   *
   * @param spec the JPA specification for filtering
   * @param pageable the pagination configuration
   * @return a page of entity DTOs matching the specification
   */
  Page<D> findAll(Specification<E> spec, Pageable pageable);

  /**
   * Updates an existing entity with the provided DTO data.
   *
   * @param id the entity ID to update
   * @param dto the data transfer object containing updated data
   * @return an Optional containing the updated entity DTO if found
   */
  Optional<D> update(UUID id, D dto);

  /**
   * Deletes an entity by its ID.
   *
   * @param id the entity ID to delete
   * @return true if the entity was deleted, false if not found
   */
  boolean delete(UUID id);

  /**
   * Deletes all entities.
   */
  void deleteAll();

  /**
   * Counts the total number of entities.
   *
   * @return the count of entities
   */
  long count();
}

