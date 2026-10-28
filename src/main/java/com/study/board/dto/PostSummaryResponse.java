package com.study.board.dto;

import com.study.board.domain.Post;

import java.time.LocalDateTime;

public record PostSummaryResponse(
        Long id,
        String title,
        String author,
        LocalDateTime createdAt,
        int commentCount
) {
    public static PostSummaryResponse from(Post p) {
        return new PostSummaryResponse(p.getId(), p.getTitle(), p.getAuthor(), p.getCreatedAt(), p.getComments().size());
    }
}
