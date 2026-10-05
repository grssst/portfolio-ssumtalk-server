package com.hottalk.hottalkserver.service;


import com.hottalk.hottalkserver.dto.RealtimeChatDTO;
import com.hottalk.hottalkserver.repository.UserChatRoomsRepository;
import com.hottalk.hottalkserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class ChatService {
    private final DynamoDbClient dynamoDbClient; // 여기에 클래스 필드로 선언
    @Value("${dynamodb.tableName}")
    private String tableName;

    @Autowired
    public ChatService(@Value("${aws.accessKeyId}") String accessKey, @Value("${aws.secretAccessKey}") String secretKey,
                       @Value("${aws.region}") String region) {
        // DynamoDB Client 초기화
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }



    public void saveMessage(RealtimeChatDTO message) {
        String kstTimestamp = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        Map<String, AttributeValue> itemValues = new HashMap<>();
        itemValues.put("room_id", AttributeValue.builder().s(message.getRoomId()).build());
        itemValues.put("timestamp", AttributeValue.builder().s(kstTimestamp).build());
        itemValues.put("messageContent", AttributeValue.builder().s(message.getMessageContent()).build());
        itemValues.put("senderId", AttributeValue.builder().n(message.getSenderId()).build());
        itemValues.put("recipientId", AttributeValue.builder().n(message.getRecipientId()).build());
        //itemValues.put("myNickname", AttributeValue.builder().s(message.getMyNickname()).build());
        itemValues.put("isRead", AttributeValue.builder().bool(false).build());
        // 타입 기본값 "text" 지정: null이거나 공백인 경우 "text" 할당
        String type = (message.getType() == null || message.getType().trim().isEmpty())
                ? "text"
                : message.getType();
        itemValues.put("type", AttributeValue.builder().s(type).build());


        // 만약 room_id가 "meeting_"으로 시작하면 effectiveRoomId도 저장 (예: "meeting_24")
        if (message.getRoomId().startsWith("meeting_")) {
            String[] parts = message.getRoomId().split("_");
            if (parts.length >= 2) {
                String effectiveRoomId = parts[0] + "_" + parts[1];
                itemValues.put("effective_room_id", AttributeValue.builder().s(effectiveRoomId).build());
            }
        }

        // DynamoDB에 아이템 저장
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(itemValues)
                .build());
    }


}
