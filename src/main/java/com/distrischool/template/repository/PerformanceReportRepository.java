package com.distrischool.template.repository;

import com.distrischool.template.entity.PerformanceReport;
import com.distrischool.template.entity.Teacher;
import com.distrischool.template.entity.Subject;
import com.distrischool.template.entity.ClassGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PerformanceReportRepository extends JpaRepository<PerformanceReport, Long> {
    
    List<PerformanceReport> findByTeacher(Teacher teacher);
    
    List<PerformanceReport> findBySubject(Subject subject);
    
    List<PerformanceReport> findByClassGroup(ClassGroup classGroup);
    
    List<PerformanceReport> findByOverallRating(PerformanceReport.OverallRating overallRating);
    
    @Query("SELECT pr FROM PerformanceReport pr WHERE pr.teacher = :teacher ORDER BY pr.reportPeriodEnd DESC")
    List<PerformanceReport> findByTeacherOrderByPeriod(@Param("teacher") Teacher teacher);
    
    @Query("SELECT pr FROM PerformanceReport pr WHERE pr.reportPeriodStart >= :startDate AND pr.reportPeriodEnd <= :endDate")
    List<PerformanceReport> findByPeriodRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT pr FROM PerformanceReport pr WHERE pr.teacher = :teacher AND pr.reportPeriodStart >= :startDate AND pr.reportPeriodEnd <= :endDate")
    List<PerformanceReport> findByTeacherAndPeriodRange(@Param("teacher") Teacher teacher, 
                                                       @Param("startDate") LocalDate startDate, 
                                                       @Param("endDate") LocalDate endDate);
    
    @Query("SELECT pr FROM PerformanceReport pr WHERE pr.overallRating = :rating AND pr.reportPeriodEnd >= :date")
    List<PerformanceReport> findByRatingAndRecentPeriod(@Param("rating") PerformanceReport.OverallRating rating, 
                                                        @Param("date") LocalDate date);
    
    @Query("SELECT pr FROM PerformanceReport pr WHERE pr.deletedAt IS NULL")
    List<PerformanceReport> findAllActive();
}
