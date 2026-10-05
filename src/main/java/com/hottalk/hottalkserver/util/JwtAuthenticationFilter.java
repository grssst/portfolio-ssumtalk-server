package com.hottalk.hottalkserver.util;

import com.hottalk.hottalkserver.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.replace("Bearer ", "");
            try {
                // 토큰 검증 및 클레임 추출
                Claims claims = JwtUtil.validateTokenAndGetClaims(token);
                // 관리 페이지 관련 URL에 대해서만 role 검사 수행
                String requestUri = request.getRequestURI();
                if (requestUri.startsWith("/api/admin")) {
                    String role = claims.get("role", String.class);
                    if (role == null || !role.equals("admin")) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.getWriter().write("Forbidden: Admin role required");
                        return;
                    }
                }

                // 토큰 검증
                String userId = claims.getSubject();

                // 요청 속성에 사용자 정보 추가
                request.setAttribute("userId", userId);

            } catch (Exception ex) {
                // 토큰이 만료되었거나 잘못된 경우 401 반환
                System.out.println("여기서 401 발생");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Unauthorized: " + ex.getMessage());
                return;
            }
        }

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }
}