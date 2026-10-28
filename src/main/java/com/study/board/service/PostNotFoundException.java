package com.study.board.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PostNotFoundException extends RuntimeException {
    public PostNotFoundException(Long id) {
        super("게시글을 찾을 수 없습니다. id=" + id);
    }
}
