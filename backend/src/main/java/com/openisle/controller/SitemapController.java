package com.openisle.controller;

import com.openisle.model.Post;
import com.openisle.model.PostStatus;
import com.openisle.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for dynamic sitemap generation.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SitemapController {
    private final PostRepository postRepository;

    @Value("${app.website-url:https://www.open-isle.com}")
    private String websiteUrl;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        List<Post> posts = postRepository.findByStatus(PostStatus.PUBLISHED);

        StringBuilder body = new StringBuilder();
        body.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        body.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        List<String> staticRoutes = List.of(
                "/",
                "/about",
                "/activities",
                "/login",
                "/signup"
        );

        for (String path : staticRoutes) {
            body.append("  <url><loc>")
                .append(websiteUrl)
                .append(path)
                .append("</loc></url>\n");
        }

        for (Post p : posts) {
            body.append("  <url>\n")
                .append("    <loc>")
                .append(websiteUrl)
                .append("/posts/")
                .append(p.getId())
                .append("</loc>\n")
                .append("    <lastmod>")
                .append(p.getCreatedAt().toLocalDate())
                .append("</lastmod>\n")
                .append("  </url>\n");
        }

        body.append("</urlset>");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(body.toString());
    }
}
