package com.hybrid.attendancetracker.dto;

public class EmployeeSummary {
    private String employeeId;
    private String employeeName;
    private String email;
    private int attendedDays = 0;
    private double totalHours = 0.0;
    private double avgHours = 0.0;
    private int shortfallDays = 0;
    private String remainderStatus = "Pending";

    // Constructors
    public EmployeeSummary() {}

    // Method to add a record's hours and recalculate
    public void addHours(double hours) {
        attendedDays++;
        totalHours += hours;
        avgHours = attendedDays > 0 ? totalHours / attendedDays : 0.0;
        shortfallDays = Math.max(0, 12 - attendedDays);  // Target 12 days
    }

    // Getters
    public String getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public String getEmail() { return email; }
    public int getAttendedDays() { return attendedDays; }
    public double getTotalHours() { return totalHours; }
    public double getAvgHours() { return avgHours; }
    public int getShortfallDays() { return shortfallDays; }
    public String getRemainderStatus() { return remainderStatus; }

    // Setters
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public void setEmail(String email) { this.email = email; }
    public void setAttendedDays(int attendedDays) { this.attendedDays = attendedDays; }
    public void setTotalHours(double totalHours) { this.totalHours = totalHours; }
    public void setAvgHours(double avgHours) { this.avgHours = avgHours; }
    public void setShortfallDays(int shortfallDays) { this.shortfallDays = shortfallDays; }
    public void setRemainderStatus(String remainderStatus) { this.remainderStatus = remainderStatus; }
}