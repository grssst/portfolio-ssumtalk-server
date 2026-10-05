package com.hottalk.hottalkserver.controller;

import com.hottalk.hottalkserver.dto.PostWithUserDTO;
import com.hottalk.hottalkserver.model.Post;
import com.hottalk.hottalkserver.service.PostService;
import com.hottalk.hottalkserver.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @PostMapping("/create-post")
    public ResponseEntity<?> createPost(
            @RequestPart("content") String content,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestHeader("Authorization") String token) {

        try {
            String externalUserId = JwtUtil.validateToken(token.substring(7)); // "Bearer " 제거

            Post post = new Post();
            post.setContent(content);
            post.setExternalUserId(externalUserId);

            try {
                postService.savePost(post, image);

                return ResponseEntity.ok("Post created successfully.");
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.status(500).body("Failed to create post.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Invalid token.");
        }
    }

    @GetMapping("/get-post")
    public ResponseEntity<List<PostWithUserDTO>> getPosts(@RequestHeader("Authorization") String token, @RequestParam Map<String, String> selectedFilter) {
        try {

            String externalUserId = JwtUtil.validateToken(token.substring(7)); // "Bearer " 제거
            List<PostWithUserDTO> postWithUser = postService.getAllPostsWithUserInfoAndCoordinates(externalUserId, selectedFilter);

            return ResponseEntity.ok(postWithUser);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/get-more-post")
    public ResponseEntity<List<PostWithUserDTO>> getMorePosts(@RequestHeader("Authorization") String token, @RequestParam Map<String, String> selectedFilter) {
        try {
            System.out.println("요청 받음");
            String externalUserId = JwtUtil.validateToken(token.substring(7)); // "Bearer " 제거
            System.out.println("선택된 필터: " + selectedFilter);

            List<PostWithUserDTO> postWithUser = postService.getMorePostsFromLastPostId(externalUserId, selectedFilter);  //externalUserId, gender, sort, lastDistance, lastLogin
            return ResponseEntity.ok(postWithUser);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

}
