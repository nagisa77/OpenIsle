package com.openisle.service;

import com.openisle.model.ContributorConfig;
import com.openisle.repository.ContributorConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContributorService {
    private static final String OWNER = "nagisa77";
    private static final String REPO = "OpenIsle";

    private final ContributorConfigRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    @Scheduled(cron = "0 0 * * * *")
    public void updateContributions() {
        for (ContributorConfig config : repository.findAll()) {
            long lines = fetchContributionLines(config.getGithubId());
            if (lines != -1) {
                config.setContributionLines(lines);
                repository.save(config);
            }
        }
    }

    private long fetchContributionLines(String githubId) {
        try {
            String url = String.format("https://api.github.com/repos/%s/%s/stats/contributors", OWNER, REPO);
            ResponseEntity<?> response = restTemplate.getForEntity(url, Object.class);

            // 检查是否为202，GitHub有时会返回202表示正在生成统计数据
            if (response.getStatusCodeValue() == 202) {
                log.warn("GitHub API 返回202，统计数据正在生成中，githubId: {}", githubId);
                return -1;
            }

            Object body = response.getBody();
            if (!(body instanceof List)) {
                // 不是List类型，直接返回0
                return 0;
            }
            List<?> listBody = (List<?>) body;
            for (Object itemObj : listBody) {
                if (!(itemObj instanceof Map)) continue;
                Map<String, Object> item = (Map<String, Object>) itemObj;
                Map<String, Object> author = (Map<String, Object>) item.get("author");
                if (author != null && githubId.equals(author.get("login"))) {
                    List<Map<String, Object>> weeks = (List<Map<String, Object>>) item.get("weeks");
                    long total = 0;
                    if (weeks != null) {
                        for (Map<String, Object> week : weeks) {
                            Number a = (Number) week.get("a");
                            Number d = (Number) week.get("d");
                            if (a != null) {
                                total += a.longValue();
                            }
                            if (d != null) {
                                total += d.longValue();
                            }
                        }
                    }
                    return total;
                }
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        return 0;
    }

    public long getContributionLines(String userIname) {
        return repository.findByUserIname(userIname)
                .map(ContributorConfig::getContributionLines)
                .orElse(0L);
    }
}

