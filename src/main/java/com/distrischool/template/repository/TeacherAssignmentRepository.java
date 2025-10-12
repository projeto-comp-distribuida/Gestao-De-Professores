package com.distrischool.template.repository;

import com.distrischool.template.entity.TeacherAssignment;
import com.distrischool.template.entity.Teacher;
import com.distrischool.template.entity.Subject;
import com.distrischool.template.entity.ClassGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, Long> {
    
    List<TeacherAssignment> findByTeacher(Teacher teacher);
    
    List<TeacherAssignment> findBySubject(Subject subject);
    
    List<TeacherAssignment> findByClassGroup(ClassGroup classGroup);
    
    List<TeacherAssignment> findByStatus(TeacherAssignment.AssignmentStatus status);
    
    @Query("SELECT ta FROM TeacherAssignment ta WHERE ta.teacher = :teacher AND ta.status = 'ACTIVE'")
    List<TeacherAssignment> findActiveByTeacher(@Param("teacher") Teacher teacher);
    
    @Query("SELECT ta FROM TeacherAssignment ta WHERE ta.subject = :subject AND ta.status = 'ACTIVE'")
    List<TeacherAssignment> findActiveBySubject(@Param("subject") Subject subject);
    
    @Query("SELECT ta FROM TeacherAssignment ta WHERE ta.classGroup = :classGroup AND ta.status = 'ACTIVE'")
    List<TeacherAssignment> findActiveByClassGroup(@Param("classGroup") ClassGroup classGroup);
    
    @Query("SELECT ta FROM TeacherAssignment ta WHERE ta.teacher = :teacher AND ta.subject = :subject AND ta.classGroup = :classGroup AND ta.status = 'ACTIVE'")
    Optional<TeacherAssignment> findActiveAssignment(@Param("teacher") Teacher teacher, 
                                                    @Param("subject") Subject subject, 
                                                    @Param("classGroup") ClassGroup classGroup);
    
    @Query("SELECT ta FROM TeacherAssignment ta WHERE ta.assignmentDate BETWEEN :startDate AND :endDate")
    List<TeacherAssignment> findByAssignmentDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT ta FROM TeacherAssignment ta WHERE ta.notificationSent = false AND ta.status = 'ACTIVE'")
    List<TeacherAssignment> findPendingNotifications();
    
    @Query("SELECT ta FROM TeacherAssignment ta WHERE ta.deletedAt IS NULL")
    List<TeacherAssignment> findAllActive();
}
