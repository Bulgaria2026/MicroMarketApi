package com.noserbulgaria.micromarket.generic;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Abstract base service implementation providing CRUD operations for extended entities.
 * This class implements the ExtendedService interface and uses ModelMapper for DTO conversions.
 *
 * @param <E> The entity type extending ExtendedEntity
 * @param <D> The DTO type extending ExtendedDto
 * @param <R> The repository type
 */
@RequiredArgsConstructor
public abstract class ExtendedServiceImpl<
    E extends ExtendedEntity,
    D extends ExtendedDto,
    R extends JpaRepository<E, UUID> & JpaSpecificationExecutor<E>>
    implements ExtendedService<E, D> {

  protected final R repository;
  protected final ModelMapper modelMapper;
  protected final Class<E> entityClass;
  protected final Class<D> dtoClass;

  @Override
  public D create(D dto) {
    E entity = modelMapper.map(dto, entityClass);
    E saved = repository.save(entity);
    return modelMapper.map(saved, dtoClass);
  }

  @Override
  public Optional<D> findById(UUID id) {
    return repository
        .findById(id)
        .map(entity -> modelMapper.map(entity, dtoClass));
  }

  @Override
  public List<D> findAll() {
    return repository.findAll()
        .stream()
        .map(entity -> modelMapper.map(entity, dtoClass))
        .collect(Collectors.toList());
  }

  @Override
  public Page<D> findAll(Pageable pageable) {
    return repository.findAll(pageable)
        .map(entity -> modelMapper.map(entity, dtoClass));
  }

  @Override
  public Page<D> findAll(Specification<E> spec, Pageable pageable) {
    return repository.findAll(spec, pageable)
        .map(entity -> modelMapper.map(entity, dtoClass));
  }

  @Override
  public Optional<D> update(UUID id, D dto) {
    return repository.findById(id)
        .map(entity -> {
          modelMapper.map(dto, entity);
          E updated = repository.save(entity);
          return modelMapper.map(updated, dtoClass);
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

  @Override
  public void deleteAll() {
    repository.deleteAll();
  }

  @Override
  public long count() {
    return repository.count();
  }
}

