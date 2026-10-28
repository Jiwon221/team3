package com.study.board.controller;

import com.study.board.domain.Post;
import com.study.board.dto.PostRequest;
import com.study.board.dto.PostResponse;
import com.study.board.dto.PostSummaryResponse;
import com.study.board.service.PostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PostController {

    private final PostService postService;

    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/api/posts")
    public ResponseEntity<List<PostSummaryResponse>> getPostList() {
        return ResponseEntity.ok(postService.findAll());
    }

    @PostMapping("/api/posts")
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody PostRequest postRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(postRequest));
    }

    @GetMapping("/api/posts/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable(value = "id") Long id) {
        return ResponseEntity.ok(postService.findById(id));
    }

    @PutMapping("/api/posts/{id}")
    public ResponseEntity<PostResponse> updatePost(@PathVariable(value = "id") Long id,
                                                   @Valid @RequestBody PostRequest postRequest) {
        return ResponseEntity.ok(postService.updatedPost(id, postRequest));
    }

    @DeleteMapping("/api/posts/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable(value = "id") Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }


}
