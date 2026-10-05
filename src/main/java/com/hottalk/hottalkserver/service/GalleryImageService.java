package com.hottalk.hottalkserver.service;


import com.hottalk.hottalkserver.model.GalleryImage;
import com.hottalk.hottalkserver.repository.GalleryImageRepository;
import com.hottalk.hottalkserver.util.S3Uploader;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GalleryImageService {

    @Autowired
    private GalleryImageRepository galleryImageRepository;
    @Autowired
    private S3Uploader s3Uploader;

    public String uploadAndSaveImage(MultipartFile file, String meetingId) throws IOException {
        // S3에 이미지 업로드
        String fileUrl = s3Uploader.uploadMeetingGalleryImage(file, meetingId);

        // DB에 저장
        GalleryImage image = new GalleryImage();
        image.setImageUrl(fileUrl);
        image.setMeetingId(meetingId);
        galleryImageRepository.save(image);

        return fileUrl;
    }

    /**
     * meetingId에 해당하는 갤러리 이미지 목록 조회
     */
    public List<GalleryImage> getGalleryImagesByMeetingId(String meetingId) {
        return galleryImageRepository.findByMeetingIdOrderByCreatedAtDesc(meetingId);
    }

    public void deleteImage(Long imageId, String meetingId, String imageUrl) {
        // DB에서 이미지 레코드 조회
        GalleryImage image = galleryImageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("이미지를 찾을 수 없습니다."));
        // meetingId 일치 여부 확인 (보안 체크)
        if (!image.getMeetingId().equals(meetingId)) {
            throw new RuntimeException("모임 ID가 일치하지 않습니다.");
        }
        // (필요한 경우 imageUrl도 비교할 수 있습니다.)
        // S3 삭제 로직은 여기서 추가하면 됩니다.
        // 예: s3Uploader.deleteImage(imageUrl);
        s3Uploader.deleteGalleryImage(imageUrl);
        // DB 레코드 삭제
        galleryImageRepository.delete(image);
    }

}
