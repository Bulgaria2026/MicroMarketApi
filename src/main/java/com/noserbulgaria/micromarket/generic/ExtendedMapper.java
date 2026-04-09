package com.noserbulgaria.micromarket.generic;

import java.util.List;
import java.util.Set;

/**
 * This interface is used to map between entities and DTOs.
 *
 * @param <T> The entity class that extends ExtendedEntity
 * @param <D> The DTO class that extends ExtendedDTO
 */
public interface ExtendedMapper<T extends ExtendedEntity, D extends ExtendedDto> {

  /**
   * Maps an entity to a DTO.
   *
   * @param entity the entity
   * @return the DTO
   */
  D entityToDto(T entity);

  /**
   * Maps a DTO to an entity.
   *
   * @param dto the DTO
   * @return the entity
   */
  T dtoToEntity(D dto);

  /**
   * Maps a set of entities to a set of DTOs.
   *
   * @param entities the set of entities
   * @return the set of DTOs
   */
  Set<D> entitiesToDtoSet(Set<T> entities);

  /**
   * Maps a list of entities to a list of DTOs.
   *
   * @param entities the list of entities
   * @return the list of DTOs
   */
  List<D> entitiesToDtoList(List<T> entities);

  /**
   * Maps a set of DTOs to a set of entities.
   *
   * @param dtoSet the set of DTOs
   * @return the set of entities
   */
  Set<T> dtoSetToEntity(Set<D> dtoSet);

}
