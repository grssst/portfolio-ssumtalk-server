package com.hottalk.hottalkserver.controller;

import com.hottalk.hottalkserver.model.GalleryImage;
import com.hottalk.hottalkserver.service.GalleryImageService;
import com.hottalk.hottalkserver.util.S3Uploader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gallery")
public class GalleryImageController {

    @Autowired
    private GalleryImageService galleryImageService;


    @PostMapping("/upload")
    public ResponseEntity<?> uploadMeetingGalleryImage(
            @RequestHeader("Authorization") String token,
            @RequestParam("image") MultipartFile file,
            @RequestParam("meetingId") String meetingId) {
        try {
            // Service를 통해 S3 업로드 및 DB 저장 처리
            String fileUrl = galleryImageService.uploadAndSaveImage(file, meetingId);
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", fileUrl);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("이미지 업로드 실패: " + e.getMessage());
        }
    }

    // meetingId에 해당하는 갤러리 이미지 목록 조회
    @GetMapping("/getImages")
    public ResponseEntity<?> getGalleryImages(
            @RequestHeader("Authorization") String token,
            @RequestParam("meetingId") String meetingId) {
        try {
            List<GalleryImage> images = galleryImageService.getGalleryImagesByMeetingId(meetingId);
            return ResponseEntity.ok(images);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("갤러리 이미지를 불러오는 중 오류 발생: " + e.getMessage());
        }
    }

    @DeleteMapping("/deleteImage")
    public ResponseEntity<?> deleteImage(
            @RequestParam("imageId") Long imageId,
            @RequestParam("meetingId") String meetingId,
            @RequestParam("imageUrl") String imageUrl) {
        try {
            // S3 삭제 로직은 별도로 구현 (예: s3Uploader.deleteImage(...))
            // 여기서는 DB에서 레코드 삭제만 처리합니다.
            galleryImageService.deleteImage(imageId, meetingId, imageUrl);
            return ResponseEntity.ok("이미지가 삭제되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("이미지 삭제 실패: " + e.getMessage());
        }
    }
}
