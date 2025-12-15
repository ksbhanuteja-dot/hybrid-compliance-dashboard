package com.hybrid.attendancetracker.controller;

import com.hybrid.attendancetracker.dto.EmployeeSummary;
import com.hybrid.attendancetracker.service.AttendanceService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    /* =========================================================
       UPLOAD EXCEL
       ========================================================= */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            String message = attendanceService.uploadExcel(file);
            return ResponseEntity.ok(message);
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body("Upload failed: " + e.getMessage());
        }
    }

    /* =========================================================
       DASHBOARD API
       - No silent filtering
       - Full dataset by default
       ========================================================= */
    @GetMapping("/api/dashboard")
    public List<EmployeeSummary> getDashboard(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDate startDate =
                (from != null && !from.isEmpty())
                        ? LocalDate.parse(from)
                        : LocalDate.of(2000, 1, 1); // ALL DATA

        LocalDate endDate =
                (to != null && !to.isEmpty())
                        ? LocalDate.parse(to)
                        : LocalDate.now();

        return attendanceService.getDashboardData(startDate, endDate);
    }

    /* =========================================================
       SEND REMINDER
       ========================================================= */
    @PostMapping("/send-reminder/{employeeId}")
    public ResponseEntity<String> sendReminder(
            @PathVariable String employeeId) {

        attendanceService.sendReminder(employeeId);
        return ResponseEntity.ok("Reminder sent successfully.");
    }

    /* =========================================================
       EXPORT DASHBOARD TO EXCEL
       - Uses SAME date logic as dashboard
       ========================================================= */
    @GetMapping("/api/export-excel")
    public void exportExcel(HttpServletResponse response,
                            @RequestParam(required = false) String from,
                            @RequestParam(required = false) String to)
            throws IOException {

        LocalDate startDate =
                (from != null && !from.isEmpty())
                        ? LocalDate.parse(from)
                        : LocalDate.of(2000, 1, 1); // ALL DATA

        LocalDate endDate =
                (to != null && !to.isEmpty())
                        ? LocalDate.parse(to)
                        : LocalDate.now();

        List<EmployeeSummary> data =
                attendanceService.getDashboardData(startDate, endDate);

        response.setContentType("application/octet-stream");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=attendance_dashboard.xlsx"
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet =
                    workbook.createSheet("Employees Below Compliance");

            // Header
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Emp ID");
            header.createCell(1).setCellValue("Employee Name");
            header.createCell(2).setCellValue("Attended Days");
            header.createCell(3).setCellValue("Avg Hours / Day");
            header.createCell(4).setCellValue("Shortfall (Days)");
            header.createCell(5).setCellValue("Status");
            header.createCell(6).setCellValue("Email");

            // Data
            int rowNum = 1;
            for (EmployeeSummary emp : data) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(emp.getEmployeeId());
                row.createCell(1).setCellValue(emp.getEmployeeName());
                row.createCell(2).setCellValue(emp.getAttendedDays());
                row.createCell(3).setCellValue(emp.getAvgHours());
                row.createCell(4).setCellValue(emp.getShortfallDays());
                row.createCell(5).setCellValue(emp.getRemainderStatus());
                row.createCell(6).setCellValue(emp.getEmail());
            }

            workbook.write(response.getOutputStream());
        }
    }
}
