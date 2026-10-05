package com.hottalk.hottalkserver.service;

import com.hottalk.hottalkserver.dto.PostWithUserDTO;
import com.hottalk.hottalkserver.dto.UserProfileDTO;
import com.hottalk.hottalkserver.model.BalanceHistory;
import com.hottalk.hottalkserver.model.Post;
import com.hottalk.hottalkserver.model.User;
import com.hottalk.hottalkserver.repository.BalanceHistoryRepository;
import com.hottalk.hottalkserver.repository.PostRepository;
import com.hottalk.hottalkserver.repository.UserRepository;
import com.hottalk.hottalkserver.util.GeoUtils;
import com.hottalk.hottalkserver.util.S3Uploader;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BalanceHistoryRepository balanceHistoryRepository;

    @Autowired
    private S3Uploader s3Uploader;

    @Transactional
    public void savePost(Post post, MultipartFile image) throws IOException {
        // 게시글을 작성한 User의 위치 정보를 가져옴

        User user = userRepository.findByExternalUserId(post.getExternalUserId())
                .orElseThrow(() -> new RuntimeException("User not found with externalUserId: " + post.getExternalUserId()));

            post.setLocation(user.getLocation());
            post.setAge(user.getAge());
            post.setGender(user.getGender());
            post.setUserId(user.getId());
            post.setProfileImageUrl(user.getProfileImageUrl());
            post.setNickname(user.getNickname());
            post.setStatus(user.getStatus());
            post.setMyLocation(user.getMyLocation());
            post.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
            // 만약 마지막 게시글 작성 날짜가 오늘이 아니라면, 카운터를 초기화
            if (user.getLastPostDate() == null || !user.getLastPostDate().equals(today)) {
                user.setDailyPostCount(0);
                user.setLastPostDate(today);
            }

            // 카운터 증가
            user.setDailyPostCount(user.getDailyPostCount() + 1);

            // 만약 일일 게시글 작성 횟수가 3회 이하이면 캔디 지급 (예: 3개 지급)
            if (user.getDailyPostCount() <= 5) {


                // balance_history 테이블에 적립 내역 기록 생성
                BalanceHistory history = new BalanceHistory();
                history.setUserId(user.getId());
                // changeType은 'send_message' 혹은 '쪽지 전송' 등으로 구분할 수 있습니다.
                history.setChangeType("write_reward");
                // 사용 내역은 음수로 기록 (예: -3)
                history.setAmount(+3);
                history.setBalanceBefore(user.getCandy());
                history.setBalanceAfter(user.getCandy() + 3);
                // 관련 거래가 없으므로 relatedTransactionId는 null로 둡니다.
                balanceHistoryRepository.save(history);



                user.setCandy(user.getCandy() + 3);
            }

            // 업데이트된 정보를 저장 (트랜잭션 내에서 커밋)
            userRepository.save(user);


        if (image != null && !image.isEmpty()) {
            postRepository.save(post);  // 먼저 post를 저장하여 ID를 생성
            String imageUrl = s3Uploader.uploadPostImage(image, String.valueOf(post.getId()));
            post.setImageUrl(imageUrl);
        }
        postRepository.save(post); // 이미지 URL 설정 후 다시 저장

    }

    public class DefaultLocation {
        public static Point createDefaultPoint() {
            GeometryFactory geometryFactory = new GeometryFactory();
            Point point = geometryFactory.createPoint(new Coordinate(128.000000, 37.000000)); // 기본 좌표
            point.setSRID(4326); // SRID 설정
            return point;
        }
    }


    public List<PostWithUserDTO> getAllPostsWithUserInfoAndCoordinates(String externalUserId, Map<String, String> selectedFilter) {
        try {
            Optional<User> currentUser = userRepository.findByExternalUserId(externalUserId);  //유저목록을 호출한 나
            Point myLocation = DefaultLocation.createDefaultPoint(); // 기본값 설정
            if (currentUser.isPresent() && currentUser.get().getLocation() != null) {
                myLocation = currentUser.get().getLocation(); // 유효한 location이 있으면 덮어씀. point 타입
            }
            //System.out.println(myLocation);
            double latitude = myLocation.getY();  // 나의 위도
            double longitude = myLocation.getX(); // 나의 경도
            //System.out.println(latitude);
            double distanceKm = 30; // 20km 반경
            double maxDistanceKm = 540; // 최대 반경 500km

            String sort = selectedFilter.get("sort");
            String gender = selectedFilter.get("gender");
            Integer ageRange1 = Integer.valueOf(selectedFilter.get("ageRange1"));
            Integer ageRange2 = Integer.valueOf(selectedFilter.get("ageRange2"));

            List<Post> posts = null;
            if (Objects.equals(sort, "distance")) { // 거리순 정렬 초기요청 개발 완료
                while (distanceKm <= maxDistanceKm) {
                    Envelope boundingBox = GeoUtils.calculateBoundingBox(latitude, longitude, distanceKm);
                    String wktBoundingBox = GeoUtils.envelopeToWKT(boundingBox);
                    //System.out.println("Bounding Box: " + wktBoundingBox);

                    if (Objects.equals(gender, "all")) { // 모든 성별
                        posts = postRepository.findPostsWithinBoundingBox(wktBoundingBox, myLocation, externalUserId, ageRange1, ageRange2);
                    } else { // 특정 성별
                        posts = postRepository.findPostsWithinBoundingBoxAndGender(wktBoundingBox, gender, myLocation, externalUserId, ageRange1, ageRange2);
                    }

                    if (posts.size() >= 70) {
                        System.out.println("데이터 70개 찾음");
                        break; // 100개 이상의 데이터가 쿼리되면 중단
                    }
                    System.out.println("쿼리된 데이터 개수:" + posts.size());
                    distanceKm += 80; // 범위를 80km씩 증가
                    System.out.println("범위를 80km 확장합니다. 확장된 범위:" + distanceKm);
                }
            } else if (Objects.equals(sort, "time")) {      //최신순 정렬 개발완료
                // 시간 기반 쿼리 수행
                if (Objects.equals(gender, "all")) {   //전체성별 + 최신순
                    posts = postRepository.findTop70ByAgeRangeAndOrderByIdDesc(externalUserId, ageRange1, ageRange2);
                } else {    //특정 성별 + 시간순
                    posts = postRepository.findTop70ByGenderAndAgeRangeAndOrderByIdDesc(gender, externalUserId, ageRange1, ageRange2);
                }
            }



            return posts.stream().map(post -> {
                Point userLocation = currentUser.isPresent() ? currentUser.get().getLocation() : null;
                Integer dailyPostCount = currentUser.isPresent() ? currentUser.get().getDailyPostCount() : null;
                Double postLatitude = null;
                Double postLongitude = null;
                if (post.getLocation() != null) {
                    postLatitude = post.getLocation().getY();
                    postLongitude = post.getLocation().getX();
                }

                Double userLatitude = null;
                Double userLongitude = null;
                if (userLocation != null) {
                    userLatitude = userLocation.getY();
                    userLongitude = userLocation.getX();
                }

                return new PostWithUserDTO(
                        post.getId(),
                        post.getContent(),
                        post.getImageUrl(),
                        post.getCreatedAt(),
                        post.getUpdatedAt(),
                        post.getDeletedAt(),
                        post.getUserId(),
                        post.getExternalUserId(),
                        postLatitude,
                        postLongitude,
                        post.getNickname(),
                        post.getGender(),
                        post.getAge(),
                        post.getProfileImageUrl(),
                        post.getStatus(),
                        post.getMyLocation(),
                        userLatitude,
                        userLongitude,
                        dailyPostCount
                );
            }).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<PostWithUserDTO> getMorePostsFromLastPostId(String externalUserId, Map<String, String> selectedFilter) {
        try {
            Optional<User> currentUser = userRepository.findByExternalUserId(externalUserId);  //유저목록을 호출한 나
            Point myLocation = PostService.DefaultLocation.createDefaultPoint(); // 기본값 설정
            if (currentUser.isPresent() && currentUser.get().getLocation() != null) {
                myLocation = currentUser.get().getLocation(); // 유효한 location이 있으면 덮어씀
            }

            double latitude = myLocation.getY();  // 나의 위도
            double longitude = myLocation.getX(); // 나의 경도

            double distanceKm = 30; // 20km 반경
            double maxDistanceKm = 540; // 최대 반경 500km

            String sort = selectedFilter.get("sort");
            String gender = selectedFilter.get("gender");
            Integer ageRange1 = Integer.valueOf(selectedFilter.get("ageRange1"));
            Integer ageRange2 = Integer.valueOf(selectedFilter.get("ageRange2"));
            //long lastPostId = Long.parseLong(selectedFilter.get("lastPostId"));
            // lastDistance 처리
            String lastDistanceStr = selectedFilter.get("lastDistance");
            Double lastDistance = null;

            if (lastDistanceStr != null && !lastDistanceStr.isEmpty()) {
                try {
                    lastDistance = Double.valueOf(lastDistanceStr);
                } catch (NumberFormatException e) {
                    System.out.println("lastDistance 값이 유효하지 않습니다: " + lastDistanceStr);
                    lastDistance = null; // 기본값으로 설정하거나 null로 유지
                }
            }
            String lastPostIdStr = selectedFilter.get("lastPostId");

            // lastPostId가 null 또는 "null"일 경우 처리
            if (lastPostIdStr == null || lastPostIdStr.equals("null")) {
                return new ArrayList<>(); // 빈 리스트 리턴
            }

            long lastPostId = Long.parseLong(lastPostIdStr);
            System.out.println("라스트포스트아이디:" + lastPostId);
            List<Post> posts = null;
            if (Objects.equals(sort, "distance")) { // 거리순 정렬
                while (distanceKm <= maxDistanceKm) {
                    Envelope boundingBox = GeoUtils.calculateBoundingBox(latitude, longitude, distanceKm);
                    String wktBoundingBox = GeoUtils.envelopeToWKT(boundingBox);
                    //System.out.println("Bounding Box: " + wktBoundingBox);

                    if (Objects.equals(gender, "all")) { // 모든 성별
                        posts = postRepository.findPostsWithinBoundingBoxAndMinDistance(wktBoundingBox, myLocation, lastDistance, externalUserId, ageRange1, ageRange2);
                    } else { // 특정 성별
                        posts = postRepository.findPostsWithinBoundingBoxAndGenderAndMinDistance(wktBoundingBox, gender, myLocation, lastDistance, externalUserId, ageRange1, ageRange2);
                    }

                    if (posts.size() >= 70) {
                        System.out.println("데이터 70개 찾음");
                        break; // 70개 이상의 데이터가 쿼리되면 중단
                    }
                    System.out.println("쿼리된 데이터 개수:" + posts.size());
                    distanceKm += 80; // 범위를 80km씩 증가
                    System.out.println("범위를 80km 확장합니다. 확장된 범위:" + distanceKm);
                }
            }
            else if (Objects.equals(sort, "time")) {   //시간순
                // 시간 기반 쿼리 수행
                if (Objects.equals(gender, "all")) {  //모든 성별
                    posts = postRepository.findTop70ByCreatedAtAndIdBeforeOrderByIdDesc(externalUserId, lastPostId, ageRange1, ageRange2);
                } else {        //특정성별
                    posts = postRepository.findTop70ByGenderAndIdBeforeOrderByIdDesc(gender, externalUserId, lastPostId, ageRange1, ageRange2);
                }
            }



            return posts.stream().map(post -> {
                Point userLocation = currentUser.isPresent() ? currentUser.get().getLocation() : null;
                Integer dailyPostCount = currentUser.isPresent() ? currentUser.get().getDailyPostCount() : null;
                Double postLatitude = null;
                Double postLongitude = null;
                if (post.getLocation() != null) {
                    postLatitude = post.getLocation().getY();
                    postLongitude = post.getLocation().getX();
                }

                Double userLatitude = null;
                Double userLongitude = null;
                if (userLocation != null) {
                    userLatitude = userLocation.getY();
                    userLongitude = userLocation.getX();
                }

                return new PostWithUserDTO(
                        post.getId(),
                        post.getContent(),
                        post.getImageUrl(),
                        post.getCreatedAt(),
                        post.getUpdatedAt(),
                        post.getDeletedAt(),
                        post.getUserId(),
                        post.getExternalUserId(),
                        postLatitude,
                        postLongitude,
                        post.getNickname(),
                        post.getGender(),
                        post.getAge(),
                        post.getProfileImageUrl(),
                        post.getStatus(),
                        post.getMyLocation(),
                        userLatitude,
                        userLongitude,
                        dailyPostCount
                );
            }).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
