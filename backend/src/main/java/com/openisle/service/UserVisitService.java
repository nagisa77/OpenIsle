package com.openisle.service;

import com.openisle.model.User;
import com.openisle.model.UserVisit;
import com.openisle.repository.UserRepository;
import com.openisle.repository.UserVisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserVisitService {
    private final UserVisitRepository userVisitRepository;
    private final UserRepository userRepository;

    public boolean recordVisit(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        LocalDate today = LocalDate.now();
        return userVisitRepository.findByUserAndVisitDate(user, today).map(v -> false).orElseGet(() -> {
            UserVisit visit = new UserVisit();
            visit.setUser(user);
            visit.setVisitDate(today);
            userVisitRepository.save(visit);
            return true;
        });
    }

    public long countVisits(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        return userVisitRepository.countByUser(user);
    }

    public long countDau(LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        return userVisitRepository.countByVisitDate(d);
    }

    public Map<LocalDate, Long> countDauRange(LocalDate start, LocalDate end) {
        Map<LocalDate, Long> result = new LinkedHashMap<>();
        if (start == null || end == null || start.isAfter(end)) {
            return result;
        }
        var list = userVisitRepository.countRange(start, end);
        for (var obj : list) {
            LocalDate d = (LocalDate) obj[0];
            Long c = (Long) obj[1];
            result.put(d, c);
        }
        // fill zero counts for missing dates
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            result.putIfAbsent(d, 0L);
        }
        return result;
    }
}
