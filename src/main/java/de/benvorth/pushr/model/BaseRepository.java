package de.benvorth.pushr.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;

// we need that because a "normal" CRUD-Repo only uses ".save()" which generates a
// SELECT before an UPDATE
// see https://dzone.com/articles/persisting-natural-key-entities-with-spring-data-j

// use .save() for new entries (INSERT)
// use .persist for UPDATES

@NoRepositoryBean
public interface BaseRepository<T, ID extends Serializable> extends CrudRepository<T, ID> {
    <S extends T> S persist(S entity);
}
