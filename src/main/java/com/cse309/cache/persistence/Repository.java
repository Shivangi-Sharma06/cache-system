package com.cse309.cache.persistence;

import com.cse309.cache.exception.CachePersistenceException;

import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD repository interface for SQLite persistence.
 * Demonstrates generic interface with two type parameters (Unit 5).
 *
 * @param <T> entity type
 * @param <ID> primary key identifier type
 */
public interface Repository<T, ID> {

    T save(T entity) throws CachePersistenceException;

    Optional<T> findById(ID id) throws CachePersistenceException;

    List<T> findAll() throws CachePersistenceException;

    void update(T entity) throws CachePersistenceException;

    boolean deleteById(ID id) throws CachePersistenceException;
}
