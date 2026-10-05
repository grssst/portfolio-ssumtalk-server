package com.hottalk.hottalkserver.controller;


import com.hottalk.hottalkserver.dto.CallCenterDTO;
import com.hottalk.hottalkserver.dto.ReportDto;
import com.hottalk.hottalkserver.model.Post;
import com.hottalk.hottalkserver.service.CallCenterService;
import com.hottalk.hottalkserver.service.ChatService;
import com.hottalk.hottalkserver.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/call-center")
public class CallCenterController {
    @Autowired
    private CallCenterService callCenterService;

    @PostMapping("/fetch-call-center")
    public ResponseEntity<?> fetchCallCenter(@RequestHeader("Authorization") String token, @RequestBody CallCenterDTO callCenterDTO) {
        try {
            callCenterService.saveCall(callCenterDTO, token);
            return ResponseEntity.ok("Call submitted successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Invalid token.");
        }
    }

}
