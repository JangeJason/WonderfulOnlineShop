package com.wonderful.onlineshop.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wonderful.onlineshop.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@Service
public class WechatAuthService {

    private static final String STABLE_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/stable_token";
    private static final String LEGACY_TOKEN_URL_TEMPLATE =
            "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";
    private static final String PHONE_NUMBER_URL_TEMPLATE =
            "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=%s";
    private static final String CODE2SESSION_URL_TEMPLATE =
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${wechat.miniapp.appid:}")
    private String appId;

    @Value("${wechat.miniapp.secret:}")
    private String appSecret;

    private volatile String accessToken;
    private volatile long accessTokenExpireAtEpochSecond;

    public String resolvePhoneByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("微信手机号登录参数缺失");
        }
        ensureConfigReady();
        String token = getAccessToken();
        String url = String.format(PHONE_NUMBER_URL_TEMPLATE, token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> payload = new HashMap<>();
        payload.put("code", code);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(payload, headers),
                    String.class);
            JsonNode body = objectMapper.readTree(response.getBody() == null ? "{}" : response.getBody());
            int errCode = body.path("errcode").asInt(-1);
            if (errCode != 0) {
                String errMsg = body.path("errmsg").asText("未知错误");
                throw new BusinessException("微信手机号获取失败: " + errMsg);
            }

            JsonNode phoneInfo = body.path("phone_info");
            String purePhone = phoneInfo.path("purePhoneNumber").asText("");
            if (purePhone == null || purePhone.isBlank()) {
                purePhone = phoneInfo.path("phoneNumber").asText("");
            }
            if (purePhone == null || purePhone.isBlank()) {
                throw new BusinessException("未获取到微信手机号");
            }
            return normalizePhone(purePhone);
        } catch (HttpStatusCodeException ex) {
            String msg = "微信手机号登录失败: getuserphonenumber HTTP "
                    + ex.getStatusCode().value()
                    + (ex.getResponseBodyAsString() == null || ex.getResponseBodyAsString().isBlank()
                            ? ""
                            : (", body=" + ex.getResponseBodyAsString()));
            throw new BusinessException(msg);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("微信手机号登录失败: " + ex.getMessage());
        }
    }

    public String resolvePhoneByEncryptedData(String jsCode, String encryptedData, String iv) {
        if (jsCode == null || jsCode.isBlank() || encryptedData == null || encryptedData.isBlank() || iv == null
                || iv.isBlank()) {
            throw new BusinessException("微信手机号解密参数缺失");
        }
        ensureConfigReady();
        String url = String.format(CODE2SESSION_URL_TEMPLATE, appId, appSecret, jsCode);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            JsonNode body = objectMapper.readTree(response.getBody() == null ? "{}" : response.getBody());
            int errCode = body.path("errcode").asInt(0);
            if (errCode != 0) {
                String errMsg = body.path("errmsg").asText("未知错误");
                throw new BusinessException("微信 code2session 失败: " + errMsg);
            }
            String sessionKey = body.path("session_key").asText("");
            if (sessionKey == null || sessionKey.isBlank()) {
                throw new BusinessException("微信 session_key 为空");
            }
            return decryptPhoneNumber(encryptedData, iv, sessionKey);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("微信手机号解密失败: " + ex.getMessage());
        }
    }

    private synchronized String getAccessToken() {
        long now = Instant.now().getEpochSecond();
        if (accessToken != null && now < accessTokenExpireAtEpochSecond - 60) {
            return accessToken;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> payload = new HashMap<>();
        payload.put("grant_type", "client_credential");
        payload.put("appid", appId);
        payload.put("secret", appSecret);
        payload.put("force_refresh", false);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(STABLE_TOKEN_URL, new HttpEntity<>(payload, headers),
                    String.class);
            JsonNode body = objectMapper.readTree(response.getBody() == null ? "{}" : response.getBody());
            int errCode = body.path("errcode").asInt(0);
            if (errCode != 0) {
                String errMsg = body.path("errmsg").asText("未知错误");
                throw new BusinessException("微信 access_token 获取失败: " + errMsg);
            }

            String newToken = body.path("access_token").asText("");
            if (newToken == null || newToken.isBlank()) {
                throw new BusinessException("微信 access_token 为空");
            }
            int expiresIn = body.path("expires_in").asInt(7200);
            accessToken = newToken;
            accessTokenExpireAtEpochSecond = Instant.now().getEpochSecond() + expiresIn;
            return accessToken;
        } catch (HttpStatusCodeException ex) {
            // Some environments return HTTP 412 for stable_token. Fallback to legacy endpoint.
            return getAccessTokenFromLegacyEndpoint(ex.getStatusCode().value(), ex.getResponseBodyAsString());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("微信 access_token 请求失败: " + ex.getMessage());
        }
    }

    private void ensureConfigReady() {
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new BusinessException("请先配置 wechat.miniapp.appid 和 wechat.miniapp.secret");
        }
    }

    private String normalizePhone(String phone) {
        String value = phone.trim();
        if (value.startsWith("+86")) {
            return value.substring(3);
        }
        return value;
    }

    private String getAccessTokenFromLegacyEndpoint(int stableStatusCode, String stableBody) {
        String url = String.format(LEGACY_TOKEN_URL_TEMPLATE, appId, appSecret);
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            JsonNode body = objectMapper.readTree(response.getBody() == null ? "{}" : response.getBody());
            int errCode = body.path("errcode").asInt(0);
            if (errCode != 0) {
                String errMsg = body.path("errmsg").asText("未知错误");
                throw new BusinessException("微信 access_token 获取失败(回退接口): " + errMsg);
            }
            String newToken = body.path("access_token").asText("");
            if (newToken == null || newToken.isBlank()) {
                throw new BusinessException("微信 access_token 为空(回退接口)");
            }
            int expiresIn = body.path("expires_in").asInt(7200);
            accessToken = newToken;
            accessTokenExpireAtEpochSecond = Instant.now().getEpochSecond() + expiresIn;
            return accessToken;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            String msg = "微信 access_token 请求失败: stable_token HTTP "
                    + stableStatusCode
                    + (stableBody == null || stableBody.isBlank() ? "" : (", body=" + stableBody))
                    + "; fallback token 接口也失败: "
                    + ex.getMessage();
            throw new BusinessException(msg);
        }
    }

    private String decryptPhoneNumber(String encryptedData, String iv, String sessionKey) throws Exception {
        byte[] dataBytes = Base64.getDecoder().decode(encryptedData);
        byte[] keyBytes = Base64.getDecoder().decode(sessionKey);
        byte[] ivBytes = Base64.getDecoder().decode(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] result = cipher.doFinal(dataBytes);
        String json = new String(result, StandardCharsets.UTF_8);
        JsonNode node = objectMapper.readTree(json);

        String purePhone = node.path("purePhoneNumber").asText("");
        if (purePhone == null || purePhone.isBlank()) {
            purePhone = node.path("phoneNumber").asText("");
        }
        if (purePhone == null || purePhone.isBlank()) {
            throw new BusinessException("解密结果中未包含手机号");
        }
        return normalizePhone(purePhone);
    }
}
