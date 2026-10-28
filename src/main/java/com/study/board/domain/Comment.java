package com.study.board.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(nullable = false, length = 30)
    private String author;

    @Column(nullable = false, length = 500)
    private String content;

    @Column
    private LocalDateTime createdAt;

    public Comment(String author, String content) {
        if (author == null || author.isBlank()) {
            throw new IllegalArgumentException("author is blank");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content is blank");
        }
        if (author.length() > 30) {
            throw new IllegalArgumentException("author too long");
        }
        if (content.length() > 500) {
            throw new IllegalArgumentException("content too long");
        }
        this.author = author;
        this.content = content;
    }

    public Comment() {
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Post getPost() {
        return post;
    }

    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setPost(Post post) {
        this.post = post;
    }
}
