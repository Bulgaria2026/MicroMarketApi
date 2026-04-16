package com.noserbulgaria.micromarket.generic;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/**
 * Abstract base service implementation providing CRUD operations for extended entities. This class implements the
 * ExtendedService interface and uses a mapper for DTO conversions.
 *
 * @param <E> The entity type extending ExtendedEntity
 * @param <D> The DTO type extending ExtendedDto
 * @param <R> The repository type
 * @param <M> The mapper type
 */
@RequiredArgsConstructor
public abstract class ExtendedServiceImpl<
    E extends ExtendedEntity,
    D extends ExtendedDto,
    R extends JpaRepository<E, UUID> & JpaSpecificationExecutor<E>,
    M extends ExtendedMapper<E, D>>
    implements ExtendedService<E, D> {

  protected final R repository;
  protected final M mapper;

  @Override
  public D create(D dto) {
    E entity = mapper.dtoToEntity(dto);
    E saved = repository.save(entity);
    return mapper.entityToDto(saved);
  }

  @Override
  public Optional<D> findById(UUID id) {
    return repository
        .findById(id)
        .map(mapper::entityToDto);
  }

  @Override
  public Page<D> findAll(Pageable pageable) {
    return repository.findAll(pageable)
        .map(mapper::entityToDto);
  }

  @Override
  public Page<D> findAll(Specification<E> spec, Pageable pageable) {
    return repository.findAll(spec, pageable)
        .map(mapper::entityToDto);
  }

  @Override
  public Optional<D> update(UUID id, D dto) {
    return repository.findById(id)
        .map(entity -> {
          E updatedEntity = mapper.dtoToEntity(dto);
          // Copy the ID from the existing entity to preserve it
          updatedEntity.setId(entity.getId());
          E saved = repository.save(updatedEntity);
          return mapper.entityToDto(saved);
        });
  }

  @Override
  public boolean delete(UUID id) {
    if (repository.existsById(id)) {
      repository.deleteById(id);
      return true;
    }
    return false;
  }
}
