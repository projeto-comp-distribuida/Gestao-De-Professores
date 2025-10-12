package com.distrischool.template.repository;

import com.distrischool.template.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    
    Optional<Teacher> findByEmployeeId(String employeeId);
    
    List<Teacher> findByStatus(Teacher.TeacherStatus status);
    
    @Query("SELECT t FROM Teacher t WHERE t.name ILIKE %:name%")
    List<Teacher> findByNameContaining(@Param("name") String name);
    
    @Query("SELECT t FROM Teacher t WHERE :subject MEMBER OF t.subjects")
    List<Teacher> findBySubject(@Param("subject") String subject);
    
    @Query("SELECT t FROM Teacher t WHERE t.deletedAt IS NULL")
    List<Teacher> findAllActive();
    
    @Query("SELECT t FROM Teacher t WHERE t.hireDate >= :startDate AND t.hireDate <= :endDate")
    List<Teacher> findByHireDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
