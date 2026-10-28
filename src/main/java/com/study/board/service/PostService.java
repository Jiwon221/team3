package com.study.board.service;

import com.study.board.domain.Post;
import com.study.board.dto.PostRequest;
import com.study.board.dto.PostResponse;
import com.study.board.dto.PostSummaryResponse;
import com.study.board.repository.PostRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    @Autowired
    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional
    public PostResponse createPost(PostRequest postRequest) {
        //1. PostRequest -> Post 변환
        Post post = new Post(postRequest.title(), postRequest.author(), postRequest.content());
        //2, Post를 저장
        Post savedPost = postRepository.save(post);
        //3. 저장한 Post -> PostResponse로 반환
        PostResponse postResponse = PostResponse.from(savedPost);

        return postResponse;
    }

    public List<PostSummaryResponse> findAll() {
        //1. repository에서 모든 post 데이터를 가져온다.
        List<Post> allPost = postRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        List<PostSummaryResponse> postSummaryResponses = new ArrayList<>();
        for (Post post : allPost) {
            postSummaryResponses.add(PostSummaryResponse.from(post));
        }
        //4. 반환
        return postSummaryResponses;
    }

    public PostResponse findById(Long id) {
        Optional<Post> rawPost = postRepository.findById(id);
        Post post = rawPost.orElseThrow(() -> new PostNotFoundException(id));
        PostResponse postResponse = PostResponse.from(post);

        return postResponse;
    }

    @Transactional
    public PostResponse updatedPost(Long id, PostRequest postRequest) {
        // 1. id로 post 조회.
        Optional<Post> rawPost = postRepository.findById(id);
        // 2. post를 Optional -> Post 형식으로 변환.
        Post post = rawPost.orElseThrow(() -> new PostNotFoundException(id));
        // 3. 게시글 수정
        post.update(postRequest.title(), postRequest.author(), postRequest.content());
        postRepository.flush(); //시간 업데이트 필수
        // 4. 반환
        return PostResponse.from(post);
    }

    @Transactional
    public void deletePost(Long id) {
        Optional<Post> rawPost = postRepository.findById(id);
        Post post = rawPost.orElseThrow(() -> new PostNotFoundException(id));
        postRepository.deleteById(id);
    }
}
