package com.hybrid.attendancetracker.service;

import com.hybrid.attendancetracker.dto.EmployeeSummary;
import com.hybrid.attendancetracker.entity.Attendance;
import com.hybrid.attendancetracker.repository.AttendanceRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository repo;

    @Autowired
    private EmailService emailService;

    private static final DateTimeFormatter TIME_PARSER =
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("hh:mm a")
                    .toFormatter(Locale.ENGLISH);

    // SPECIAL EMPLOYEES → 8 DAYS TARGET
    private static final Set<String> SPECIAL_EMPLOYEES = Set.of(
            "1271", "1272", "1321", "1522", "1430", "1415"
    );

    /* =========================================================
       BULLETPROOF EMPLOYEE ID NORMALIZATION
       ========================================================= */
    private String normalizeEmpId(String empId) {
        if (empId == null) return "";
        String digitsOnly = empId.replaceAll("[^0-9]", "");
        return digitsOnly.replaceFirst("^0+(?!$)", "");
    }

    /* =========================================================
       EXCEL UPLOAD
       ========================================================= */
    public String uploadExcel(MultipartFile file) throws IOException {
        List<Attendance> records = parseExcel(file.getInputStream());
        repo.deleteAll();
        repo.saveAll(records);
        return "Upload successful. Dashboard updated.";
    }

    private List<Attendance> parseExcel(InputStream is) throws IOException {
        List<Attendance> list = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r == null) continue;

                Attendance a = new Attendance();
                a.setEmployeeId(getString(r, 0));
                a.setEmployeeName(getString(r, 1));
                a.setEmail(getString(r, 2));
                a.setDepartment(getString(r, 3));

                String inTime = getString(r, 4);
                String outTime = getString(r, 5);

                if (!inTime.isBlank()) {
                    a.setInTime(LocalTime.parse(inTime, TIME_PARSER));
                }
                if (!outTime.isBlank()) {
                    a.setOutTime(LocalTime.parse(outTime, TIME_PARSER));
                }

                a.setWorkedHours(parseHours(getString(r, 6)));
                a.setDayOfWeek(getString(r, 8));

                a.setAttendanceDate(
                        LocalDate.parse(
                                getString(r, 9),
                                DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                );

                a.setYear((int) r.getCell(10).getNumericCellValue());
                list.add(a);
            }
        }
        return list;
    }

    private String getString(Row r, int idx) {
        Cell c = r.getCell(idx);
        if (c == null) return "";
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) c.getNumericCellValue());
            default -> "";
        };
    }

    private double parseHours(String s) {
        if (s == null || s.isBlank()) return 0.0;
        String[] p = s.split(":");
        int h = Integer.parseInt(p[0]);
        int m = p.length > 1 ? Integer.parseInt(p[1]) : 0;
        return h + m / 60.0;
    }

    /* =========================================================
       DASHBOARD DATA — SHOW ALL EMPLOYEES
       ========================================================= */
    public List<EmployeeSummary> getDashboardData(LocalDate from, LocalDate to) {

        List<Attendance> records =
                repo.findByAttendanceDateBetween(from, to);

        Map<String, Map<LocalDate, Double>> dailyHours = new HashMap<>();
        Map<String, String> names = new HashMap<>();
        Map<String, String> emails = new HashMap<>();

        for (Attendance att : records) {
            String empId = att.getEmployeeId();
            LocalDate date = att.getAttendanceDate();

            dailyHours
                    .computeIfAbsent(empId, k -> new HashMap<>())
                    .putIfAbsent(date, att.getWorkedHours());

            names.putIfAbsent(empId, att.getEmployeeName());
            emails.putIfAbsent(empId, att.getEmail());
        }

        List<EmployeeSummary> result = new ArrayList<>();

        for (String empId : dailyHours.keySet()) {

            EmployeeSummary sum = new EmployeeSummary();
            sum.setEmployeeId(empId);
            sum.setEmployeeName(names.get(empId));
            sum.setEmail(emails.get(empId));

            for (Map.Entry<LocalDate, Double> e : dailyHours.get(empId).entrySet()) {
                sum.addDailyHours(e.getKey(), e.getValue());
            }

            String normalizedEmpId = normalizeEmpId(empId);
            boolean isSpecial = SPECIAL_EMPLOYEES.contains(normalizedEmpId);

            sum.applyPolicy(isSpecial);

            // ✅ SHOW EVERY EMPLOYEE (NO FILTER)
            result.add(sum);
        }

        return result;
    }

    /* =========================================================
       REMINDER EMAIL
       ========================================================= */
    public void sendReminder(String employeeId) {

        List<Attendance> records = repo.findByEmployeeId(employeeId);
        if (records.isEmpty()) return;

        EmployeeSummary sum = new EmployeeSummary();
        sum.setEmployeeId(employeeId);
        sum.setEmployeeName(records.get(0).getEmployeeName());
        sum.setEmail(records.get(0).getEmail());

        // Collect unique days only
        Map<LocalDate, Double> daily = new HashMap<>();
        for (Attendance att : records) {
            daily.putIfAbsent(att.getAttendanceDate(), att.getWorkedHours());
        }

        for (Map.Entry<LocalDate, Double> e : daily.entrySet()) {
            sum.addDailyHours(e.getKey(), e.getValue());
        }

        // Normalize employee ID and apply special policy
        String normalizedEmpId = normalizeEmpId(employeeId);
        boolean isSpecial = SPECIAL_EMPLOYEES.contains(normalizedEmpId);

        sum.applyPolicy(isSpecial);

        String subject = "Friendly Reminder: Hybrid Compliance";

        String body;

        if (isSpecial) {
            body = String.format(
                    "Dear %s,\n\n" +
                    "As per Work from Office Hybrid policy for your role:\n\n" +
                    "Expected: 8 days/month (40 hours total)\n" +
                    "Your attended days: %d\n" +
                    "Shortfall: %d days\n\n" +
                    "Please plan accordingly.\n\n" +
                    "Best regards,\n" +
                    "HR Team",
                    sum.getEmployeeName(),
                    sum.getAttendedDays(),
                    sum.getShortfallDays()
            );
        } else {
            body = String.format(
                    "Dear %s,\n\n" +
                    "As per Work from Office Hybrid policy:\n\n" +
                    "Expected: 12 days/month (60 hours total)\n" +
                    "Your attended days: %d\n" +
                    "Shortfall: %d days\n\n" +
                    "Please plan accordingly.\n\n" +
                    "Best regards,\n" +
                    "HR Team",
                    sum.getEmployeeName(),
                    sum.getAttendedDays(),
                    sum.getShortfallDays()
            );
        }

        emailService.sendEmail(sum.getEmail(), subject, body);
    }

}
