package com.hottalk.hottalkserver.service;


import com.hottalk.hottalkserver.dto.CallCenterDTO;
import com.hottalk.hottalkserver.dto.RealtimeChatDTO;
import com.hottalk.hottalkserver.dto.ReportDto;
import com.hottalk.hottalkserver.model.CallCenter;
import com.hottalk.hottalkserver.model.Report;
import com.hottalk.hottalkserver.repository.CallCenterRepository;
import com.hottalk.hottalkserver.repository.UserRepository;
import com.hottalk.hottalkserver.util.JwtUtil;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class CallCenterService {
    @Autowired
    private CallCenterRepository callCenterRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private FcmService fcmService;

    public void saveCall(CallCenterDTO callCenterDTO, String token) {
        String externalUserId = JwtUtil.validateToken(token.replace("Bearer ", ""));
        Long myId = userService.getUserProfile(externalUserId).getId();



        CallCenter callCenter = new CallCenter();
        callCenter.setCallerUserId(myId);
        callCenter.setCallCenterContent(callCenterDTO.getCallCenterContent());
        callCenter.setCalledAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        callCenter.setCategory(callCenterDTO.getCategory());
        callCenter.setExternalUserId(externalUserId);


        callCenterRepository.save(callCenter);

        if(callCenterDTO.getCategory().equals("이용 장애 문의")){
            RealtimeChatDTO dto = new RealtimeChatDTO();
            dto.setRoomId("asd");
            dto.setMessageContent("이용 장애 문의 들어옴 id:" + myId);
            dto.setRecipientId("admin");
            dto.setSenderId("server");
            dto.setMyNickname("이용 장애 문의");
            //SQS에 푸시 알림 요청 전송
            fcmService.sendToSqsAdmin(dto);
        } else if(callCenterDTO.getCategory().equals("결제 문의")) {
            RealtimeChatDTO dto = new RealtimeChatDTO();
            dto.setRoomId("asd");
            dto.setMessageContent("결제 장애 문의 들어옴 id:" + myId);
            dto.setRecipientId("admin");
            dto.setSenderId("server");
            dto.setMyNickname("결제 장애 문의");
            fcmService.sendToSqsAdmin(dto);
        }



    }

}
