package com.study.board.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CommentNotFoundException extends RuntimeException {
  public CommentNotFoundException(Long commentId) {
    super("댓글을 찾을 수 없습니다. commentId=" + commentId);
  }
}