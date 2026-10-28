package com.study.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "author is empty") @Size(max = 30, message = "author too long") String author,
        @NotBlank(message = "content is empty") @Size(max = 500, message = "content too long") String content
) {
}
