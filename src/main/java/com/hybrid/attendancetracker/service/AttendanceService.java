package com.hybrid.attendancetracker.service;

import com.hybrid.attendancetracker.dto.EmployeeSummary;
import com.hybrid.attendancetracker.entity.Attendance;
import com.hybrid.attendancetracker.repository.AttendanceRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
    import java.util.Map;

@Service
public class AttendanceService {
    @Autowired
    private AttendanceRepository repo;

    @Autowired
    private EmailService emailService;

    public String uploadExcel(MultipartFile file) throws IOException {
        List<Attendance> attendanceList = parseExcel(file.getInputStream());
        repo.saveAll(attendanceList);
        return "Upload successful! Check your dashboard for detailed analysis.";
    }

    private List<Attendance> parseExcel(InputStream inputStream) throws IOException {
        List<Attendance> attendanceList = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {  // Skip header
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Attendance att = new Attendance();
                att.setEmployeeId(row.getCell(0).getStringCellValue());
                att.setEmployeeName(row.getCell(1).getStringCellValue());
                att.setEmail(row.getCell(2).getStringCellValue());
                att.setDepartment(row.getCell(3).getStringCellValue());

                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
                att.setInTime(LocalTime.parse(row.getCell(4).getStringCellValue(), timeFormatter));
                att.setOutTime(LocalTime.parse(row.getCell(5).getStringCellValue(), timeFormatter));

                att.setWorkedHours(parseHours(row.getCell(6).getStringCellValue()));
                att.setMonthlyAverage(parseHours(row.getCell(7).getStringCellValue()));

                att.setDayOfWeek(row.getCell(8).getStringCellValue());

                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                att.setAttendanceDate(LocalDate.parse(row.getCell(9).getStringCellValue(), dateFormatter));

                att.setYear((int) row.getCell(10).getNumericCellValue());

                attendanceList.add(att);
            }
        }
        return attendanceList;
    }

    private double parseHours(String hoursStr) {
        if (hoursStr == null || hoursStr.isEmpty()) return 0.0;
        String[] parts = hoursStr.split(":");
        if (parts.length != 2) return 0.0;
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        return hours + (minutes / 60.0);
    }

    public List<EmployeeSummary> getDashboardData(LocalDate from, LocalDate to) {
        List<Attendance> records = repo.findByAttendanceDateBetween(from, to);
        Map<String, EmployeeSummary> summaryMap = new HashMap<>();

        for (Attendance att : records) {
            summaryMap.computeIfAbsent(att.getEmployeeId(), k -> {
                EmployeeSummary sum = new EmployeeSummary();
                sum.setEmployeeId(att.getEmployeeId());
                sum.setEmployeeName(att.getEmployeeName());
                sum.setEmail(att.getEmail());
                return sum;
            }).addHours(att.getWorkedHours());
        }

        List<EmployeeSummary> summaries = new ArrayList<>(summaryMap.values());
        summaries.removeIf(sum -> sum.getShortfallDays() <= 0);
        return summaries;
    }

    public void sendReminder(String employeeId) {
        List<Attendance> records = repo.findByEmployeeId(employeeId);
        if (records.isEmpty()) return;

        EmployeeSummary summary = new EmployeeSummary();
        summary.setEmployeeName(records.get(0).getEmployeeName());
        summary.setEmail(records.get(0).getEmail());
        for (Attendance att : records) {
            summary.addHours(att.getWorkedHours());
        }

        String subject = "Friendly Reminder: Hybrid Compliance Monthly Days";
        String body = String.format(
                "Dear %s,\n\n" +
                        "As per policy, complete 12 days/month.\n" +
                        "Your status: %d days, Shortfall: %d days.\n\n" +
                        "Please address this.\n\nBest,\nHR Team",
                summary.getEmployeeName(), summary.getAttendedDays(), summary.getShortfallDays()
        );

        emailService.sendEmail(summary.getEmail(), subject, body);
    }
}