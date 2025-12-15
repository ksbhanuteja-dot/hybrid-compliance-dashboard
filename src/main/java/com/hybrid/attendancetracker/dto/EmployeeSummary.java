package com.hybrid.attendancetracker.dto;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class EmployeeSummary {

    private String employeeId;
    private String employeeName;
    private String email;

    // track distinct attendance days
    private final Set<LocalDate> attendedDates = new HashSet<>();

    private double totalHours = 0.0;

    // derived values
    private int attendedDays;
    private double avgHours;
    private int shortfallDays;

    private boolean specialEmployee;
    private String remainderStatus = "Pending";

    // constants
    private static final int NORMAL_TARGET_DAYS = 12;
    private static final int SPECIAL_TARGET_DAYS = 8;
    private static final double SPECIAL_MIN_AVG_HOURS = 5.0;

    public void addDailyHours(LocalDate date, double hours) {
        // only count once per date
        if (attendedDates.add(date)) {
            totalHours += hours;
            recalc();
        }
    }

    public void applyPolicy(boolean isSpecialEmployee) {
        this.specialEmployee = isSpecialEmployee;

        int targetDays = isSpecialEmployee
                ? SPECIAL_TARGET_DAYS
                : NORMAL_TARGET_DAYS;

        this.shortfallDays = Math.max(0, targetDays - attendedDays);

        // optional: set remainder status
        if (isSpecialEmployee) {
            if (attendedDays >= SPECIAL_TARGET_DAYS && avgHours >= SPECIAL_MIN_AVG_HOURS) {
                remainderStatus = "Compliant";
            } else {
                remainderStatus = "Pending";
            }
        } else {
            remainderStatus = attendedDays >= NORMAL_TARGET_DAYS
                    ? "Compliant"
                    : "Pending";
        }
    }

    private void recalc() {
        this.attendedDays = attendedDates.size();
        this.avgHours = attendedDays > 0 ? totalHours / attendedDays : 0.0;
    }

    // getters
    public String getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public String getEmail() { return email; }
    public int getAttendedDays() { return attendedDays; }
    public double getTotalHours() { return totalHours; }
    public double getAvgHours() { return avgHours; }
    public int getShortfallDays() { return shortfallDays; }
    public boolean isSpecialEmployee() { return specialEmployee; }
    public String getRemainderStatus() { return remainderStatus; }

    // setters
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public void setEmail(String email) { this.email = email; }
}
