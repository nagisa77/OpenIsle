package com.openisle.service;

import com.openisle.model.Post;
import com.openisle.model.PostStatus;
import com.openisle.model.PublishMode;
import com.openisle.model.User;
import com.openisle.model.Category;
import com.openisle.model.Comment;
import com.openisle.model.NotificationType;
import com.openisle.repository.PostRepository;
import com.openisle.repository.UserRepository;
import com.openisle.repository.CategoryRepository;
import com.openisle.repository.TagRepository;
import com.openisle.service.SubscriptionService;
import com.openisle.service.CommentService;
import com.openisle.repository.CommentRepository;
import com.openisle.repository.ReactionRepository;
import com.openisle.repository.PostSubscriptionRepository;
import com.openisle.repository.NotificationRepository;
import com.openisle.model.Role;
import com.openisle.exception.RateLimitException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private PublishMode publishMode;
    private final NotificationService notificationService;
    private final SubscriptionService subscriptionService;
    private final CommentService commentService;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final PostSubscriptionRepository postSubscriptionRepository;
    private final NotificationRepository notificationRepository;
    private final PostReadService postReadService;
    private final ImageUploader imageUploader;

    @org.springframework.beans.factory.annotation.Autowired
    public PostService(PostRepository postRepository,
                       UserRepository userRepository,
                       CategoryRepository categoryRepository,
                       TagRepository tagRepository,
                       NotificationService notificationService,
                       SubscriptionService subscriptionService,
                       CommentService commentService,
                       CommentRepository commentRepository,
                       ReactionRepository reactionRepository,
                       PostSubscriptionRepository postSubscriptionRepository,
                       NotificationRepository notificationRepository,
                       PostReadService postReadService,
                       ImageUploader imageUploader,
                       @Value("${app.post.publish-mode:DIRECT}") PublishMode publishMode) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.notificationService = notificationService;
        this.subscriptionService = subscriptionService;
        this.commentService = commentService;
        this.commentRepository = commentRepository;
        this.reactionRepository = reactionRepository;
        this.postSubscriptionRepository = postSubscriptionRepository;
        this.notificationRepository = notificationRepository;
        this.postReadService = postReadService;
        this.imageUploader = imageUploader;
        this.publishMode = publishMode;
    }

    public PublishMode getPublishMode() {
        return publishMode;
    }

    public void setPublishMode(PublishMode publishMode) {
        this.publishMode = publishMode;
    }

    public Post createPost(String username,
                           Long categoryId,
                           String title,
                           String content,
                           java.util.List<Long> tagIds) {
        long recent = postRepository.countByAuthorAfter(username,
                java.time.LocalDateTime.now().minusMinutes(5));
        if (recent >= 1) {
            throw new RateLimitException("Too many posts");
        }
        if (tagIds == null || tagIds.isEmpty()) {
            throw new IllegalArgumentException("At least one tag required");
        }
        if (tagIds.size() > 2) {
            throw new IllegalArgumentException("At most two tags allowed");
        }
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        java.util.List<com.openisle.model.Tag> tags = tagRepository.findAllById(tagIds);
        if (tags.isEmpty()) {
            throw new IllegalArgumentException("Tag not found");
        }
        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setAuthor(author);
        post.setCategory(category);
        post.setTags(new java.util.HashSet<>(tags));
        post.setStatus(publishMode == PublishMode.REVIEW ? PostStatus.PENDING : PostStatus.PUBLISHED);
        post = postRepository.save(post);
        imageUploader.addReferences(imageUploader.extractUrls(content));
        if (post.getStatus() == PostStatus.PENDING) {
            java.util.List<User> admins = userRepository.findByRole(com.openisle.model.Role.ADMIN);
            for (User admin : admins) {
                notificationService.createNotification(admin,
                        NotificationType.POST_REVIEW_REQUEST, post, null, null, author, null, null);
            }
            notificationService.createNotification(author,
                    NotificationType.POST_REVIEW_REQUEST, post, null, null, null, null, null);
        }
        // notify followers of author
        for (User u : subscriptionService.getSubscribers(author.getUsername())) {
            if (!u.getId().equals(author.getId())) {
                notificationService.createNotification(
                        u,
                        NotificationType.FOLLOWED_POST,
                        post,
                        null,
                        null,
                        author,
                        null,
                        null);
            }
        }
        notificationService.notifyMentions(content, author, post, null);
        return post;
    }

    @Transactional
    public Post viewPost(Long id, String viewer) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
        if (post.getStatus() != PostStatus.PUBLISHED) {
            if (viewer == null) {
                throw new com.openisle.exception.NotFoundException("Post not found");
            }
            User viewerUser = userRepository.findByUsername(viewer)
                    .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
            if (!viewerUser.getRole().equals(com.openisle.model.Role.ADMIN) && !viewerUser.getId().equals(post.getAuthor().getId())) {
                throw new com.openisle.exception.NotFoundException("Post not found");
            }
        }
        post.setViews(post.getViews() + 1);
        post = postRepository.save(post);
        if (viewer != null) {
            postReadService.recordRead(viewer, id);
        }
        if (viewer != null && !viewer.equals(post.getAuthor().getUsername())) {
            User viewerUser = userRepository.findByUsername(viewer).orElse(null);
            if (viewerUser != null) {
                notificationRepository.deleteByTypeAndFromUserAndPost(NotificationType.POST_VIEWED, viewerUser, post);
                notificationService.createNotification(post.getAuthor(), NotificationType.POST_VIEWED, post, null, null, viewerUser, null, null);
            }
        }
        return post;
    }

    public List<Post> listPosts() {
        return listPostsByCategories(null, null, null);
    }

    public List<Post> listPostsByViews(Integer page, Integer pageSize) {
        return listPostsByViews(null, null, page, pageSize);
    }

    public List<Post> listPostsByViews(java.util.List<Long> categoryIds,
                                       java.util.List<Long> tagIds,
                                       Integer page,
                                       Integer pageSize) {
        boolean hasCategories = categoryIds != null && !categoryIds.isEmpty();
        boolean hasTags = tagIds != null && !tagIds.isEmpty();

        java.util.List<Post> posts;

        if (!hasCategories && !hasTags) {
            posts = postRepository.findByStatusOrderByViewsDesc(PostStatus.PUBLISHED);
        } else if (hasCategories) {
            java.util.List<Category> categories = categoryRepository.findAllById(categoryIds);
            if (categories.isEmpty()) {
                return java.util.List.of();
            }
            if (hasTags) {
                java.util.List<com.openisle.model.Tag> tags = tagRepository.findAllById(tagIds);
                if (tags.isEmpty()) {
                    return java.util.List.of();
                }
                posts = postRepository.findByCategoriesAndAllTagsOrderByViewsDesc(
                        categories, tags, PostStatus.PUBLISHED, tags.size());
            } else {
                posts = postRepository.findByCategoryInAndStatusOrderByViewsDesc(categories, PostStatus.PUBLISHED);
            }
        } else {
            java.util.List<com.openisle.model.Tag> tags = tagRepository.findAllById(tagIds);
            if (tags.isEmpty()) {
                return java.util.List.of();
            }
            posts = postRepository.findByAllTagsOrderByViewsDesc(tags, PostStatus.PUBLISHED, tags.size());
        }

        return paginate(sortByPinnedAndViews(posts), page, pageSize);
    }

    public List<Post> listPostsByLatestReply(Integer page, Integer pageSize) {
        return listPostsByLatestReply(null, null, page, pageSize);
    }

    public List<Post> listPostsByLatestReply(java.util.List<Long> categoryIds,
                                             java.util.List<Long> tagIds,
                                             Integer page,
                                             Integer pageSize) {
        boolean hasCategories = categoryIds != null && !categoryIds.isEmpty();
        boolean hasTags = tagIds != null && !tagIds.isEmpty();

        java.util.List<Post> posts;

        if (!hasCategories && !hasTags) {
            posts = postRepository.findByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED);
        } else if (hasCategories) {
            java.util.List<Category> categories = categoryRepository.findAllById(categoryIds);
            if (categories.isEmpty()) {
                return java.util.List.of();
            }
            if (hasTags) {
                java.util.List<com.openisle.model.Tag> tags = tagRepository.findAllById(tagIds);
                if (tags.isEmpty()) {
                    return java.util.List.of();
                }
                posts = postRepository.findByCategoriesAndAllTagsOrderByCreatedAtDesc(
                        categories, tags, PostStatus.PUBLISHED, tags.size());
            } else {
                posts = postRepository.findByCategoryInAndStatusOrderByCreatedAtDesc(categories, PostStatus.PUBLISHED);
            }
        } else {
            java.util.List<com.openisle.model.Tag> tags = tagRepository.findAllById(tagIds);
            if (tags.isEmpty()) {
                return java.util.List.of();
            }
            posts = postRepository.findByAllTagsOrderByCreatedAtDesc(tags, PostStatus.PUBLISHED, tags.size());
        }

        return paginate(sortByPinnedAndLastReply(posts), page, pageSize);
    }

    public List<Post> listPostsByCategories(java.util.List<Long> categoryIds,
                                            Integer page,
                                            Integer pageSize) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            java.util.List<Post> posts = postRepository.findByStatusOrderByCreatedAtDesc(PostStatus.PUBLISHED);
            return paginate(sortByPinnedAndCreated(posts), page, pageSize);
        }

        java.util.List<Category> categories = categoryRepository.findAllById(categoryIds);
        java.util.List<Post> posts = postRepository.findByCategoryInAndStatusOrderByCreatedAtDesc(categories, PostStatus.PUBLISHED);
        return paginate(sortByPinnedAndCreated(posts), page, pageSize);
    }

    public List<Post> getRecentPostsByUser(String username, int limit) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        Pageable pageable = PageRequest.of(0, limit);
        return postRepository.findByAuthorAndStatusOrderByCreatedAtDesc(user, PostStatus.PUBLISHED, pageable);
    }

    public java.time.LocalDateTime getLastPostTime(String username) {
        return postRepository.findLastPostTime(username);
    }

    public long getTotalViews(String username) {
        Long v = postRepository.sumViews(username);
        return v != null ? v : 0;
    }

    public List<Post> listPostsByTags(java.util.List<Long> tagIds,
                                      Integer page,
                                      Integer pageSize) {
        if (tagIds == null || tagIds.isEmpty()) {
            return java.util.List.of();
        }

        java.util.List<com.openisle.model.Tag> tags = tagRepository.findAllById(tagIds);
        if (tags.isEmpty()) {
            return java.util.List.of();
        }

        java.util.List<Post> posts = postRepository.findByAllTagsOrderByCreatedAtDesc(tags, PostStatus.PUBLISHED, tags.size());
        return paginate(sortByPinnedAndCreated(posts), page, pageSize);
    }

    public List<Post> listPostsByCategoriesAndTags(java.util.List<Long> categoryIds,
                                                   java.util.List<Long> tagIds,
                                                   Integer page,
                                                   Integer pageSize) {
        if (categoryIds == null || categoryIds.isEmpty() || tagIds == null || tagIds.isEmpty()) {
            return java.util.List.of();
        }

        java.util.List<Category> categories = categoryRepository.findAllById(categoryIds);
        java.util.List<com.openisle.model.Tag> tags = tagRepository.findAllById(tagIds);
        if (categories.isEmpty() || tags.isEmpty()) {
            return java.util.List.of();
        }

        java.util.List<Post> posts = postRepository.findByCategoriesAndAllTagsOrderByCreatedAtDesc(categories, tags, PostStatus.PUBLISHED, tags.size());
        return paginate(sortByPinnedAndCreated(posts), page, pageSize);
    }

    public List<Post> listPendingPosts() {
        return postRepository.findByStatus(PostStatus.PENDING);
    }

    public Post approvePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
        // publish all pending tags along with the post
        for (com.openisle.model.Tag tag : post.getTags()) {
            if (!tag.isApproved()) {
                tag.setApproved(true);
                tagRepository.save(tag);
            }
        }
        post.setStatus(PostStatus.PUBLISHED);
        post = postRepository.save(post);
        notificationService.createNotification(post.getAuthor(), NotificationType.POST_REVIEWED, post, null, true, null, null, null);
        return post;
    }

    public Post rejectPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
        // remove user created tags that are only linked to this post
        java.util.Set<com.openisle.model.Tag> tags = new java.util.HashSet<>(post.getTags());
        for (com.openisle.model.Tag tag : tags) {
            if (!tag.isApproved()) {
                long count = postRepository.countDistinctByTags_Id(tag.getId());
                if (count <= 1) {
                    post.getTags().remove(tag);
                    tagRepository.delete(tag);
                }
            }
        }
        post.setStatus(PostStatus.REJECTED);
        post = postRepository.save(post);
        notificationService.createNotification(post.getAuthor(), NotificationType.POST_REVIEWED, post, null, false, null, null, null);
        return post;
    }

    public Post pinPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
        post.setPinnedAt(java.time.LocalDateTime.now());
        return postRepository.save(post);
    }

    public Post unpinPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
        post.setPinnedAt(null);
        return postRepository.save(post);
    }

    @org.springframework.transaction.annotation.Transactional
    public Post updatePost(Long id,
                           String username,
                           Long categoryId,
                           String title,
                           String content,
                           java.util.List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            throw new IllegalArgumentException("At least one tag required");
        }
        if (tagIds.size() > 2) {
            throw new IllegalArgumentException("At most two tags allowed");
        }
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        if (!user.getId().equals(post.getAuthor().getId()) && user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Unauthorized");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        java.util.List<com.openisle.model.Tag> tags = tagRepository.findAllById(tagIds);
        if (tags.isEmpty()) {
            throw new IllegalArgumentException("Tag not found");
        }
        post.setTitle(title);
        String oldContent = post.getContent();
        post.setContent(content);
        post.setCategory(category);
        post.setTags(new java.util.HashSet<>(tags));
        Post updated = postRepository.save(post);
        imageUploader.adjustReferences(oldContent, content);
        notificationService.notifyMentions(content, user, updated, null);
        return updated;
    }

    @org.springframework.transaction.annotation.Transactional
    public void deletePost(Long id, String username) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("Post not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
        if (!user.getId().equals(post.getAuthor().getId()) && user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Unauthorized");
        }
        for (Comment c : commentRepository.findByPostAndParentIsNullOrderByCreatedAtAsc(post)) {
            commentService.deleteCommentCascade(c);
        }
        reactionRepository.findByPost(post).forEach(reactionRepository::delete);
        postSubscriptionRepository.findByPost(post).forEach(postSubscriptionRepository::delete);
        notificationRepository.deleteAll(notificationRepository.findByPost(post));
        postReadService.deleteByPost(post);
        imageUploader.removeReferences(imageUploader.extractUrls(post.getContent()));
        postRepository.delete(post);
    }

    public java.util.List<Post> getPostsByIds(java.util.List<Long> ids) {
        return postRepository.findAllById(ids);
    }

    public long countPostsByCategory(Long categoryId) {
        return postRepository.countByCategory_Id(categoryId);
    }

    public long countPostsByTag(Long tagId) {
        return postRepository.countDistinctByTags_Id(tagId);
    }

    private java.util.List<Post> sortByPinnedAndCreated(java.util.List<Post> posts) {
        return posts.stream()
                .sorted(java.util.Comparator
                        .comparing(Post::getPinnedAt, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                        .thenComparing(Post::getCreatedAt, java.util.Comparator.reverseOrder()))
                .toList();
    }

    private java.util.List<Post> sortByPinnedAndViews(java.util.List<Post> posts) {
        return posts.stream()
                .sorted(java.util.Comparator
                        .comparing(Post::getPinnedAt, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                        .thenComparing(Post::getViews, java.util.Comparator.reverseOrder()))
                .toList();
    }

    private java.util.List<Post> sortByPinnedAndLastReply(java.util.List<Post> posts) {
        return posts.stream()
                .sorted(java.util.Comparator
                        .comparing(Post::getPinnedAt, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                        .thenComparing(p -> {
                            java.time.LocalDateTime t = commentRepository.findLastCommentTime(p);
                            return t != null ? t : p.getCreatedAt();
                        }, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .toList();
    }

    private java.util.List<Post> paginate(java.util.List<Post> posts, Integer page, Integer pageSize) {
        if (page == null || pageSize == null) {
            return posts;
        }
        int from = page * pageSize;
        if (from >= posts.size()) {
            return java.util.List.of();
        }
        int to = Math.min(from + pageSize, posts.size());
        return posts.subList(from, to);
    }
}
