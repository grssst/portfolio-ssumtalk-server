package com.hottalk.hottalkserver.util;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.hottalk.hottalkserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class S3Uploader {

    private AmazonS3 s3Client;

    @Value("${S3_BUCKET}")
    private String bucket;

    @Value("${S3_REGION}")
    private String region;

    @Value("${S3_ACCESS_KEY}")
    private String accessKey;

    @Value("${S3_SECRET_KEY}")
    private String secretKey;

    @Value("${S3_ENDPOINT}")
    private String endpoint;

    @Autowired
    private UserRepository userRepository;
    @PostConstruct
    public void initializeAmazon() {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
    }

    public String uploadProfileImage(MultipartFile file, String externalUserId) throws IOException {
        File uploadFile = convertMultiPartToFile(file);
        String fileName = "profile/" + externalUserId + "/" +  "1/" + UUID.randomUUID().toString() + "_" + file.getOriginalFilename().replace(" ", "_");
        uploadFileToS3Bucket(fileName, uploadFile);
        uploadFile.delete();
        String cloudFrontDomain = "*********";
        String imageUrl = cloudFrontDomain + "/" + fileName;
        return imageUrl;

        //return s3Client.getUrl(bucket, fileName).toString();
    }

    public String uploadProfileImage2(MultipartFile file, String externalUserId) throws IOException {
        File uploadFile = convertMultiPartToFile(file);
        String fileName = "profile/" + externalUserId + "/" + "2/" +UUID.randomUUID().toString() + "_" + file.getOriginalFilename().replace(" ", "_");
        uploadFileToS3Bucket(fileName, uploadFile);
        uploadFile.delete();
        String cloudFrontDomain = "************";
        String imageUrl = cloudFrontDomain + "/" + fileName;
        return imageUrl;
    }

    public String uploadProfileImage3(MultipartFile file, String externalUserId) throws IOException {
        File uploadFile = convertMultiPartToFile(file);
        String fileName = "profile/" + externalUserId + "/" + "3/" + UUID.randomUUID().toString() + "_" + file.getOriginalFilename().replace(" ", "_");
        uploadFileToS3Bucket(fileName, uploadFile);
        uploadFile.delete();
        String cloudFrontDomain = "************";
        String imageUrl = cloudFrontDomain + "/" + fileName;
        return imageUrl;
    }

    public void deleteProfileImage(String externalUserId, int imageNumber) {
        try {
            // 데이터베이스에서 기존 파일 경로 조회
            List<Object[]> fileNames = userRepository.findAllProfileImageUrlsByExternalUserId(externalUserId);

            if (fileNames == null || fileNames.isEmpty()) {
                System.out.println("No images found for user: " + externalUserId);
                return;
            }
            //System.out.println("fileNames:" + fileNames);
            if (fileNames.get(0).length > 0 && fileNames.get(0)[0] != null && imageNumber == 1) {
                String fileName1 = fileNames.get(0)[0].toString();
                if (fileName1 != null && !fileName1.isEmpty() && !fileName1.equals("../assets/images/men.png") && !fileName1.equals("../assets/images/women.png")) {
                    int index = fileName1.indexOf("profile/");
                    if (index != -1) {
                        fileName1 = fileName1.substring(fileName1.indexOf("profile/")); // S3 키 값 추출
                        s3Client.deleteObject(bucket, fileName1);
                        System.out.println("Deleted existing profile image1: " + fileName1);
                    }
                }
            }

            if (fileNames.get(0).length > 1 && fileNames.get(0)[1] != null && imageNumber == 2) {
                String fileName2 = fileNames.get(0)[1].toString();
                //System.out.println("삭제할 이미지2:" + fileName2);
                if (fileName2 != null && !fileName2.isEmpty() && !fileName2.equals("../assets/images/men.png") && !fileName2.equals("../assets/images/women.png")) {
                    int index = fileName2.indexOf("profile/");
                    if (index != -1) {
                        fileName2 = fileName2.substring(fileName2.indexOf("profile/")); // S3 키 값 추출
                        s3Client.deleteObject(bucket, fileName2);
                        System.out.println("Deleted existing profile image2: " + fileName2);
                    }
                }
            }

            if (fileNames.get(0).length > 2 && fileNames.get(0)[2] != null && imageNumber == 3) {
                String fileName3 = fileNames.get(0)[2].toString();
                //System.out.println("삭제할 이미지3:" + fileName3);
                if (fileName3 != null && !fileName3.isEmpty() && !fileName3.equals("../assets/images/men.png") && !fileName3.equals("../assets/images/women.png")) {
                    int index = fileName3.indexOf("profile/");
                    if (index != -1) {
                        fileName3 = fileName3.substring(fileName3.indexOf("profile/")); // S3 키 값 추출
                        s3Client.deleteObject(bucket, fileName3);
                        System.out.println("Deleted existing profile image3: " + fileName3);
                    }
                }
            }

        } catch (AmazonS3Exception e) {
            System.out.println("Failed to delete profile image: " + e.getMessage());
        }
    }
    public String uploadPostImage(MultipartFile file, String postId) throws IOException {
        File uploadFile = convertMultiPartToFile(file);
        String fileName = "post/" + postId + "/" + UUID.randomUUID().toString() + "_" + file.getOriginalFilename().replace(" ", "_");
        uploadFileToS3Bucket(fileName, uploadFile);
        uploadFile.delete();
        // CloudFront 도메인을 사용하여 CDN URL 구성
        String cloudFrontDomain = "************";
        String imageUrl = cloudFrontDomain + "/" + fileName;
        return imageUrl;
    }

    public void deleteAllProfileImagesByExternalUserId(String externalUserId) {
        for (int i = 1; i <= 3; i++) {
            try {
                deleteProfileImage(externalUserId, i);
            } catch (Exception e) {
                System.err.println("❌ 프로필 이미지 " + i + " 삭제 실패: " + e.getMessage());
            }
        }
    }

    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename());
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }

    private void uploadFileToS3Bucket(String fileName, File file) {
        s3Client.putObject(new PutObjectRequest(bucket, fileName, file));
    }

    public void deletePostImage(String imageUrl) {
        try {
            // S3 URL에서 파일 키 추출
            String fileKey = extractFileKeyFromUrl(imageUrl);

            // AWS SDK v1에서 사용하는 DeleteObjectRequest
            s3Client.deleteObject(new DeleteObjectRequest(bucket, fileKey));

            System.out.println("✅ S3에서 삭제 완료: " + imageUrl);
        } catch (AmazonS3Exception e) {
            System.err.println("❌ S3 파일 삭제 실패: " + imageUrl);
            e.printStackTrace();
        }
    }

    public void deleteMeetingImage(String meetingId, Long myUserId) {
        try {
            // 예: chatting/meeting_144_  → 여기로 시작하는 모든 객체 삭제
            String directoryPrefix = "chatting/meeting_" + meetingId + "_";

            ObjectListing objectListing = s3Client.listObjects(bucket, directoryPrefix);
            List<DeleteObjectsRequest.KeyVersion> keysToDelete = new ArrayList<>();

            while (true) {
                for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                    String key = objectSummary.getKey();

                    // 안전하게 한 번 더 필터링 (혹시 몰라서)
                    if (key.startsWith(directoryPrefix)) {
                        keysToDelete.add(new DeleteObjectsRequest.KeyVersion(key));
                    }
                }

                if (objectListing.isTruncated()) {
                    objectListing = s3Client.listNextBatchOfObjects(objectListing);
                } else {
                    break;
                }
            }

            if (!keysToDelete.isEmpty()) {
                DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(bucket)
                        .withKeys(keysToDelete);
                s3Client.deleteObjects(deleteRequest);
                System.out.println("✅ S3 모임 이미지 전체 삭제 완료: " + directoryPrefix);
            } else {
                System.out.println("삭제할 객체가 없습니다: " + directoryPrefix);
            }

        } catch (AmazonS3Exception e) {
            System.err.println("❌ S3 모임 이미지 삭제 실패: meetingId=" + meetingId);
            e.printStackTrace();
        }
    }


    public void deleteChattingImage(String roomId) {
        try {
            // 삭제할 디렉토리 접두어 설정 (예: "chatting/12345/")
            String directoryKey = "chatting/" + roomId +"/";

            // 지정된 접두어를 가진 모든 객체 목록 조회
            ObjectListing objectListing = s3Client.listObjects(bucket, directoryKey);
            List<DeleteObjectsRequest.KeyVersion> keysToDelete = new ArrayList<>();

            // 조회한 객체들을 삭제 리스트에 추가
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                keysToDelete.add(new DeleteObjectsRequest.KeyVersion(objectSummary.getKey()));
            }

            // 객체가 여러 페이지로 분할되어 있다면 계속 조회
            while (objectListing.isTruncated()) {
                objectListing = s3Client.listNextBatchOfObjects(objectListing);
                for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                    keysToDelete.add(new DeleteObjectsRequest.KeyVersion(objectSummary.getKey()));
                }
            }

            // 삭제할 객체가 있으면 한 번에 삭제
            if (!keysToDelete.isEmpty()) {
                DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(bucket)
                        .withKeys(keysToDelete);
                s3Client.deleteObjects(deleteRequest);
                System.out.println("✅ S3에서 삭제 완료 (디렉토리 " + directoryKey + ")");
            } else {
                System.out.println("삭제할 객체가 없습니다: " + directoryKey);
            }
        } catch (AmazonS3Exception e) {
            System.err.println("❌ S3 파일 삭제 실패: " + roomId);
            e.printStackTrace();
        }
    }



    /**
     * S3 URL에서 파일 키 추출
     */
    private String extractFileKeyFromUrl(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            // URL의 path에서 선행 '/' 제거
            return url.getPath().substring(1);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL: " + imageUrl, e);
        }
    }


    // 🚀 신고된 게시글 이미지 저장 (파일명 유지, 중복 확인)
    public void syncReportedPostImage(String postId) {
        // 원본 폴더: post/{userId}/
        String originalPrefix = "post/" + postId + "/";
        ListObjectsV2Request originalReq = new ListObjectsV2Request()
                .withBucketName(bucket)
                .withPrefix(originalPrefix)
                .withMaxKeys(1);
        ListObjectsV2Result originalResult = s3Client.listObjectsV2(originalReq);

        if (originalResult.getObjectSummaries().isEmpty()) {
            System.out.println("원본 게시글 이미지 없음: " + originalPrefix);
            return;
        }

        String originalKey = originalResult.getObjectSummaries().get(0).getKey();
        String originalFileName = originalKey.substring(originalKey.lastIndexOf("/") + 1);

        // reported 폴더: reported/post/{userId}/
        String reportedPrefix = "reported/post/" + postId + "/";
        ListObjectsV2Request reportedReq = new ListObjectsV2Request()
                .withBucketName(bucket)
                .withPrefix(reportedPrefix)
                .withMaxKeys(1);
        ListObjectsV2Result reportedResult = s3Client.listObjectsV2(reportedReq);

        String reportedFileName = null;
        if (!reportedResult.getObjectSummaries().isEmpty()) {
            String repKey = reportedResult.getObjectSummaries().get(0).getKey();
            reportedFileName = repKey.substring(repKey.lastIndexOf("/") + 1);
        }

        // 만약 reported에 파일이 없거나, 파일명이 다르면 복사 진행
        if (reportedFileName == null || !originalFileName.equals(reportedFileName)) {
            String newReportedKey = reportedPrefix + originalFileName;
            s3Client.copyObject(new CopyObjectRequest(bucket, originalKey, bucket, newReportedKey));
            System.out.println("게시글 이미지 복사 완료: " + newReportedKey);
        } else {
            System.out.println("게시글 이미지 동일하므로 복사하지 않음.");
        }
    }



    // 🚀 신고된 프로필 이미지 저장 (파일명 유지, 중복 확인)
    public void syncReportedProfileImages(String externalUserId) {
        // 프로필 이미지는 1,2,3 폴더 각각 확인
        for (int i = 1; i <= 3; i++) {
            String originalPrefix = "profile/" + externalUserId + "/" + i + "/";
            ListObjectsV2Request originalReq = new ListObjectsV2Request()
                    .withBucketName(bucket)
                    .withPrefix(originalPrefix)
                    .withMaxKeys(1);
            ListObjectsV2Result originalResult = s3Client.listObjectsV2(originalReq);

            if (originalResult.getObjectSummaries().isEmpty()) {
                System.out.println("원본 프로필 이미지 없음: " + originalPrefix);
                continue;
            }

            String originalKey = originalResult.getObjectSummaries().get(0).getKey();
            String originalFileName = originalKey.substring(originalKey.lastIndexOf("/") + 1);

            String reportedPrefix = "reported/profile/" + externalUserId + "/" + i + "/";
            ListObjectsV2Request reportedReq = new ListObjectsV2Request()
                    .withBucketName(bucket)
                    .withPrefix(reportedPrefix)
                    .withMaxKeys(1);
            ListObjectsV2Result reportedResult = s3Client.listObjectsV2(reportedReq);

            String reportedFileName = null;
            if (!reportedResult.getObjectSummaries().isEmpty()) {
                String repKey = reportedResult.getObjectSummaries().get(0).getKey();
                reportedFileName = repKey.substring(repKey.lastIndexOf("/") + 1);
            }

            if (reportedFileName == null || !originalFileName.equals(reportedFileName)) {
                String newReportedKey = reportedPrefix + originalFileName;
                s3Client.copyObject(new CopyObjectRequest(bucket, originalKey, bucket, newReportedKey));
                System.out.println("프로필 이미지 복사 완료: " + newReportedKey);
            } else {
                System.out.println("프로필 이미지(" + i + ") 동일하므로 복사하지 않음.");
            }
        }
    }

    public String uploadMeetingProfileImage(MultipartFile file, String externalUserId) throws IOException {
        File uploadFile = convertMultiPartToFile(file);
        String fileName = "meeting/" + externalUserId + "/" + UUID.randomUUID().toString() + "_" + file.getOriginalFilename().replace(" ", "_");
        uploadFileToS3Bucket(fileName, uploadFile);
        String cloudFrontDomain = "https://d2rhx7q10awn3j.cloudfront.net";
        String imageUrl = cloudFrontDomain + "/" + fileName;
        return imageUrl;

        //return s3Client.getUrl(bucket, fileName).toString();
    }

    public String uploadMeetingGalleryImage(MultipartFile file, String meetingId) throws IOException {
        File uploadFile = convertMultiPartToFile(file);
        String fileName = "gallery/" + meetingId + "/" + UUID.randomUUID().toString() + "_" +
                file.getOriginalFilename().replace(" ", "_");
        uploadFileToS3Bucket(fileName, uploadFile);
        uploadFile.delete();
        // CloudFront 도메인을 사용하여 CDN URL 구성
        String cloudFrontDomain = "https://d2rhx7q10awn3j.cloudfront.net";
        String imageUrl = cloudFrontDomain + "/" + fileName;
        return imageUrl;
    }

    public String uploadChattingImage(MultipartFile file, String roomId) throws IOException {
        File uploadFile = convertMultiPartToFile(file);
        String fileName = "chatting/" + roomId + "/" + UUID.randomUUID().toString() + "_" +
                file.getOriginalFilename().replace(" ", "_");
        uploadFileToS3Bucket(fileName, uploadFile);
        uploadFile.delete();
        // CloudFront 도메인을 사용하여 CDN URL 구성
        String cloudFrontDomain = "https://d2rhx7q10awn3j.cloudfront.net";
        String imageUrl = cloudFrontDomain + "/" + fileName;
        return imageUrl;
    }


    public void deleteGalleryImage(String imageUrl) {
        try {
            // S3 URL에서 파일 키 추출
            String fileKey = extractFileKeyFromUrl(imageUrl);

            // AWS SDK v1에서 사용하는 DeleteObjectRequest
            s3Client.deleteObject(new DeleteObjectRequest(bucket, fileKey));

            System.out.println("✅ S3에서 삭제 완료: " + imageUrl);
        } catch (AmazonS3Exception e) {
            System.err.println("❌ S3 파일 삭제 실패: " + imageUrl);
            e.printStackTrace();
        }
    }

    public void backupReportChattingImage(String imageUrl, String roomId) {
        try {
            // imageUrl에서 S3 객체 키 추출 (예: "chatting/room_23_92/UUID_filename.jpg")
            String fileKey = extractFileKeyFromUrl(imageUrl);

            // 파일명 추출: 파일 키의 마지막 '/' 이후의 문자열
            int lastSlashIndex = fileKey.lastIndexOf("/");
            String fileName = (lastSlashIndex != -1) ? fileKey.substring(lastSlashIndex + 1) : fileKey;

            // 백업 대상 경로 구성: 예를 들어 "reported/chatting/{roomId}/{fileName}"
            String backupKey = "reported/chatting/" + roomId + "/" + fileName;

            // 원본 파일을 백업 대상 경로로 복사 (AWS SDK v1의 CopyObjectRequest 사용)
            CopyObjectRequest copyRequest = new CopyObjectRequest(bucket, fileKey, bucket, backupKey);
            s3Client.copyObject(copyRequest);
            System.out.println("✅ 신고 채팅 이미지 백업 완료: " + backupKey);

            // 백업이 성공하면 원본 파일 삭제 (DeleteObjectRequest 사용)
            s3Client.deleteObject(new DeleteObjectRequest(bucket, fileKey));
            System.out.println("✅ 원본 이미지 삭제 완료: " + fileKey);
        } catch (Exception e) {
            System.err.println("❌ 이미지 백업 및 삭제 실패: " + imageUrl);
            e.printStackTrace();
        }
    }



}
