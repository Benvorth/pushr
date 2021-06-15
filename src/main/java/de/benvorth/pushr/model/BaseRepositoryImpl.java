package de.benvorth.pushr.model;

import org.springframework.data.jpa.repository.support.JpaMetamodelEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.io.Serializable;

public class BaseRepositoryImpl<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> implements BaseRepository<T, ID> {

    private final EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public BaseRepositoryImpl(JpaMetamodelEntityInformation jpaMetamodelEntityInformation, Object o) {
        super(jpaMetamodelEntityInformation.getJavaType(), (EntityManager) o);
        this.entityManager = (EntityManager) o;
    }

    @Override
    @Transactional
    public <S extends T> S persist(S entity) {
        entityManager.persist(entity);
        return entity;
    }
}
