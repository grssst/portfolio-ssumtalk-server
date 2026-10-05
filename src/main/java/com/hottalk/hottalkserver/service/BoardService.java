package com.hottalk.hottalkserver.service;

import com.hottalk.hottalkserver.dto.BoardRequestDto;
import com.hottalk.hottalkserver.dto.BoardResponseDto;
import com.hottalk.hottalkserver.model.Board;
import com.hottalk.hottalkserver.model.User;
import com.hottalk.hottalkserver.repository.BoardRepository;
import com.hottalk.hottalkserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BoardService {

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private UserRepository userRepository; // UserService 대신 UserRepository 직접 주입

    public Board createBoard(String externalUserId, Long meetingId, BoardRequestDto dto) {
        Board board = new Board();
        board.setMeetingId(meetingId);
        board.setExternalUserId(externalUserId);
        board.setTitle(dto.getTitle());
        board.setContent(dto.getContent());
        board.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")));

        // externalUserId로 UserRepository에서 바로 사용자 정보 조회
        Optional<User> user = userRepository.findByExternalUserId(externalUserId);
        User user1 = user.get();
        if (user != null) {
            board.setNickname(user1.getNickname());
            board.setProfileImageUrl(user1.getProfileImageUrl());
            board.setGender(user1.getGender());
        }
        return boardRepository.save(board);
    }

    // 기존 createBoard 메서드 외에 불러오기 기능 추가
    // meetingId에 해당하는 게시글들을 불러오는 메서드
    public List<BoardResponseDto> getBoardPosts(Long meetingId) {
        List<Board> boards = boardRepository.findByMeetingIdOrderByCreatedAtDesc(meetingId);
        return boards.stream().map(board -> new BoardResponseDto(
                board.getId(),
                board.getMeetingId(),
                board.getExternalUserId(),
                board.getTitle(),
                board.getContent(),
                board.getCreatedAt(),
                board.getNickname(),
                board.getProfileImageUrl(),
                board.getGender()
        )).collect(Collectors.toList());
    }


    /**
     * 전달된 postId와 meetingId에 해당하는 게시글을 삭제합니다.
     * meetingId 검증(모임 대표 여부 검증 등)은 컨트롤러나 별도 로직에서 추가할 수 있습니다.
     */
    public void deleteBoardPost(Long postId, Long meetingId) {
        // postId와 meetingId로 게시글 조회
        Board post = boardRepository.findByIdAndMeetingId(postId, meetingId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없거나 모임 ID가 일치하지 않습니다."));
        // DB에서 게시글 삭제
        boardRepository.delete(post);
    }
}
