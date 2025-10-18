package com.openisle.model;

import com.openisle.model.Tag;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Post entity representing an article posted by a user.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "posts")
@Inheritance(strategy = InheritanceType.JOINED)
public class Post {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false, columnDefinition = "LONGTEXT")
  private String content;

  @CreationTimestamp
  @Column(
    nullable = false,
    updatable = false,
    columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)"
  )
  private LocalDateTime createdAt;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "author_id")
  private User author;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "category_id")
  private Category category;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
    name = "post_tags",
    joinColumns = @JoinColumn(name = "post_id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id")
  )
  private Set<Tag> tags = new HashSet<>();

  @Column(nullable = false)
  private long views = 0;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PostStatus status = PostStatus.PUBLISHED;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PostType type = PostType.NORMAL;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PostVisibleScopeType visibleScope = PostVisibleScopeType.ALL;

  @Column(nullable = false)
  private boolean closed = false;

  @Column
  private LocalDateTime pinnedAt;

  @Column(nullable = true)
  private Boolean rssExcluded = true;

  @Column(nullable = false)
  private long commentCount = 0;

  @Column(nullable = true)
  private LocalDateTime lastReplyAt;
}
