package com.study.board.service;

import com.study.board.domain.Comment;
import com.study.board.domain.Post;
import com.study.board.dto.CommentRequest;
import com.study.board.dto.CommentResponse;
import com.study.board.repository.CommentRepository;
import com.study.board.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    @Autowired
    public CommentService(CommentRepository commentRepository, PostRepository postRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
    }

    @Transactional
    public CommentResponse createComment(Long postId, CommentRequest commentRequest) {
        Optional<Post> rawPost = postRepository.findById(postId);
        Post post = rawPost.orElseThrow(() -> new PostNotFoundException(postId));
        Comment comment = new Comment(commentRequest.author(), commentRequest.content());
        post.addComment(comment);
        postRepository.flush();
        return CommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(Long postId, Long commentId) {
        Optional<Comment> rawComment = commentRepository.findById(commentId);
        Comment comment = rawComment.orElseThrow(() -> new CommentNotFoundException(commentId));
        if(comment.getPost().getId().equals(postId)) {
            comment.getPost().getComments().remove(comment);
        } else {
            throw new CommentNotFoundException(commentId);
        }
    }
}
