package com.hybrid.attendancetracker.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "attendance")
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String employeeId;
    private String employeeName;
    private String email;
    private String department;
    private LocalTime inTime;
    private LocalTime outTime;
    private Double workedHours;
    private Double monthlyAverage;
    private String dayOfWeek;
    private LocalDate attendanceDate;
    private Integer year;

    // Constructors
    public Attendance() {}

    // Getters
    public Long getId() { return id; }
    public String getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public String getEmail() { return email; }
    public String getDepartment() { return department; }
    public LocalTime getInTime() { return inTime; }
    public LocalTime getOutTime() { return outTime; }
    public Double getWorkedHours() { return workedHours; }
    public Double getMonthlyAverage() { return monthlyAverage; }
    public String getDayOfWeek() { return dayOfWeek; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public Integer getYear() { return year; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public void setEmail(String email) { this.email = email; }
    public void setDepartment(String department) { this.department = department; }
    public void setInTime(LocalTime inTime) { this.inTime = inTime; }
    public void setOutTime(LocalTime outTime) { this.outTime = outTime; }
    public void setWorkedHours(Double workedHours) { this.workedHours = workedHours; }
    public void setMonthlyAverage(Double monthlyAverage) { this.monthlyAverage = monthlyAverage; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    public void setYear(Integer year) { this.year = year; }
}