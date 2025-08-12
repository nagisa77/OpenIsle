package com.openisle.repository;

import com.openisle.model.Comment;
import com.openisle.model.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.openisle.model.User;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostAndParentIsNullOrderByCreatedAtAsc(Post post);
    List<Comment> findByParentOrderByCreatedAtAsc(Comment parent);
    List<Comment> findByAuthorOrderByCreatedAtDesc(User author, Pageable pageable);
    List<Comment> findByContentContainingIgnoreCase(String keyword);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT c.author FROM Comment c WHERE c.post = :post")
    java.util.List<User> findDistinctAuthorsByPost(@org.springframework.data.repository.query.Param("post") Post post);

    @org.springframework.data.jpa.repository.Query("SELECT MAX(c.createdAt) FROM Comment c WHERE c.post = :post")
    java.time.LocalDateTime findLastCommentTime(@org.springframework.data.repository.query.Param("post") Post post);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(c) FROM Comment c WHERE c.author.username = :username AND c.createdAt >= :start")
    long countByAuthorAfter(@org.springframework.data.repository.query.Param("username") String username,
                            @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start);

    @org.springframework.data.jpa.repository.Query("SELECT MAX(c.createdAt) FROM Comment c WHERE c.author.id = :userId")
    java.time.LocalDateTime findLastCommentTimeOfUserByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId")
    long countByPostId(@org.springframework.data.repository.query.Param("postId") Long postId);

    long countByAuthor_Id(Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT FUNCTION('date', c.createdAt) AS d, COUNT(c) AS c FROM Comment c " +
            "WHERE c.createdAt >= :start AND c.createdAt < :end GROUP BY d ORDER BY d")
    java.util.List<Object[]> countDailyRange(@org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
                                             @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);
}
