package com.openisle.service;

import com.openisle.dto.CommentMedalDto;
import com.openisle.dto.ContributorMedalDto;
import com.openisle.dto.MedalDto;
import com.openisle.dto.PostMedalDto;
import com.openisle.dto.SeedUserMedalDto;
import com.openisle.dto.PioneerMedalDto;
import com.openisle.model.MedalType;
import com.openisle.model.User;
import com.openisle.repository.CommentRepository;
import com.openisle.repository.PostRepository;
import com.openisle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedalService {
    private static final long COMMENT_TARGET = 100;
    private static final long POST_TARGET = 100;
    private static final LocalDateTime SEED_USER_DEADLINE = LocalDateTime.of(2025, 9, 16, 0, 0);
    private static final long CONTRIBUTION_TARGET = 1;
    private static final long PIONEER_LIMIT = 1000;

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ContributorService contributorService;

    public List<MedalDto> getMedals(Long userId) {
        List<MedalDto> medals = new ArrayList<>();
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }
        MedalType selected = user != null ? user.getDisplayMedal() : null;

        CommentMedalDto commentMedal = new CommentMedalDto();
        commentMedal.setIcon("https://openisle-1307107697.cos.ap-guangzhou.myqcloud.com/assert/icons/achi_comment.png");
        commentMedal.setTitle("评论达人");
        commentMedal.setDescription("评论超过100条");
        commentMedal.setType(MedalType.COMMENT);
        commentMedal.setTargetCommentCount(COMMENT_TARGET);
        if (user != null) {
            long count = commentRepository.countByAuthor_Id(userId);
            commentMedal.setCurrentCommentCount(count);
            commentMedal.setCompleted(count >= COMMENT_TARGET);
        } else {
            commentMedal.setCurrentCommentCount(0);
            commentMedal.setCompleted(false);
        }
        commentMedal.setSelected(selected == MedalType.COMMENT);
        medals.add(commentMedal);

        PostMedalDto postMedal = new PostMedalDto();
        postMedal.setIcon("https://openisle-1307107697.cos.ap-guangzhou.myqcloud.com/assert/icons/achi_post.png");
        postMedal.setTitle("发帖达人");
        postMedal.setDescription("发帖超过100条");
        postMedal.setType(MedalType.POST);
        postMedal.setTargetPostCount(POST_TARGET);
        if (user != null) {
            long count = postRepository.countByAuthor_Id(userId);
            postMedal.setCurrentPostCount(count);
            postMedal.setCompleted(count >= POST_TARGET);
        } else {
            postMedal.setCurrentPostCount(0);
            postMedal.setCompleted(false);
        }
        postMedal.setSelected(selected == MedalType.POST);
        medals.add(postMedal);

        ContributorMedalDto contributorMedal = new ContributorMedalDto();
        contributorMedal.setIcon("https://openisle-1307107697.cos.ap-guangzhou.myqcloud.com/assert/icons/achi_coder.png");
        contributorMedal.setTitle("贡献者");
        contributorMedal.setDescription("对仓库贡献超过1行代码");
        contributorMedal.setType(MedalType.CONTRIBUTOR);
        contributorMedal.setTargetContributionLines(CONTRIBUTION_TARGET);
        if (user != null) {
            long lines = contributorService.getContributionLines(user.getUsername());
            contributorMedal.setCurrentContributionLines(lines);
            contributorMedal.setCompleted(lines >= CONTRIBUTION_TARGET);
        } else {
            contributorMedal.setCurrentContributionLines(0);
            contributorMedal.setCompleted(false);
        }
        contributorMedal.setSelected(selected == MedalType.CONTRIBUTOR);
        medals.add(contributorMedal);

        SeedUserMedalDto seedUserMedal = new SeedUserMedalDto();
        seedUserMedal.setIcon("https://openisle-1307107697.cos.ap-guangzhou.myqcloud.com/assert/icons/achi_seed.png");
        seedUserMedal.setTitle("种子用户");
        seedUserMedal.setDescription("2025.9.16前注册的用户");
        seedUserMedal.setType(MedalType.SEED);
        if (user != null) {
            seedUserMedal.setRegisterDate(user.getCreatedAt());
            seedUserMedal.setCompleted(user.getCreatedAt().isBefore(SEED_USER_DEADLINE));
        } else {
            seedUserMedal.setCompleted(false);
        }
        seedUserMedal.setSelected(selected == MedalType.SEED);
        medals.add(seedUserMedal);

        PioneerMedalDto pioneerMedal = new PioneerMedalDto();
        pioneerMedal.setIcon("https://openisle-1307107697.cos.ap-guangzhou.myqcloud.com/assert/icons/achi_pioneer.png");
        pioneerMedal.setTitle("开山鼻祖");
        pioneerMedal.setDescription("前1000位加入的用户");
        pioneerMedal.setType(MedalType.PIONEER);
        if (user != null) {
            long rank = userRepository.countByCreatedAtBefore(user.getCreatedAt()) + 1;
            pioneerMedal.setRank(rank);
            pioneerMedal.setCompleted(rank <= PIONEER_LIMIT);
        } else {
            pioneerMedal.setCompleted(false);
        }
        pioneerMedal.setSelected(selected == MedalType.PIONEER);
        medals.add(pioneerMedal);
        if (user != null && selected == null) {
            for (MedalDto medal : medals) {
                if (medal.isCompleted()) {
                    medal.setSelected(true);
                    user.setDisplayMedal(medal.getType());
                    userRepository.save(user);
                    break;
                }
            }
        }

        return medals;
    }

    public void ensureDisplayMedal(User user) {
        if (user == null || user.getDisplayMedal() != null) {
            return;
        }
        if (commentRepository.countByAuthor_Id(user.getId()) >= COMMENT_TARGET) {
            user.setDisplayMedal(MedalType.COMMENT);
        } else if (postRepository.countByAuthor_Id(user.getId()) >= POST_TARGET) {
            user.setDisplayMedal(MedalType.POST);
        } else if (contributorService.getContributionLines(user.getUsername()) >= CONTRIBUTION_TARGET) {
            user.setDisplayMedal(MedalType.CONTRIBUTOR);
        } else if (userRepository.countByCreatedAtBefore(user.getCreatedAt()) < PIONEER_LIMIT) {
            user.setDisplayMedal(MedalType.PIONEER);
        } else if (user.getCreatedAt().isBefore(SEED_USER_DEADLINE)) {
            user.setDisplayMedal(MedalType.SEED);
        }
        if (user.getDisplayMedal() != null) {
            userRepository.save(user);
        }
    }

    public void selectMedal(String username, MedalType type) {
        User user = userRepository.findByUsername(username).orElseThrow();
        boolean completed = getMedals(user.getId()).stream()
                .filter(m -> m.getType() == type)
                .findFirst()
                .map(MedalDto::isCompleted)
                .orElse(false);
        if (!completed) {
            throw new IllegalArgumentException("Medal not completed");
        }
        user.setDisplayMedal(type);
        userRepository.save(user);
    }
}
