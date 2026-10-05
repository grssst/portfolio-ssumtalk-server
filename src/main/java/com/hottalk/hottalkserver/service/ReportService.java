package com.hottalk.hottalkserver.service;

import com.hottalk.hottalkserver.dto.ReportDto;
import com.hottalk.hottalkserver.model.Report;
import com.hottalk.hottalkserver.model.User;
import com.hottalk.hottalkserver.repository.ReportChattingRepository;
import com.hottalk.hottalkserver.repository.ReportRepository;
import com.hottalk.hottalkserver.repository.UserRepository;
import com.hottalk.hottalkserver.util.JwtUtil;
import com.hottalk.hottalkserver.util.S3Uploader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final UserChatRoomsService userChatRoomsService;
    private final S3Uploader s3Uploader;

    @Autowired
    public ReportService(ReportRepository reportRepository, UserService userService, UserChatRoomsService userChatRoomsService, UserRepository userRepository, S3Uploader s3Uploader) {
        this.reportRepository = reportRepository;
        this.userService = userService;
        this.userChatRoomsService = userChatRoomsService;
        this.userRepository = userRepository;
        this.s3Uploader = s3Uploader;
    }

    public void saveReport(ReportDto reportDto, String token) {
        String externalUserId = JwtUtil.validateToken(token.replace("Bearer ", ""));
        Long myId = userService.getUserProfile(externalUserId).getId();


        Report report = new Report();

        Optional<User> reportedUser = null;
        if (Boolean.TRUE.equals(reportDto.getIsChatRoom())) { // Boolean 체크는 .equals() 사용
            reportedUser = userRepository.findById(reportDto.getReportedUserId());
            if (reportedUser.isPresent()) {
                String reportedExternaluserId = reportedUser.get().getExternalUserId();
                report.setReportedExternalUserId(reportedExternaluserId);
            }
        } else {
            report.setReportedExternalUserId(reportDto.getReportedExternalUserId());
        }

        report.setReportedUserId(reportDto.getReportedUserId());
        report.setReportContent(reportDto.getReportContent());
        report.setPostId(reportDto.getPostId());
        report.setReportedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        report.setRoomId(reportDto.getRoomId());
        report.setPostText(reportDto.getPostText());
        report.setReporterUserId(myId);
        report.setStatus(reportDto.getStatus());


        reportRepository.save(report);

        String reportedPostId = String.valueOf(reportDto.getPostId());


        // 🚀 1. 채팅 신고 (`roomId`가 있을 경우) → `reported/profile/`
        if (reportDto.getRoomId() != null && reportedUser.isPresent()) {
            String reportedExternaluserId = reportedUser.get().getExternalUserId();
            s3Uploader.syncReportedProfileImages(reportedExternaluserId);
        }

        // 🚀 2. 사용자 신고 (`status`가 있을 경우) → `reported/profile/`
        else if (reportDto.getStatus() != null) {
            s3Uploader.syncReportedProfileImages(reportDto.getReportedExternalUserId());
        }

        // 🚀 3. 게시글 신고 (`postId`가 있을 경우) → `reported/profile/`과 `reported/post/`
        else if (reportDto.getPostId() != null) {
            s3Uploader.syncReportedPostImage(reportedPostId);
            s3Uploader.syncReportedProfileImages(reportDto.getReportedExternalUserId());
        }

    }


    public void saveReportMeeting(ReportDto reportDto, String token) {
        String externalUserId = JwtUtil.validateToken(token.replace("Bearer ", ""));
        Long myId = userService.getUserProfile(externalUserId).getId();


        Report report = new Report();

        Optional<User> reportedUser = null;
        if (Boolean.TRUE.equals(reportDto.getIsChatRoom())) { // Boolean 체크는 .equals() 사용
            reportedUser = userRepository.findById(reportDto.getReportedUserId());
            if (reportedUser.isPresent()) {
                String reportedExternaluserId = reportedUser.get().getExternalUserId();
                report.setReportedExternalUserId(reportedExternaluserId);
            }
        } else {
            report.setReportedExternalUserId(reportDto.getReportedExternalUserId());
        }

        report.setReportedUserId(reportDto.getReportedUserId());
        report.setReportContent(reportDto.getReportContent());
        report.setPostId(reportDto.getPostId());
        report.setReportedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        report.setRoomId(reportDto.getRoomId());
        report.setPostText(reportDto.getPostText());
        report.setReporterUserId(myId);
        report.setStatus(reportDto.getStatus());


        reportRepository.save(report);

        String reportedPostId = String.valueOf(reportDto.getPostId());


        // 🚀 1. 채팅 신고 (`roomId`가 있을 경우) → `reported/profile/`
        if (reportDto.getRoomId() != null && reportedUser.isPresent()) {
            String reportedExternaluserId = reportedUser.get().getExternalUserId();
            s3Uploader.syncReportedProfileImages(reportedExternaluserId);
        }

    }
}
