package com.study.board.dto;

import com.study.board.domain.Post;

import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(
        Long id,
        String title,
        String author,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CommentResponse> comments
) {
    public static PostResponse from(Post p) {
        return new PostResponse(p.getId(), p.getTitle(), p.getAuthor(), p.getContent(),
                p.getCreatedAt(), p.getUpdatedAt(),
                p.getComments().stream().map(CommentResponse::from).toList());
    }
}
