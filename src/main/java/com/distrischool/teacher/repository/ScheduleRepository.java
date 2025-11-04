package com.distrischool.teacher.repository;

import com.distrischool.teacher.entity.Schedule;
import com.distrischool.teacher.entity.Teacher;
import com.distrischool.teacher.entity.Subject;
import com.distrischool.teacher.entity.ClassGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    
    List<Schedule> findByTeacher(Teacher teacher);
    
    List<Schedule> findBySubject(Subject subject);
    
    List<Schedule> findByClassGroup(ClassGroup classGroup);
    
    List<Schedule> findByDayOfWeek(DayOfWeek dayOfWeek);
    
    List<Schedule> findByStatus(Schedule.ScheduleStatus status);
    
    List<Schedule> findByAcademicYear(String academicYear);
    
    @Query("SELECT s FROM Schedule s WHERE s.teacher = :teacher AND s.dayOfWeek = :dayOfWeek AND s.status = 'ACTIVE'")
    List<Schedule> findByTeacherAndDayOfWeek(@Param("teacher") Teacher teacher, @Param("dayOfWeek") DayOfWeek dayOfWeek);
    
    @Query("SELECT s FROM Schedule s WHERE s.classGroup = :classGroup AND s.dayOfWeek = :dayOfWeek AND s.status = 'ACTIVE'")
    List<Schedule> findByClassGroupAndDayOfWeek(@Param("classGroup") ClassGroup classGroup, @Param("dayOfWeek") DayOfWeek dayOfWeek);
    
    @Query("SELECT s FROM Schedule s WHERE s.roomNumber = :roomNumber AND s.dayOfWeek = :dayOfWeek AND s.startTime <= :endTime AND s.endTime >= :startTime AND s.status = 'ACTIVE'")
    List<Schedule> findConflictingSchedules(@Param("roomNumber") String roomNumber, 
                                           @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                           @Param("startTime") LocalTime startTime,
                                           @Param("endTime") LocalTime endTime);
    
    @Query("SELECT s FROM Schedule s WHERE s.deletedAt IS NULL")
    List<Schedule> findAllActive();
    
    @Query("SELECT s FROM Schedule s WHERE s.teacher = :teacher AND s.academicYear = :academicYear AND s.status = 'ACTIVE'")
    List<Schedule> findByTeacherAndAcademicYear(@Param("teacher") Teacher teacher, @Param("academicYear") String academicYear);
}
