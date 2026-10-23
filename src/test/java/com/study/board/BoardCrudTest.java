package com.study.board;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 과제 채점용 테스트. 직접 만든 클래스에 의존하지 않고 HTTP 요청/응답(JSON)으로만 검증합니다.
 * README의 "API 명세"(URL, 상태 코드, JSON 필드 이름)와 엔티티 이름(Post, Comment)은 지켜야 합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BoardCrudTest {

    @Autowired MockMvc mvc;
    @PersistenceContext EntityManager em;

    // ---------- helpers ----------

    private String uniq(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String repeat(int n) {
        return "가".repeat(n);
    }

    private String postJson(String title, String author, String content) {
        return "{\"title\":\"" + title + "\",\"author\":\"" + author + "\",\"content\":\"" + content + "\"}";
    }

    private String commentJson(String author, String content) {
        return "{\"author\":\"" + author + "\",\"content\":\"" + content + "\"}";
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b, String json) throws Exception {
        return mvc.perform(json == null ? b : b.contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions createPostRaw(String title, String author, String content) throws Exception {
        return send(post("/api/posts"), postJson(title, author, content));
    }

    /** 글을 작성하고 새 글의 id를 반환 */
    private long createPost(String title) throws Exception {
        String body = createPostRaw(title, "철수", "본문")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(body, "$.id");
        return id.longValue();
    }

    private String getPost(long id) throws Exception {
        return mvc.perform(get("/api/posts/" + id)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private void addComment(long postId, String content) throws Exception {
        send(post("/api/posts/" + postId + "/comments"), commentJson("영희", content))
                .andExpect(status().isCreated());
    }

    private long firstCommentId(long postId) throws Exception {
        Number id = JsonPath.read(getPost(postId), "$.comments[0].id");
        return id.longValue();
    }

    private long commentCount() {
        try {
            return em.createQuery("select count(c) from Comment c", Long.class).getSingleResult();
        } catch (RuntimeException e) {
            throw new AssertionError("엔티티 이름이 'Comment' 인지 확인하세요. (테스트가 JPQL 'from Comment'로 조회합니다) 원인: " + e.getMessage(), e);
        }
    }

    // ---------- 게시글 ----------

    @Test
    void 글_작성은_201과_저장된_글을_반환() throws Exception {
        String title = uniq("제목");

        createPostRaw(title, "철수", "본문")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.author").value("철수"))
                .andExpect(jsonPath("$.content").value("본문"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void 글_상세_조회() throws Exception {
        String title = uniq("상세");
        long id = createPost(title);

        mvc.perform(get("/api/posts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.author").value("철수"))
                .andExpect(jsonPath("$.content").value("본문"))
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments").isEmpty());
    }

    @Test
    void 목록은_최신글이_먼저이고_댓글수를_포함() throws Exception {
        String first = uniq("첫번째"), second = uniq("두번째");
        long firstId = createPost(first);
        createPost(second);
        addComment(firstId, "c1");
        addComment(firstId, "c2");

        String body = mvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn().getResponse().getContentAsString();

        List<String> titles = JsonPath.read(body, "$[*].title");
        assertThat(titles).contains(first, second);
        assertThat(titles.indexOf(second)).as("최신 글(두번째)이 먼저 와야 합니다").isLessThan(titles.indexOf(first));

        List<Integer> counts = JsonPath.read(body, "$[?(@.title=='" + first + "')].commentCount");
        assertThat(counts).containsExactly(2);
        List<String> authors = JsonPath.read(body, "$[?(@.title=='" + first + "')].author");
        assertThat(authors).containsExactly("철수");
        List<Object> ids = JsonPath.read(body, "$[?(@.title=='" + first + "')].id");
        assertThat(ids).hasSize(1);
        List<Object> createdAt = JsonPath.read(body, "$[?(@.title=='" + first + "')].createdAt");
        assertThat(createdAt).hasSize(1);
    }

    @Test
    void 글_검증_실패는_400_각_필드와_길이제한() throws Exception {
        List<String[]> invalid = List.of(
                new String[]{" ", "철수", "본문"},
                new String[]{repeat(101), "철수", "본문"},
                new String[]{"제목", "", "본문"},
                new String[]{"제목", repeat(31), "본문"},
                new String[]{"제목", "철수", ""},
                new String[]{"제목", "철수", repeat(2001)});

        for (String[] c : invalid) {
            createPostRaw(c[0], c[1], c[2]).andExpect(status().isBadRequest());
        }
    }

    @Test
    void 글_경계값은_통과() throws Exception {
        createPostRaw(repeat(100), repeat(30), repeat(2000)).andExpect(status().isCreated());
    }

    @Test
    void 없는_글은_404() throws Exception {
        mvc.perform(get("/api/posts/999999")).andExpect(status().isNotFound());
        send(put("/api/posts/999999"), postJson("t", "a", "c")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/posts/999999")).andExpect(status().isNotFound());
    }

    @Test
    void 글_수정() throws Exception {
        long id = createPost(uniq("원래제목"));
        String newTitle = uniq("새제목");

        send(put("/api/posts/" + id), postJson(newTitle, "영희", "새내용"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value(newTitle));

        mvc.perform(get("/api/posts/" + id))
                .andExpect(jsonPath("$.title").value(newTitle))
                .andExpect(jsonPath("$.author").value("영희"))
                .andExpect(jsonPath("$.content").value("새내용"));
    }

    @Test
    void 수정하면_updatedAt이_달라진다() throws Exception {
        long id = createPost(uniq("제목"));
        String before = getPost(id);
        assertThat((String) JsonPath.read(before, "$.updatedAt"))
                .as("수정 전에는 createdAt == updatedAt 이어야 합니다 (화면의 '(수정됨)' 표시 기준)")
                .isEqualTo(JsonPath.read(before, "$.createdAt"));

        send(put("/api/posts/" + id), postJson(uniq("수정제목"), "철수", "수정본문")).andExpect(status().isOk());

        String after = getPost(id);
        assertThat((String) JsonPath.read(after, "$.updatedAt"))
                .as("수정 후에는 updatedAt이 createdAt과 달라야 합니다")
                .isNotEqualTo(JsonPath.read(after, "$.createdAt"));
    }

    @Test
    void 글_수정_검증실패는_400이고_값은_그대로() throws Exception {
        String title = uniq("원래제목");
        long id = createPost(title);

        send(put("/api/posts/" + id), postJson("", "영희", "새내용")).andExpect(status().isBadRequest());

        mvc.perform(get("/api/posts/" + id)).andExpect(jsonPath("$.title").value(title));
    }

    @Test
    void 글_삭제는_204() throws Exception {
        long id = createPost(uniq("삭제대상"));

        mvc.perform(delete("/api/posts/" + id)).andExpect(status().isNoContent());

        mvc.perform(get("/api/posts/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void 글_삭제시_댓글도_삭제() throws Exception {
        long id = createPost(uniq("삭제대상"));
        addComment(id, "c1");
        addComment(id, "c2");
        long before = commentCount();

        mvc.perform(delete("/api/posts/" + id)).andExpect(status().isNoContent());

        assertThat(before - commentCount()).isEqualTo(2);
    }

    // ---------- 댓글 ----------

    @Test
    void 댓글_작성은_201과_저장된_댓글을_반환() throws Exception {
        long id = createPost(uniq("글"));
        String text = uniq("안녕");

        send(post("/api/posts/" + id + "/comments"), commentJson("영희", text))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.author").value("영희"))
                .andExpect(jsonPath("$.content").value(text))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        mvc.perform(get("/api/posts/" + id))
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.comments[0].content").value(text))
                .andExpect(jsonPath("$.comments[0].author").value("영희"))
                .andExpect(jsonPath("$.comments[0].createdAt").isNotEmpty());
    }

    @Test
    void 댓글은_작성순으로_보인다() throws Exception {
        long id = createPost(uniq("글"));
        String c1 = uniq("첫댓글"), c2 = uniq("둘째댓글"), c3 = uniq("셋째댓글");
        addComment(id, c1);
        addComment(id, c2);
        addComment(id, c3);

        List<String> contents = JsonPath.read(getPost(id), "$.comments[*].content");
        assertThat(contents).containsExactly(c1, c2, c3);
    }

    @Test
    void 댓글_검증실패는_400_각_필드와_길이제한() throws Exception {
        long id = createPost(uniq("글"));
        long before = commentCount();
        String[][] invalid = {
                {"", "내용"},
                {repeat(31), "내용"},
                {"영희", ""},
                {"영희", repeat(501)}};

        for (String[] c : invalid) {
            send(post("/api/posts/" + id + "/comments"), commentJson(c[0], c[1])).andExpect(status().isBadRequest());
        }
        assertThat(commentCount()).isEqualTo(before);

        send(post("/api/posts/" + id + "/comments"), commentJson(repeat(30), repeat(500)))
                .andExpect(status().isCreated());
    }

    @Test
    void 없는_글에_댓글은_404() throws Exception {
        send(post("/api/posts/999999/comments"), commentJson("a", "c")).andExpect(status().isNotFound());
    }

    @Test
    void 댓글_삭제는_204() throws Exception {
        long id = createPost(uniq("글"));
        String text = uniq("지울댓글");
        addComment(id, text);
        long commentId = firstCommentId(id);

        mvc.perform(delete("/api/posts/" + id + "/comments/" + commentId)).andExpect(status().isNoContent());

        mvc.perform(get("/api/posts/" + id)).andExpect(jsonPath("$.comments").isEmpty());
    }

    @Test
    void 다른_글의_댓글은_삭제할수_없다() throws Exception {
        long p1 = createPost(uniq("글1"));
        long p2 = createPost(uniq("글2"));
        addComment(p1, uniq("남아야함"));
        long commentId = firstCommentId(p1);

        mvc.perform(delete("/api/posts/" + p2 + "/comments/" + commentId)).andExpect(status().isNotFound());

        mvc.perform(get("/api/posts/" + p1)).andExpect(jsonPath("$.comments.length()").value(1));
    }

    @Test
    void 없는_댓글_삭제는_404() throws Exception {
        long id = createPost(uniq("글"));
        mvc.perform(delete("/api/posts/" + id + "/comments/999999")).andExpect(status().isNotFound());
    }

    // ---------- 제공된 화면 ----------

    @Test
    void 제공된_화면이_서빙된다() throws Exception {
        for (String page : List.of("/", "/index.html", "/form.html", "/post.html", "/js/api.js")) {
            mvc.perform(get(page)).andExpect(status().isOk());
        }
    }
}
