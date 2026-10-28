package com.study.board.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 30)
    private String author;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<Comment> comments = new ArrayList<>();

    public Post(String title, String author, String content) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("id required");
        }
        if (title.length() > 100) {
            throw new IllegalArgumentException("title too long");
        }

        if (author.length() > 30) {
            throw new IllegalArgumentException("author too long");
        }
        if (content.length() > 2000) {
            throw new IllegalArgumentException("content too long");
        }

        this.title = title;
        this.author = author;
        this.content = content;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Post() {
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void update(String title, String author, String content) {
        if (title.length() > 100) {
            throw new IllegalArgumentException("title too long");
        }

        if (author.length() > 30) {
            throw new IllegalArgumentException("author too long");
        }
        if (content.length() > 2000) {
            throw new IllegalArgumentException("content too long");
        }

        this.title = title;
        this.author = author;
        this.content = content;
    }

    public void addComment(Comment comment) {
        this.comments.add(comment);
        comment.setPost(this);
    }
}
