package com.wonderful.onlineshop.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wonderful.onlineshop.common.BusinessException;
import com.wonderful.onlineshop.user.entity.UserCertificate;
import com.wonderful.onlineshop.user.mapper.UserCertificateMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class UserCertificateService {

    private final UserCertificateMapper userCertificateMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserCertificateService(UserCertificateMapper userCertificateMapper) {
        this.userCertificateMapper = userCertificateMapper;
    }

    public List<CertificateDTO> listByUser(Long userId) {
        List<UserCertificate> list = userCertificateMapper.selectList(
                new LambdaQueryWrapper<UserCertificate>()
                        .eq(UserCertificate::getUserId, userId)
                        .orderByDesc(UserCertificate::getUpdatedAt)
                        .orderByDesc(UserCertificate::getCreatedAt));
        return list.stream().map(this::toDTO).toList();
    }

    public CertificateDTO create(Long userId, CreateCertificateRequest req) {
        if (req.fileUrls() == null || req.fileUrls().isEmpty()) {
            throw new BusinessException("请至少上传1个证书文件");
        }
        if (req.fileUrls().size() > 5) {
            throw new BusinessException("证书文件最多上传5个");
        }
        LocalDate endDate;
        try {
            endDate = LocalDate.parse(req.endDate());
        } catch (Exception ex) {
            throw new BusinessException("结束日期格式无效");
        }

        UserCertificate cert = new UserCertificate();
        cert.setUserId(userId);
        cert.setCertificateType(req.certificateType().trim());
        cert.setTrademarkType(req.trademarkType() == null || req.trademarkType().isBlank() ? "未知" : req.trademarkType().trim());
        cert.setTrademarkContent(req.trademarkContent().trim());
        cert.setPrincipal(req.principal().trim());
        cert.setEndDate(endDate);
        cert.setFileUrls(writeFileUrls(req.fileUrls(), req.fileNames()));
        cert.setCreatedAt(LocalDateTime.now());
        cert.setUpdatedAt(LocalDateTime.now());
        userCertificateMapper.insert(cert);
        return toDTO(cert);
    }

    public CertificateDTO update(Long userId, Long certificateId, CreateCertificateRequest req) {
        UserCertificate cert = userCertificateMapper.selectById(certificateId);
        if (cert == null || !userId.equals(cert.getUserId())) {
            throw new BusinessException("证书不存在");
        }
        if (req.fileUrls() == null || req.fileUrls().isEmpty()) {
            throw new BusinessException("请至少上传1个证书文件");
        }
        if (req.fileUrls().size() > 5) {
            throw new BusinessException("证书文件最多上传5个");
        }
        LocalDate endDate;
        try {
            endDate = LocalDate.parse(req.endDate());
        } catch (Exception ex) {
            throw new BusinessException("结束日期格式无效");
        }

        cert.setCertificateType(req.certificateType().trim());
        cert.setTrademarkType(req.trademarkType() == null || req.trademarkType().isBlank() ? "未知" : req.trademarkType().trim());
        cert.setTrademarkContent(req.trademarkContent().trim());
        cert.setPrincipal(req.principal().trim());
        cert.setEndDate(endDate);
        cert.setFileUrls(writeFileUrls(req.fileUrls(), req.fileNames()));
        cert.setUpdatedAt(LocalDateTime.now());
        userCertificateMapper.updateById(cert);
        return toDTO(cert);
    }

    private CertificateDTO toDTO(UserCertificate cert) {
        List<StoredFile> files = readStoredFiles(cert.getFileUrls());
        return new CertificateDTO(
                cert.getId(),
                cert.getCertificateType(),
                cert.getTrademarkType(),
                cert.getTrademarkContent(),
                cert.getPrincipal(),
                cert.getEndDate() == null ? null : cert.getEndDate().toString(),
                files.stream().map(StoredFile::url).toList(),
                files.stream().map(StoredFile::name).toList());
    }

    private List<StoredFile> readStoredFiles(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();
        try {
            List<?> list = objectMapper.readValue(raw, new TypeReference<List<?>>() {
            });
            List<StoredFile> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof String s) {
                    result.add(new StoredFile(s, extractNameFromUrl(s)));
                    continue;
                }
                if (item instanceof Map<?, ?> map) {
                    Object urlObj = map.get("url");
                    if (!(urlObj instanceof String url) || url.isBlank()) {
                        continue;
                    }
                    Object nameObj = map.get("name");
                    String name = (nameObj instanceof String n && !n.isBlank()) ? n : extractNameFromUrl(url);
                    result.add(new StoredFile(url, name));
                }
            }
            return result;
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private String writeFileUrls(List<String> urls, List<String> names) {
        try {
            List<StoredFile> files = new ArrayList<>();
            for (int i = 0; i < urls.size(); i++) {
                String url = urls.get(i);
                if (url == null || url.isBlank()) continue;
                String name = extractNameFromUrl(url);
                if (names != null && names.size() > i) {
                    String candidate = names.get(i);
                    if (candidate != null && !candidate.isBlank()) {
                        name = candidate;
                    }
                }
                files.add(new StoredFile(url, name));
            }
            return objectMapper.writeValueAsString(files);
        } catch (Exception ex) {
            throw new BusinessException("证书文件保存失败");
        }
    }

    private String extractNameFromUrl(String url) {
        if (url == null || url.isBlank()) return "";
        int idx = url.lastIndexOf('/');
        if (idx >= 0 && idx < url.length() - 1) {
            return url.substring(idx + 1);
        }
        return url;
    }

    private record StoredFile(String url, String name) {
    }

    public record CreateCertificateRequest(
            String certificateType,
            String trademarkType,
            String trademarkContent,
            String principal,
            String endDate,
            List<String> fileUrls,
            List<String> fileNames) {
    }

    public record CertificateDTO(
            Long id,
            String certificateType,
            String trademarkType,
            String trademarkContent,
            String principal,
            String endDate,
            List<String> fileUrls,
            List<String> fileNames) {
    }
}
