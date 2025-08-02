package com.openisle.repository;

import com.openisle.model.Post;
import com.openisle.model.PostStatus;
import com.openisle.model.User;
import com.openisle.model.Category;
import com.openisle.model.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByStatus(PostStatus status);
    List<Post> findByStatus(PostStatus status, Pageable pageable);
    List<Post> findByStatusOrderByCreatedAtDesc(PostStatus status);
    List<Post> findByStatusOrderByCreatedAtDesc(PostStatus status, Pageable pageable);
    List<Post> findByStatusOrderByViewsDesc(PostStatus status);
    List<Post> findByStatusOrderByViewsDesc(PostStatus status, Pageable pageable);
    List<Post> findByAuthorAndStatusOrderByCreatedAtDesc(User author, PostStatus status, Pageable pageable);
    List<Post> findByCategoryInAndStatus(List<Category> categories, PostStatus status);
    List<Post> findByCategoryInAndStatus(List<Category> categories, PostStatus status, Pageable pageable);
    List<Post> findByCategoryInAndStatusOrderByCreatedAtDesc(List<Category> categories, PostStatus status);
    List<Post> findByCategoryInAndStatusOrderByCreatedAtDesc(List<Category> categories, PostStatus status, Pageable pageable);
    List<Post> findDistinctByTagsInAndStatus(List<Tag> tags, PostStatus status);
    List<Post> findDistinctByTagsInAndStatus(List<Tag> tags, PostStatus status, Pageable pageable);
    List<Post> findDistinctByTagsInAndStatusOrderByCreatedAtDesc(List<Tag> tags, PostStatus status);
    List<Post> findDistinctByTagsInAndStatusOrderByCreatedAtDesc(List<Tag> tags, PostStatus status, Pageable pageable);
    List<Post> findDistinctByCategoryInAndTagsInAndStatus(List<Category> categories, List<Tag> tags, PostStatus status);
    List<Post> findDistinctByCategoryInAndTagsInAndStatus(List<Category> categories, List<Tag> tags, PostStatus status, Pageable pageable);
    List<Post> findDistinctByCategoryInAndTagsInAndStatusOrderByCreatedAtDesc(List<Category> categories, List<Tag> tags, PostStatus status);
    List<Post> findDistinctByCategoryInAndTagsInAndStatusOrderByCreatedAtDesc(List<Category> categories, List<Tag> tags, PostStatus status, Pageable pageable);

    // Queries requiring all provided tags to be present
    @Query("SELECT p FROM Post p JOIN p.tags t WHERE t IN :tags AND p.status = :status GROUP BY p.id HAVING COUNT(DISTINCT t.id) = :tagCount")
    List<Post> findByAllTags(@Param("tags") List<Tag> tags, @Param("status") PostStatus status, @Param("tagCount") long tagCount);

    @Query(value = "SELECT p FROM Post p JOIN p.tags t WHERE t IN :tags AND p.status = :status GROUP BY p.id HAVING COUNT(DISTINCT t.id) = :tagCount")
    List<Post> findByAllTags(@Param("tags") List<Tag> tags, @Param("status") PostStatus status, @Param("tagCount") long tagCount, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN p.tags t WHERE t IN :tags AND p.status = :status GROUP BY p.id HAVING COUNT(DISTINCT t.id) = :tagCount ORDER BY p.createdAt DESC")
    List<Post> findByAllTagsOrderByCreatedAtDesc(@Param("tags") List<Tag> tags, @Param("status") PostStatus status, @Param("tagCount") long tagCount);

    @Query(value = "SELECT p FROM Post p JOIN p.tags t WHERE t IN :tags AND p.status = :status GROUP BY p.id HAVING COUNT(DISTINCT t.id) = :tagCount ORDER BY p.createdAt DESC")
    List<Post> findByAllTagsOrderByCreatedAtDesc(@Param("tags") List<Tag> tags, @Param("status") PostStatus status, @Param("tagCount") long tagCount, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN p.tags t WHERE t IN :tags AND p.status = :status GROUP BY p.id HAVING COUNT(DISTINCT t.id) = :tagCount ORDER BY p.views DESC")
    List<Post> findByAllTagsOrderByViewsDesc(@Param("tags") List<Tag> tags, @Param("status") PostStatus status, @Param("tagCount") long tagCount);

    @Query(value = "SELECT p FROM Post p JOIN p.tags t WHERE t IN :tags AND p.status = :status GROUP BY p.id HAVING COUNT(DISTINCT t.id) = :tagCount ORDER BY p.views DESC")
    List<Post> findByAllTagsOrderByViewsDesc(@Param("tags") List<Tag> tags, @Param("status") PostStatus status, @Param("tagCount") long tagCount, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN p.tags t WHERE p.category IN :categories AND t IN :tags AND p.status = :status GROUP BY p.id HAVING COUNT(DISTINCT t.id) = :tagCount")
    List<Post> findByCategoriesAndAllTags(@Param("categories") List<Category> categories, @Param("tags") List<Tag> tags, @Param("status") PostStatus status, @Param("tagCount") long tagCount);

    @Query(value = "SELECT p FROM Post p JOIN p.tags t WHERE p.category IN :categories AND t IN :tags AND p.status = :status GROUP BY p.id HAVING COUNT(DISTINCT t.id) = :tagCount")
    List<Post> findByCategoriesAndAllTags(@Param("categories") List<Category> categories, @Param("tags") List<Tag> tags, @Param("status") PostStatus status, @Param("tagCount") long tagCount, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN p.tags t WHERE p.category IN :categories AND t IN :tags AND p.status = :status GROUP BY p.id HAVING COUNT(DISTINCT t.id) = :tagCount ORDER BY p.views DESC")
    List<Post> findByCategoriesAndAllTagsOrderByViewsDesc(@Param("categories") List<Category> categories, @Param("tags") List<Tag> tags, @Param("status") PostStatus status, @Param("tagCount") long tagCount);

    @Query(value = "SELECT p FROM Post p JOIN p.tags t WHERE p.category IN :categories AND t IN :tags AND p.status = :status GROUP BY p.id HAVING COUNT(DISTINCT t.id) = :tagCount ORDER BY p.views DESC")
    List<Post> findByCategoriesAndAllTagsOrderByViewsDesc(@Param("categories") List<Category> categories, @Param("tags") List<Tag> tags, @Param("status") PostStatus status, @Param("tagCount") long tagCount, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN p.tags t WHERE p.category IN :categories AND t IN :tags AND p.status = :status GROUP BY p.id HAVING COUNT(DISTINCT t.id) = :tagCount ORDER BY p.createdAt DESC")
    List<Post> findByCategoriesAndAllTagsOrderByCreatedAtDesc(@Param("categories") List<Category> categories, @Param("tags") List<Tag> tags, @Param("status") PostStatus status, @Param("tagCount") long tagCount);

    @Query(value = "SELECT p FROM Post p JOIN p.tags t WHERE p.category IN :categories AND t IN :tags AND p.status = :status GROUP BY p.id HAVING COUNT(DISTINCT t.id) = :tagCount ORDER BY p.createdAt DESC")
    List<Post> findByCategoriesAndAllTagsOrderByCreatedAtDesc(@Param("categories") List<Category> categories, @Param("tags") List<Tag> tags, @Param("status") PostStatus status, @Param("tagCount") long tagCount, Pageable pageable);

    List<Post> findByCategoryInAndStatusOrderByViewsDesc(List<Category> categories, PostStatus status);
    List<Post> findByCategoryInAndStatusOrderByViewsDesc(List<Category> categories, PostStatus status, Pageable pageable);
    List<Post> findDistinctByTagsInAndStatusOrderByViewsDesc(List<Tag> tags, PostStatus status);
    List<Post> findDistinctByTagsInAndStatusOrderByViewsDesc(List<Tag> tags, PostStatus status, Pageable pageable);
    List<Post> findDistinctByCategoryInAndTagsInAndStatusOrderByViewsDesc(List<Category> categories, List<Tag> tags, PostStatus status);
    List<Post> findDistinctByCategoryInAndTagsInAndStatusOrderByViewsDesc(List<Category> categories, List<Tag> tags, PostStatus status, Pageable pageable);
    List<Post> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCaseAndStatus(String titleKeyword, String contentKeyword, PostStatus status);
    List<Post> findByContentContainingIgnoreCaseAndStatus(String keyword, PostStatus status);
    List<Post> findByTitleContainingIgnoreCaseAndStatus(String keyword, PostStatus status);

    @Query("SELECT MAX(p.createdAt) FROM Post p WHERE p.author.username = :username AND p.status = com.openisle.model.PostStatus.PUBLISHED")
    LocalDateTime findLastPostTime(@Param("username") String username);

    @Query("SELECT SUM(p.views) FROM Post p WHERE p.author.username = :username AND p.status = com.openisle.model.PostStatus.PUBLISHED")
    Long sumViews(@Param("username") String username);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.author.username = :username AND p.createdAt >= :start")
    long countByAuthorAfter(@Param("username") String username, @Param("start") java.time.LocalDateTime start);

    long countByCategory_Id(Long categoryId);

    long countDistinctByTags_Id(Long tagId);
}
