package com.distrischool.teacher.repository;

import com.distrischool.teacher.entity.ClassGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassGroupRepository extends JpaRepository<ClassGroup, Long> {
    
    Optional<ClassGroup> findByCode(String code);
    
    List<ClassGroup> findByStatus(ClassGroup.ClassStatus status);
    
    List<ClassGroup> findByAcademicYear(String academicYear);
    
    List<ClassGroup> findByGradeLevel(String gradeLevel);
    
    List<ClassGroup> findByShift(ClassGroup.Shift shift);
    
    @Query("SELECT c FROM ClassGroup c WHERE c.name ILIKE %:name%")
    List<ClassGroup> findByNameContaining(@Param("name") String name);
    
    @Query("SELECT c FROM ClassGroup c WHERE c.deletedAt IS NULL")
    List<ClassGroup> findAllActive();
    
    @Query("SELECT c FROM ClassGroup c WHERE c.academicYear = :year AND c.deletedAt IS NULL")
    List<ClassGroup> findByAcademicYearAndActive(@Param("year") String academicYear);
}
