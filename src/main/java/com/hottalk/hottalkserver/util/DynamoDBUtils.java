package com.hottalk.hottalkserver.util;

import com.hottalk.hottalkserver.model.ReportChatting;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Map;
import java.util.stream.Collectors;

public class DynamoDBUtils {
    // DateTimeFormatter 상수 정의
    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true) // 최소 1자리, 최대 9자리까지 허용
            .appendPattern("XXX")
            .toFormatter();

    // DynamoDB AttributeValue를 ReportChatting 엔티티로 변환

    public static ReportChatting mapToEntity(Map<String, AttributeValue> dynamoDbItem) {
        ReportChatting entity = new ReportChatting();
        System.out.println(dynamoDbItem);
        entity.setRoomId(dynamoDbItem.get("room_id").s());
        entity.setSenderId(dynamoDbItem.get("senderId").n());
        entity.setRecipientId(dynamoDbItem.get("recipientId").n());
        entity.setRead(dynamoDbItem.get("isRead").bool());
        entity.setMessageContent(dynamoDbItem.get("messageContent").s());
        // OffsetDateTime으로 파싱 후 LocalDateTime으로 변환
        String timestamp = dynamoDbItem.get("timestamp").s();
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(timestamp, FORMATTER);
        entity.setTimestamp(offsetDateTime.toLocalDateTime());

        return entity;
    }
}
