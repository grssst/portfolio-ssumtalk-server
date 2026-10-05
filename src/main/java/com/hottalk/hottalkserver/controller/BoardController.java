package com.hottalk.hottalkserver.controller;



import com.hottalk.hottalkserver.dto.BoardRequestDto;
import com.hottalk.hottalkserver.dto.BoardResponseDto;
import com.hottalk.hottalkserver.service.BoardService;
import com.hottalk.hottalkserver.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/board")
public class BoardController {

    @Autowired
    private BoardService boardService;

    @PostMapping("/createBoard")
    public ResponseEntity<?> createBoard(
            @RequestHeader("Authorization") String token,
            @RequestParam Long meetingId,
            @RequestBody BoardRequestDto boardRequestDto) {
        try {
            // "Bearer " 접두사를 제거한 후 JWT 토큰 검증 (구현되어 있다고 가정)
            String jwtToken = token.replace("Bearer ", "");
            String externalUserId = JwtUtil.validateToken(jwtToken);

            boardService.createBoard(externalUserId, meetingId, boardRequestDto);
            return ResponseEntity.ok("게시판 글 등록 완료!");
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류 발생");
        }
    }

    @GetMapping("/getBoard")
    public ResponseEntity<?> getBoardPosts(
            @RequestHeader("Authorization") String token,
            @RequestParam Long meetingId) {
        try {
            // "Bearer " 접두사를 제거 후 JWT 토큰 검증
            String jwtToken = token.replace("Bearer ", "");
            String externalUserId = JwtUtil.validateToken(jwtToken);
            if (externalUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유효하지 않은 토큰입니다.");
            }
            List<BoardResponseDto> boardPosts = boardService.getBoardPosts(meetingId);
            return ResponseEntity.ok(boardPosts);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류 발생");
        }
    }

    @DeleteMapping("/deleteBoard")
    public ResponseEntity<?> deleteBoardPost(
            @RequestParam("postId") Long postId,
            @RequestParam("meetingId") Long meetingId) {
        try {
            boardService.deleteBoardPost(postId, meetingId);
            return ResponseEntity.ok("게시글이 삭제되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("게시글 삭제 실패: " + e.getMessage());
        }
    }
}
