package com.openisle.service;

import com.openisle.model.Post;
import com.openisle.model.PostStatus;
import com.openisle.model.Comment;
import com.openisle.model.User;
import com.openisle.repository.PostRepository;
import com.openisle.repository.CommentRepository;
import com.openisle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.stream.Stream;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public List<User> searchUsers(String keyword) {
        return userRepository.findByUsernameContainingIgnoreCase(keyword);
    }

    public List<Post> searchPosts(String keyword) {
        return postRepository
                .findByTitleContainingIgnoreCaseOrContentContainingIgnoreCaseAndStatus(keyword, keyword, PostStatus.PUBLISHED);
    }

    public List<Post> searchPostsByContent(String keyword) {
        return postRepository
                .findByContentContainingIgnoreCaseAndStatus(keyword, PostStatus.PUBLISHED);
    }

    public List<Post> searchPostsByTitle(String keyword) {
        return postRepository
                .findByTitleContainingIgnoreCaseAndStatus(keyword, PostStatus.PUBLISHED);
    }

    public List<Comment> searchComments(String keyword) {
        return commentRepository.findByContentContainingIgnoreCase(keyword);
    }

    public List<SearchResult> globalSearch(String keyword) {
        Stream<SearchResult> users = searchUsers(keyword).stream()
                .map(u -> new SearchResult(
                        "user",
                        u.getId(),
                        u.getUsername(),
                        safeSnippet(u.getIntroduction(), null)
                ));

        // Merge post results while removing duplicates between search by content
        // and search by title
        List<SearchResult> mergedPosts = Stream.concat(
                    searchPosts(keyword).stream()
                            .map(p -> new SearchResult(
                                    "post",
                                    p.getId(),
                                    p.getTitle(),
                                    safeSnippet(p.getContent(), keyword)
                            )),
                    searchPostsByTitle(keyword).stream()
                            .map(p -> new SearchResult(
                                    "post_title",
                                    p.getId(),
                                    p.getTitle(),
                                    safeSnippet(p.getContent(), null)
                            ))
                )
                .collect(java.util.stream.Collectors.toMap(
                        SearchResult::id,
                        sr -> sr,
                        (a, b) -> a,
                        java.util.LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();

        Stream<SearchResult> comments = searchComments(keyword).stream()
                .map(c -> new SearchResult(
                        "comment",
                        c.getId(),
                        c.getContent(),
                        c.getAuthor().getUsername() + " - " + c.getPost().getTitle()
                ));

        return Stream.concat(Stream.concat(users, mergedPosts.stream()), comments)
                .toList();
    }

    private String safeSnippet(String text, String keyword) {
        if (text == null) return "";
        String clean = text.replaceAll("\\s+", " ").trim();
        if (keyword == null) {
            return clean.length() > 80 ? clean.substring(0, 80) : clean;
        }
        String lower = clean.toLowerCase();
        String kw = keyword.toLowerCase();
        int idx = lower.indexOf(kw);
        if (idx == -1) {
            return clean.length() > 80 ? clean.substring(0, 80) : clean;
        }
        int start = Math.max(0, idx - 20);
        int end = Math.min(clean.length(), idx + kw.length() + 60);
        return clean.substring(start, end);
    }

    public record SearchResult(String type, Long id, String text, String subText) {}
}
