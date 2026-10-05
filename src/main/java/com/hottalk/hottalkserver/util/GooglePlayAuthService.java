package com.hottalk.hottalkserver.util;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

@Service
public class GooglePlayAuthService {

    private static final String SCOPE_ANDROID_PUBLISHER = "https://www.googleapis.com/auth/androidpublisher";
    private static final String SERVICE_ACCOUNT_KEY_PATH = "************";
    // 리소스 경로에 배치했다면 위와 같이 리소스 기준 경로로 지정

    public String getAccessToken() {
        try (InputStream serviceAccountStream = getClass().getClassLoader().getResourceAsStream(SERVICE_ACCOUNT_KEY_PATH)) {
            if (serviceAccountStream == null) {
                throw new RuntimeException("Service Account Key file not found: " + SERVICE_ACCOUNT_KEY_PATH);
            }
            GoogleCredentials credentials = ServiceAccountCredentials.fromStream(serviceAccountStream)
                    .createScoped(Collections.singletonList(SCOPE_ANDROID_PUBLISHER));

            credentials.refreshIfExpired();
            System.out.println(credentials.getAccessToken().getTokenValue());
            return credentials.getAccessToken().getTokenValue();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load service account credentials.", e);
        }
    }
}