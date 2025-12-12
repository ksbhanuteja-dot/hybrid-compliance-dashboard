package com.hybrid.attendancetracker.repository;

import com.hybrid.attendancetracker.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByAttendanceDateBetween(LocalDate start, LocalDate end);
    List<Attendance> findByEmployeeId(String employeeId);
}