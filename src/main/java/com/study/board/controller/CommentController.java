package com.study.board.controller;

import com.study.board.dto.CommentRequest;
import com.study.board.dto.CommentResponse;
import com.study.board.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class CommentController {

    private final CommentService commentService;

    @Autowired
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/api/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> createComment(@PathVariable(value = "postId") Long postId,
                                                         @Valid @RequestBody CommentRequest commentRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createComment(postId, commentRequest));
    }

    @DeleteMapping("/api/posts/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable(value = "postId") Long postId,
                                              @PathVariable(value = "commentId") Long commentId) {
        commentService.deleteComment(postId, commentId);
        return ResponseEntity.noContent().build();
    }
}
