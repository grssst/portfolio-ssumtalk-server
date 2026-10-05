package com.hottalk.hottalkserver.controller;

import com.hottalk.hottalkserver.dto.ReportDto;
import com.hottalk.hottalkserver.model.Report;
import com.hottalk.hottalkserver.model.ReportChatting;
import com.hottalk.hottalkserver.repository.ReportChattingRepository;
import com.hottalk.hottalkserver.repository.ReportRepository;
import com.hottalk.hottalkserver.service.ReportService;
import com.hottalk.hottalkserver.service.UserService;
import com.hottalk.hottalkserver.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportChattingRepository reportChattingRepository;
    private final ReportRepository reportRepository;
    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService, ReportRepository reportRepository, ReportChattingRepository reportChattingRepository) {
        this.reportService = reportService;
        this.reportRepository = reportRepository;
        this.reportChattingRepository = reportChattingRepository;
    }

    @PostMapping("/report-user")
    public ResponseEntity<String> reportUser(@RequestHeader("Authorization") String token, @RequestBody ReportDto reportDto) {

        reportService.saveReport(reportDto, token);
        return ResponseEntity.ok("Report submitted successfully");
    }

    // 예: ReportController.java
    @PostMapping("/report-meeting")
    public ResponseEntity<String> reportMeeting(@RequestHeader("Authorization") String token, @RequestBody ReportDto reportDto) {

        reportService.saveReportMeeting(reportDto, token);
        return ResponseEntity.ok("Report submitted successfully");
    }

}
