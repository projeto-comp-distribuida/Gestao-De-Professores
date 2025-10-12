package com.distrischool.template.repository;

import com.distrischool.template.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    
    Optional<Subject> findByCode(String code);
    
    List<Subject> findByStatus(Subject.SubjectStatus status);
    
    List<Subject> findByLevel(Subject.SubjectLevel level);
    
    @Query("SELECT s FROM Subject s WHERE s.name ILIKE %:name%")
    List<Subject> findByNameContaining(@Param("name") String name);
    
    @Query("SELECT s FROM Subject s WHERE s.deletedAt IS NULL")
    List<Subject> findAllActive();
    
    @Query("SELECT s FROM Subject s WHERE s.code = :code AND s.deletedAt IS NULL")
    Optional<Subject> findByCodeAndActive(@Param("code") String code);
}
