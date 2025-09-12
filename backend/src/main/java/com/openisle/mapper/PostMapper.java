package com.openisle.mapper;

import com.openisle.dto.CommentDto;
import com.openisle.dto.PostDetailDto;
import com.openisle.dto.PostSummaryDto;
import com.openisle.dto.ReactionDto;
import com.openisle.dto.LotteryDto;
import com.openisle.dto.PollDto;
import com.openisle.dto.AuthorDto;
import com.openisle.model.CommentSort;
import com.openisle.model.Post;
import com.openisle.model.LotteryPost;
import com.openisle.model.PollPost;
import com.openisle.model.User;
import com.openisle.model.PollVote;
import com.openisle.service.CommentService;
import com.openisle.service.ReactionService;
import com.openisle.service.SubscriptionService;
import com.openisle.repository.PollVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Mapper responsible for converting posts into DTOs. */
@Component
@RequiredArgsConstructor
public class PostMapper {

    private final CommentService commentService;
    private final ReactionService reactionService;
    private final SubscriptionService subscriptionService;
    private final CommentMapper commentMapper;
    private final ReactionMapper reactionMapper;
    private final UserMapper userMapper;
    private final TagMapper tagMapper;
    private final CategoryMapper categoryMapper;
    private final PollVoteRepository pollVoteRepository;

    public PostSummaryDto toSummaryDto(Post post) {
        PostSummaryDto dto = new PostSummaryDto();
        applyCommon(post, dto);
        return dto;
    }

    public PostDetailDto toDetailDto(Post post, String viewer) {
        PostDetailDto dto = new PostDetailDto();
        applyCommon(post, dto);
        List<CommentDto> comments = commentService.getCommentsForPost(post.getId(), CommentSort.OLDEST)
                .stream()
                .map(commentMapper::toDtoWithReplies)
                .collect(Collectors.toList());
        dto.setComments(comments);
        dto.setSubscribed(viewer != null && subscriptionService.isPostSubscribed(viewer, post.getId()));
        return dto;
    }

    private void applyCommon(Post post, PostSummaryDto dto) {
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setAuthor(userMapper.toAuthorDto(post.getAuthor()));
        dto.setCategory(categoryMapper.toDto(post.getCategory()));
        dto.setTags(post.getTags().stream().map(tagMapper::toDto).collect(Collectors.toList()));
        dto.setViews(post.getViews());
        dto.setCommentCount(post.getCommentCount());
        dto.setStatus(post.getStatus());
        dto.setPinnedAt(post.getPinnedAt());
        dto.setRssExcluded(post.getRssExcluded() == null || post.getRssExcluded());
        dto.setClosed(post.isClosed());

        List<ReactionDto> reactions = reactionService.getReactionsForPost(post.getId())
                .stream()
                .map(reactionMapper::toDto)
                .collect(Collectors.toList());
        dto.setReactions(reactions);

        List<User> participants = commentService.getParticipants(post.getId(), 5);
        dto.setParticipants(participants.stream().map(userMapper::toAuthorDto).collect(Collectors.toList()));

        LocalDateTime last = post.getLastReplyAt();
        dto.setLastReplyAt(last != null ? last : post.getCreatedAt());
        dto.setReward(0);
        dto.setSubscribed(false);
        dto.setType(post.getType());

        if (post instanceof LotteryPost lp) {
            LotteryDto l = new LotteryDto();
            l.setPrizeDescription(lp.getPrizeDescription());
            l.setPrizeIcon(lp.getPrizeIcon());
            l.setPrizeCount(lp.getPrizeCount());
            l.setPointCost(lp.getPointCost());
            l.setStartTime(lp.getStartTime());
            l.setEndTime(lp.getEndTime());
            l.setParticipants(lp.getParticipants().stream().map(userMapper::toAuthorDto).collect(Collectors.toList()));
            l.setWinners(lp.getWinners().stream().map(userMapper::toAuthorDto).collect(Collectors.toList()));
            dto.setLottery(l);
        }

        if (post instanceof PollPost pp) {
            PollDto p = new PollDto();
            p.setOptions(pp.getOptions());
            p.setVotes(pp.getVotes());
            p.setEndTime(pp.getEndTime());
            p.setParticipants(pp.getParticipants().stream().map(userMapper::toAuthorDto).collect(Collectors.toList()));
            Map<Integer, List<AuthorDto>> optionParticipants = pollVoteRepository.findByPostId(pp.getId()).stream()
                    .collect(Collectors.groupingBy(PollVote::getOptionIndex,
                            Collectors.mapping(v -> userMapper.toAuthorDto(v.getUser()), Collectors.toList())));
            p.setOptionParticipants(optionParticipants);
            p.setMultiple(Boolean.TRUE.equals(pp.getMultiple()));
            dto.setPoll(p);
        }
    }
}
