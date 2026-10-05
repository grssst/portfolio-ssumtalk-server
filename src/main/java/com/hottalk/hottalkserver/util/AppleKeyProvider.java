package com.hottalk.hottalkserver.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

public class AppleKeyProvider {

    private static final String APPLE_PUBLIC_KEYS_URL = "https://appleid.apple.com/auth/keys";

    public static PublicKey getApplePublicKey() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(APPLE_PUBLIC_KEYS_URL).openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        InputStream inputStream = connection.getInputStream();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(inputStream);
        JsonNode keys = jsonNode.get("keys");

        // Assuming we're using the first key
        JsonNode key = keys.get(0);
        String n = key.get("n").asText();
        String e = key.get("e").asText();

        byte[] nBytes = Base64.getUrlDecoder().decode(n);
        byte[] eBytes = Base64.getUrlDecoder().decode(e);

        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
                new java.math.BigInteger(1, nBytes),
                new java.math.BigInteger(1, eBytes)
        );

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(publicKeySpec);
    }
}
