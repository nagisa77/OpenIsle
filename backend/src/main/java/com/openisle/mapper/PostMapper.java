package com.openisle.mapper;

import com.openisle.dto.*;
import com.openisle.model.CategoryProposalPost;
import com.openisle.model.CommentSort;
import com.openisle.model.LotteryPost;
import com.openisle.model.PollPost;
import com.openisle.model.PollVote;
import com.openisle.model.Post;
import com.openisle.model.User;
import com.openisle.repository.PollVoteRepository;
import com.openisle.service.CommentService;
import com.openisle.service.ReactionService;
import com.openisle.service.SubscriptionService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
    List<CommentDto> comments = commentService
      .getCommentsForPost(post.getId(), CommentSort.OLDEST)
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
    dto.setStatus(post.getStatus());
    dto.setPinnedAt(post.getPinnedAt());
    dto.setRssExcluded(post.getRssExcluded() == null || post.getRssExcluded());
    dto.setClosed(post.isClosed());
    dto.setVisibleScope(post.getVisibleScope());

    List<ReactionDto> reactions = reactionService
      .getReactionsForPost(post.getId())
      .stream()
      .map(reactionMapper::toDto)
      .collect(Collectors.toList());
    dto.setReactions(reactions);

    List<User> participants = commentService.getParticipants(post.getId(), 5);
    dto.setParticipants(
      participants.stream().map(userMapper::toAuthorDto).collect(Collectors.toList())
    );

    LocalDateTime last = post.getLastReplyAt();
    if (last == null) {
      commentService.updatePostCommentStats(post);
    }
    dto.setCommentCount(post.getCommentCount());
    dto.setLastReplyAt(post.getLastReplyAt());
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
      l.setParticipants(
        lp.getParticipants().stream().map(userMapper::toAuthorDto).collect(Collectors.toList())
      );
      l.setWinners(
        lp.getWinners().stream().map(userMapper::toAuthorDto).collect(Collectors.toList())
      );
      dto.setLottery(l);
    }

    if (post instanceof CategoryProposalPost cp) {
      ProposalDto proposalDto = (ProposalDto) buildPollDto(cp, new ProposalDto());
      proposalDto.setProposalStatus(cp.getProposalStatus());
      proposalDto.setProposedName(cp.getProposedName());
      proposalDto.setDescription(cp.getDescription());
      proposalDto.setApproveThreshold(cp.getApproveThreshold());
      proposalDto.setQuorum(cp.getQuorum());
      proposalDto.setStartAt(cp.getStartAt());
      proposalDto.setResultSnapshot(cp.getResultSnapshot());
      proposalDto.setRejectReason(cp.getRejectReason());
      dto.setPoll(proposalDto);
    } else if (post instanceof PollPost pp) {
      dto.setPoll(buildPollDto(pp, new PollDto()));
    }
  }

  private PollDto buildPollDto(PollPost pollPost, PollDto target) {
    target.setOptions(pollPost.getOptions());
    target.setVotes(pollPost.getVotes());
    target.setEndTime(pollPost.getEndTime());
    target.setParticipants(
      pollPost.getParticipants().stream().map(userMapper::toAuthorDto).collect(Collectors.toList())
    );
    Map<Integer, List<AuthorDto>> optionParticipants = pollVoteRepository
      .findByPostId(pollPost.getId())
      .stream()
      .collect(
        Collectors.groupingBy(
          PollVote::getOptionIndex,
          Collectors.mapping(v -> userMapper.toAuthorDto(v.getUser()), Collectors.toList())
        )
      );
    target.setOptionParticipants(optionParticipants);
    target.setMultiple(Boolean.TRUE.equals(pollPost.getMultiple()));
    return target;
  }

  // TODO
  // 当前只作用于最新回复接口
  // ============以下是性能优化的临时对应，本地无法测试性能问题，如果解决了首页分页的性能且无逻辑问题，即可在其他分类list展开===============

  public PostSummaryDto toSimpleDto(Post post){
      PostSummaryDto dto = new PostSummaryDto();

      dto.setId(post.getId());
      dto.setTitle(post.getTitle());
      // 这个字段是否内容太多
      dto.setContent(post.getContent());

      dto.setCreatedAt(post.getCreatedAt());
      // 首页不需要具体的作者信息
      //dto.setAuthor(userMapper.toAuthorDto(post.getAuthor()));
      dto.setCategory(categoryMapper.toDto(post.getCategory()));
      dto.setTags(post.getTags().stream().map(tagMapper::toDto).collect(Collectors.toList()));
      dto.setViews(post.getViews());
      dto.setStatus(post.getStatus());
      dto.setPinnedAt(post.getPinnedAt());
      dto.setRssExcluded(post.getRssExcluded() == null || post.getRssExcluded());
      dto.setClosed(post.isClosed());
      dto.setVisibleScope(post.getVisibleScope());
      //首页不需要具体文章的reaction
//      List<ReactionDto> reactions = reactionService
//              .getReactionsForPost(post.getId())
//              .stream()
//              .map(reactionMapper::toDto)
//              .collect(Collectors.toList());
//      dto.setReactions(reactions);

      List<User> participants = commentService.getParticipants(post.getId(), 5);
      dto.setParticipants(
              participants.stream().map(userMapper::toAuthorDto).collect(Collectors.toList())
      );

      LocalDateTime last = post.getLastReplyAt();
      if (last == null) {
          commentService.updatePostCommentStats(post);
      }
      dto.setCommentCount(post.getCommentCount());
      dto.setLastReplyAt(post.getLastReplyAt());
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
          l.setParticipants(
                  lp.getParticipants().stream().map(userMapper::toAuthorDto).collect(Collectors.toList())
          );
          // 不需要赢的信息
//          l.setWinners(
//                  lp.getWinners().stream().map(userMapper::toAuthorDto).collect(Collectors.toList())
//          );
          dto.setLottery(l);
      }

      if (post instanceof CategoryProposalPost cp) {
          ProposalDto proposalDto = (ProposalDto) buildPollDto2(cp, new ProposalDto());
          proposalDto.setProposalStatus(cp.getProposalStatus());
          proposalDto.setProposedName(cp.getProposedName());
          proposalDto.setDescription(cp.getDescription());
          proposalDto.setApproveThreshold(cp.getApproveThreshold());
          proposalDto.setQuorum(cp.getQuorum());
          proposalDto.setStartAt(cp.getStartAt());
          proposalDto.setResultSnapshot(cp.getResultSnapshot());
          proposalDto.setRejectReason(cp.getRejectReason());
          dto.setPoll(proposalDto);
      } else if (post instanceof PollPost pp) {
          dto.setPoll(buildPollDto2(pp, new PollDto()));
      }
      return dto;
  }


    private PollDto buildPollDto2(PollPost pollPost, PollDto target) {
        target.setOptions(pollPost.getOptions());
        target.setVotes(pollPost.getVotes());
        target.setEndTime(pollPost.getEndTime());
        target.setParticipants(
                pollPost.getParticipants().stream().map(userMapper::toAuthorDto).collect(Collectors.toList())
        );
        // 不需要具体的投票信息，首页
//        Map<Integer, List<AuthorDto>> optionParticipants = pollVoteRepository
//                .findByPostId(pollPost.getId())
//                .stream()
//                .collect(
//                        Collectors.groupingBy(
//                                PollVote::getOptionIndex,
//                                Collectors.mapping(v -> userMapper.toAuthorDto(v.getUser()), Collectors.toList())
//                        )
//                );
//        target.setOptionParticipants(optionParticipants);
        target.setMultiple(Boolean.TRUE.equals(pollPost.getMultiple()));
        return target;
    }
}
