package com.study.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostRequest(
        @NotBlank(message = "title is empty") @Size(max = 100, message = "title too long") String title,
        @NotBlank(message = "author is empty") @Size(max = 30, message = "author too long") String author,
        @NotBlank(message = "content is empty") @Size(max = 2000, message = "content too long") String content
) {
}
