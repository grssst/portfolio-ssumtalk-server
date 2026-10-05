package com.hottalk.hottalkserver.controller;


import com.hottalk.hottalkserver.dto.*;
import com.hottalk.hottalkserver.model.Meeting;
import com.hottalk.hottalkserver.model.MeetingParticipant;
import com.hottalk.hottalkserver.model.Post;
import com.hottalk.hottalkserver.model.User;
import com.hottalk.hottalkserver.repository.UserRepository;
import com.hottalk.hottalkserver.service.MeetingService;
import com.hottalk.hottalkserver.service.UserService;
import com.hottalk.hottalkserver.util.JwtUtil;
import com.hottalk.hottalkserver.util.S3Uploader;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/meeting")
public class MeetingController {
    private final MeetingService meetingService;
    private final UserRepository userRepository;


    @Autowired
    public MeetingController(
            MeetingService meetingService, UserRepository userRepository) {
        this.meetingService = meetingService;
        this.userRepository = userRepository;
    }


    @PostMapping(value = "/create-meeting", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createMeeting(
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("topic") String topic,
            @RequestParam("location") String location,
            @RequestParam("maxParticipants") int maxParticipants,
            @RequestHeader("Authorization") String token) {

        try {
            // 1) 토큰에서 externalUserId 추출
            String externalUserId = JwtUtil.validateToken(token.substring(7));

            // 2) 외부ID로 User 엔티티 조회 (Optional 처리)
            User creator = userRepository
                    .findByExternalUserId(externalUserId)
                    .orElseThrow(() ->
                            new RuntimeException("사용자 정보가 없습니다: " + externalUserId)
                    );
            Long creatorUserId = creator.getId();

            // 3) Meeting 엔티티 생성 및 저장
            Meeting meeting = new Meeting();
            meeting.setExternalUserId(externalUserId);
            meeting.setTitle(title);
            meeting.setDescription(description);
            meeting.setTopic(topic);
            meeting.setLocation(location);
            meeting.setMaxParticipants(maxParticipants);
            meeting.setWriterNickname(creator.getNickname());

            Long meetingId;
            try {
                meetingId = meetingService.saveMeeting(meeting, image);
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to save meeting.");
            }

            // 4) 응답 DTO 생성
            Map<String, Long> response = new HashMap<>();
            response.put("meetingId", meetingId);
            response.put("creatorUserId", creatorUserId);

            return ResponseEntity.ok(response);

        } catch (JwtException e) {
            // 토큰 검증 실패
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid token.");
        } catch (RuntimeException e) {
            // 사용자 조회 실패 등
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }


    @DeleteMapping("/kickMember")
    public ResponseEntity<?> kickMember(
            @RequestParam Long meetingId,
            @RequestParam String externalUserId,
            @RequestHeader("Authorization") String token
    ) {
        try {
            // 1) 토큰에서 추출한 내 externalUserId (모임장인지 확인)
            String organizerExternalUserId = JwtUtil.validateToken(token.substring(7));

            // 2) 추방 로직
            meetingService.kickMember(meetingId, organizerExternalUserId, externalUserId);
            return ResponseEntity.ok("추방이 완료되었습니다.");

        } catch (Exception e) {
            // 그 외 에러
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("멤버 추방 중 오류가 발생했습니다.");
        }
    }

    public User getUserProfile(String externalUserId) {
        // 외부 사용자 ID로 사용자 정보를 조회
        Optional<User> userOptional = userRepository.findByExternalUserId(externalUserId);

        // 사용자 정보를 찾으면 반환, 그렇지 않으면 HTTP 에러 코드 404 반환
        return userOptional.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );
    }

    @GetMapping("/get-meeting")
    public ResponseEntity<List<MeetingDTO>> getMeeting(@RequestHeader("Authorization") String token,
                                                         @RequestParam String location, @RequestParam String selectedTopics) {
        try {
            // "Bearer " 부분을 제거하고 토큰만 추출
            String jwtToken = token.replace("Bearer ", "");
            // JWT 토큰을 검증하고 externalUserId 추출
            String externalUserId = JwtUtil.validateToken(jwtToken);


            System.out.println("지역:" + location);
            System.out.println("주제:" + selectedTopics);


            List<MeetingDTO> meetings = meetingService.getAllMeetings(externalUserId, location, selectedTopics);


            return ResponseEntity.ok(meetings);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/join-meeting")
    public ResponseEntity<?> joinMeeting(@RequestHeader("Authorization") String token, @RequestParam Long meetingId, @RequestParam(required = false, defaultValue = "false") boolean firstCreate) {
        try {
            // "Bearer " 부분을 제거하고 토큰만 추출
            String jwtToken = token.replace("Bearer ", "");
            // JWT 토큰을 검증하고 externalUserId 추출
            String externalUserId = JwtUtil.validateToken(jwtToken);

            meetingService.joinMeeting(externalUserId, meetingId, firstCreate);

            return ResponseEntity.ok("ok");
        } catch (IllegalArgumentException ex) {
            // 모임 대표 참가 또는 차단 관계일 경우 400 상태 코드를 반환
            if ("모임 대표는 참가할 수 없습니다.".equals(ex.getMessage())
                    || "차단 관계로 인해 참가할 수 없습니다.".equals(ex.getMessage())
                    || "이미 참가한 사용자입니다.".equals(ex.getMessage())
                    || "참가자 수가 꽉 찼습니다.".equals(ex.getMessage())) {
                return ResponseEntity.badRequest().body(ex.getMessage());
            }
            ex.printStackTrace();
            return ResponseEntity.status(500).body(null);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/fetch-members")
    public ResponseEntity<List<MeetingParticipantUserDto>> fetchMembers(
            @RequestHeader("Authorization") String token,
            @RequestParam Long meetingId) {
        try {
            // "Bearer " 부분 제거 후 JWT 토큰 검증
            String jwtToken = token.replace("Bearer ", "");
            String externalUserId = JwtUtil.validateToken(jwtToken);

            List<MeetingParticipantUserDto> dtos = meetingService.getMembers(meetingId);
            if (dtos == null || dtos.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            return ResponseEntity.ok(dtos);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    @GetMapping("/get-meeting-info")
    public ResponseEntity<MeetingInfoDto> getMeetingInfo(
            @RequestHeader("Authorization") String token,
            @RequestParam Long meetingId) {
        try {
            // "Bearer " 제거 후 토큰 검증 (JwtUtil.validateToken 메서드 사용)
            String jwtToken = token.replace("Bearer ", "");
            String externalUserId = JwtUtil.validateToken(jwtToken);

            MeetingInfoDto dto = meetingService.getMeetingInfo(token, meetingId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/extendMeeting")
    public ResponseEntity<String> extendMeeting(@RequestParam Long meetingId,
                                                @RequestParam int months) {

        return meetingService.extendMeeting(meetingId, months);
    }

    @PostMapping("/register-periodic")
    public ResponseEntity<?> registerPeriodicMeeting(
            @RequestHeader("Authorization") String token,
            @RequestParam Long meetingId,
            @RequestBody PeriodicMeetingRegistrationDto request) {
        try {
            // "Bearer " 제거 후 JWT 토큰 검증
            String jwtToken = token.replace("Bearer ", "");
            String externalUserId = JwtUtil.validateToken(jwtToken);

            meetingService.registerPeriodicMeeting(externalUserId, meetingId, request);
            return ResponseEntity.ok("정기모임 등록 완료!");
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류 발생");
        }
    }

    @GetMapping("/fetch-periodic")
    public ResponseEntity<List<PeriodicMeetingDto>> fetchPeriodicMeetings(
            @RequestHeader("Authorization") String token,
            @RequestParam Long meetingId) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            String externalUserId = JwtUtil.validateToken(jwtToken);

            List<PeriodicMeetingDto> dtos = meetingService.getPeriodicMeetings(meetingId);
            return ResponseEntity.ok(dtos);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/leave-meeting")
    public ResponseEntity<?> leaveMeeting(
            @RequestHeader("Authorization") String token,
            @RequestParam String meetingId) {
        try {
            // "Bearer " 제거 후 토큰 검증 (JwtUtil.validateToken 메서드 사용)
            String jwtToken = token.replace("Bearer ", "");
            String externalUserId = JwtUtil.validateToken(jwtToken);

            meetingService.leaveMeeting(externalUserId, meetingId);
            return ResponseEntity.ok("ok");
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/delete-meeting")
    public ResponseEntity<?> deleteMeeting(
            @RequestHeader("Authorization") String token,
            @RequestParam String meetingId,
            @RequestParam Long myUserId) {
        try {
            // "Bearer " 제거 후 토큰 검증 (JwtUtil.validateToken 메서드 사용)
            String jwtToken = token.replace("Bearer ", "");
            String externalUserId = JwtUtil.validateToken(jwtToken);

            System.out.println("🔹 전달된 meetingId: " + meetingId);

            // 🔹 meetingId 유효성 검사
            Long meetingIdLong;
            try {
                // "meeting_31_21"에서 "31"을 추출
                String[] parts = meetingId.split("_");
                if (parts.length < 2) {
                    return ResponseEntity.badRequest().body("잘못된 meetingId 형식입니다.");
                }

                meetingIdLong = Long.valueOf(parts[1]); // 두 번째 요소(31) 추출

            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body("잘못된 meetingId 형식입니다.");
            }

            // 🔹 모임 삭제 처리
            meetingService.deleteMeeting(externalUserId, meetingIdLong, myUserId);
            return ResponseEntity.ok("ok");

        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            return ResponseEntity.badRequest().body(ex.getMessage()); // 에러 메시지 반환
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("서버 내부 오류 발생");
        }
    }

    @PostMapping("/update-meeting-info")
    public ResponseEntity<String> updateMeetingInfo(@RequestParam Long meetingId,
                                                    @RequestBody MeetingUpdateRequestDTO updateRequest,
                                                    @RequestHeader("Authorization") String token) {
        // 실제 구현에서는 JWT 라이브러리를 이용하여 토큰에서 externalUserId를 추출하세요.
        String jwtToken = token.replace("Bearer ", "");
        String externalUserId = JwtUtil.validateToken(jwtToken);
        try {
            Meeting updatedMeeting = meetingService.updateMeetingInfo(meetingId, updateRequest, externalUserId);
            return ResponseEntity.ok("Meeting updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value = "/update-meeting-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateMeetingImage(
            @RequestParam Long meetingId,
            @RequestParam("image") MultipartFile image,
            @RequestHeader("Authorization") String token) {

        // JWT 토큰에서 externalUserId 추출 (실제 구현 시 JWT 라이브러리 사용)
        String jwtToken = token.replace("Bearer ", "");
        String externalUserId = JwtUtil.validateToken(jwtToken);

        try {
            // 모임 이미지 업데이트 서비스 호출
            Meeting updatedMeeting = meetingService.updateMeetingImage(meetingId, image, externalUserId);
            // 업데이트된 이미지 URL을 응답 DTO로 반환 (원하는 형태로 수정)
            ImageUpdateResponseDTO responseDTO = new ImageUpdateResponseDTO(updatedMeeting.getImageUrl());
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 정기모임 삭제 API
     * DELETE /api/meeting/deletePeriodic?periodicId={periodicId}&meetingId={meetingId}
     */
    @DeleteMapping("/deletePeriodic")
    public ResponseEntity<?> deletePeriodicMeeting(
            @RequestParam("periodicId") Long periodicId,
            @RequestParam("meetingId") Long meetingId) {
        try {
            meetingService.deletePeriodicMeeting(periodicId, meetingId);
            return ResponseEntity.ok("정기모임이 삭제되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("정기모임 삭제 실패: " + e.getMessage());
        }
    }

}
