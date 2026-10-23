# 게시판 REST API 실습 (Spring Boot + JPA)

게시글(Post)과 댓글(Comment)이 있는 게시판의 **백엔드 REST API를 만드는** 과제입니다.
**화면(HTML/JS)은 모두 제공**되며, 여러분은 **도메인 설계 → 리포지토리 → DTO → 서비스 → 컨트롤러**를 직접 구현해
화면이 동작하게 만듭니다. 컨트롤러는 `@RestController`, 응답은 `ResponseEntity<DTO>` 로 반환합니다.

> **예상 소요 시간**: 3~4시간 (도전 과제 제외) · **난이도**: Spring MVC/JPA 입문~초급

| 항목 | 버전/기술 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.x |
| Web | `@RestController` + JSON |
| Data | Spring Data JPA + H2 (인메모리) |
| Build | Gradle (wrapper 포함) |
| 화면 | 정적 HTML + JS(fetch) — 제공 |

## 1. 실행 방법

```bash
./gradlew bootRun          # http://localhost:8080  (8080이 사용 중이면 --args='--server.port=8089')
./gradlew test             # 채점용 테스트 (처음엔 대부분 실패, 구현하면서 초록불로)
```

> 처음 `./gradlew test`를 돌리면 대부분 실패하는 게 정상입니다.
> 단, `없는_글은_404`, `없는_글에_댓글은_404`, `제공된_화면이_서빙된다` 3개는 아무것도 구현하지 않아도 통과합니다
> (매핑이 없으면 404가 나오기 때문). 나머지가 실제 구현을 검증합니다.

- 화면: http://localhost:8080 — 앱을 실행하고 브라우저로 접속하면 됩니다.
  화면은 `src/main/resources/static/`의 정적 파일이라 **컨트롤러를 만들 필요 없이** Spring Boot가 자동으로 보여줍니다.
  (API를 구현하기 전에는 목록 대신 에러 메시지가 표시됩니다. 구현하면서 화면이 점점 살아나는 걸 확인해 보세요.)
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:board`)
- API를 화면 없이 직접 호출해 보려면 `requests.http`(IntelliJ) 또는 `curl`/Postman을 사용하세요.
- 서버를 재시작하면 데이터가 초기화됩니다.

## 2. 제공되는 것 / 직접 만들 것

| 구분 | 상태 |
|---|---|
| 화면 `static/index.html`, `form.html`, `post.html`, `js/api.js`, `css/style.css` | **제공** (수정 불필요) |
| `application.properties`, 채점 테스트, `requests.http` | **제공** |
| **도메인(엔티티)** `Post`, `Comment` | **직접 설계** |
| **리포지토리** | **직접 작성** |
| **DTO** (요청/응답) | **직접 작성** |
| **서비스** | **직접 작성** |
| **컨트롤러** (`@RestController`, `ResponseEntity<DTO>`) | **직접 작성** |
| **예외** (404 처리) | **직접 작성** |

패키지 구조는 자유지만 아래를 추천합니다.

```
com.study.board
├── domain       Post, Comment
├── repository   PostRepository, CommentRepository
├── dto          요청/응답 DTO
├── service      PostService, CommentService, 예외
└── controller   PostController, CommentController
```

> 추천 순서: 도메인 → 리포지토리 → DTO → 서비스 → 컨트롤러.
> 한 단계 끝날 때마다 앱이 뜨는지 확인하고, 컨트롤러까지 만들면 `./gradlew test`로 채점합니다.
> 테스트는 여러분이 만든 클래스에 직접 의존하지 않고 **HTTP(JSON)로만** 검증하므로, 아래 "API 명세"만 지키면 내부 설계는 자유입니다.

## 3. 도메인 요구사항

게시판에는 **게시글(Post)** 과 **댓글(Comment)** 이 있습니다. 클래스는 직접 설계하되 아래 명세를 만족해야 합니다.

**게시글 Post**

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | Long | PK, 자동 생성 | |
| `title` | String | not null, 최대 100자 | 제목 |
| `author` | String | not null, 최대 30자 | 작성자 이름 (회원 기능 없음, 단순 문자열) |
| `content` | String | not null, 최대 2000자 | 본문 |
| `createdAt` | LocalDateTime | 저장 시 자동 설정 | 작성 시각 |
| `updatedAt` | LocalDateTime | 저장/수정 시 자동 갱신 | 최종 수정 시각 |
| `comments` | List<Comment> | | 이 글의 댓글들 |

**댓글 Comment**

| 필드 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | Long | PK, 자동 생성 | |
| `post` | Post | not null | 소속 게시글 |
| `author` | String | not null, 최대 30자 | 작성자 이름 |
| `content` | String | not null, 최대 500자 | 댓글 내용 |
| `createdAt` | LocalDateTime | 저장 시 자동 설정 | 작성 시각 |

**관계와 규칙**

- Post 1 : N Comment, 댓글은 반드시 하나의 글에 속합니다. 연관관계의 주인은 어느 쪽이어야 할지 직접 정합니다.
- 글을 삭제하면 그 글의 댓글도 함께 삭제됩니다.
- 댓글은 작성순(id 오름차순)으로 보여야 합니다.
- 글을 수정하면 `title`, `author`, `content`가 바뀌고 `updatedAt`이 갱신됩니다.
  글을 막 작성한 직후에는 `createdAt`과 `updatedAt`이 **같은 값**이어야 합니다 (화면의 "(수정됨)" 표시 기준).
- 댓글은 수정하지 않고 삭제만 가능합니다 (수정은 도전 과제).
- 엔티티 이름은 **`Post`, `Comment`** 여야 합니다 (테스트가 JPQL로 조회).
- 엔티티를 무분별한 setter로 열어두지 말고, 변경은 의미 있는 메서드(예: 글 수정, 댓글 추가)로 표현해 보세요.
- **엔티티를 응답으로 직접 반환하지 마세요.** 응답은 반드시 DTO로 변환해서 반환합니다.

### 검증(Validation) 요구사항

| 필드 | 글 | 댓글 |
|---|---|---|
| 제목 | 필수(공백만 있어도 안 됨), 100자 이하 | - |
| 작성자 | 필수, 30자 이하 | 필수, 30자 이하 |
| 내용 | 필수, 2000자 이하 | 필수, 500자 이하 |

요청 본문을 `@Valid @RequestBody`로 받아 검증하고, 실패하면 **400 Bad Request** 를 응답합니다.
(Spring 기본 동작을 쓰면 되고, 에러 메시지를 직접 만들면 화면에 그대로 표시됩니다.)

## 4. API 명세

모든 요청/응답 본문은 JSON 입니다. 날짜(`createdAt`, `updatedAt`)는 ISO 형식 문자열(예: `2026-10-23T12:24:04.495086`)입니다.

| 기능 | Method | URL | 요청 본문 | 성공 응답 |
|---|---|---|---|---|
| 글 목록 | GET | `/api/posts` | - | `200` + `PostSummary[]` (최신 글이 먼저) |
| 글 작성 | POST | `/api/posts` | `PostRequest` | `201` + `PostDetail` |
| 글 상세 | GET | `/api/posts/{id}` | - | `200` + `PostDetail` |
| 글 수정 | PUT | `/api/posts/{id}` | `PostRequest` | `200` + `PostDetail` |
| 글 삭제 | DELETE | `/api/posts/{id}` | - | `204` (본문 없음), 댓글도 삭제 |
| 댓글 작성 | POST | `/api/posts/{postId}/comments` | `CommentRequest` | `201` + `Comment` |
| 댓글 삭제 | DELETE | `/api/posts/{postId}/comments/{commentId}` | - | `204` (본문 없음) |

**실패 응답**

| 상황 | 상태 코드 |
|---|---|
| 검증 실패 (필수값 누락, 길이 초과) | `400` |
| 없는 글 (조회/수정/삭제/댓글 작성) | `404` |
| 없는 댓글, 또는 **다른 글에 속한 댓글** 삭제 | `404` |

### JSON 형식

**PostRequest** (글 작성/수정 요청)
```json
{ "title": "제목", "author": "철수", "content": "본문" }
```

**CommentRequest** (댓글 작성 요청)
```json
{ "author": "영희", "content": "댓글 내용" }
```

**PostSummary** (목록의 각 항목)
```json
{ "id": 1, "title": "제목", "author": "철수", "createdAt": "2026-10-23T12:24:04.495086", "commentCount": 2 }
```

**PostDetail** (작성/상세/수정 응답)
```json
{
  "id": 1, "title": "제목", "author": "철수", "content": "본문",
  "createdAt": "2026-10-23T12:24:04.495086",
  "updatedAt": "2026-10-23T12:24:04.495086",
  "comments": [
    { "id": 1, "author": "영희", "content": "댓글 내용", "createdAt": "2026-10-23T12:30:00.123456" }
  ]
}
```

**Comment** (댓글 작성 응답)
```json
{ "id": 1, "author": "영희", "content": "댓글 내용", "createdAt": "2026-10-23T12:30:00.123456" }
```

> 필드 이름이 다르면 화면이 비거나 `undefined`가 표시됩니다. 위 이름 그대로 응답하세요.

## 5. 화면(제공) 사용 안내

화면은 브라우저에서 `fetch`로 위 API를 호출하는 정적 페이지입니다. **HTML/JS는 수정하지 않습니다.**

| 화면 | 호출하는 API |
|---|---|
| `index.html` (목록) | `GET /api/posts` |
| `form.html` (작성) | `POST /api/posts` |
| `form.html?id=1` (수정) | `GET /api/posts/1`, `PUT /api/posts/1` |
| `post.html?id=1` (상세) | `GET /api/posts/1`, `DELETE /api/posts/1`, 댓글 `POST`/`DELETE` |

- 화면이 이상하면 브라우저 개발자도구(F12) → **Network** 탭에서 요청의 상태 코드와 응답 본문을 확인하세요.
  서버 쪽 문제인지(상태 코드/JSON 형식), 화면 쪽 문제인지 구분할 수 있습니다.
- 서버가 성공 응답에 `id`를 내려주지 않으면 작성/수정 후 상세 화면으로 이동하지 못합니다.

## 6. 요구사항 체크리스트

### 기본 (테스트로 채점)
- [ ] 게시글 목록(최신순, 댓글 수 포함) / 상세 / 작성 / 수정 / 삭제
- [ ] 댓글 작성 / 삭제
- [ ] 입력 검증 실패 시 400 (`@Valid`), 길이 경계값은 통과
- [ ] 없는 게시글/댓글 접근 시 404
- [ ] 게시글 삭제 시 댓글 함께 삭제
- [ ] 다른 게시글의 댓글은 삭제할 수 없음
- [ ] 작성 직후 `createdAt == updatedAt`, 수정 후에는 달라짐

### 도전 (선택)
- [ ] 페이징 (`Pageable`, 페이지당 10개) — 응답 형식을 바꾸면 화면도 함께 고쳐야 합니다
- [ ] 제목 검색 (`/api/posts?keyword=...`)
- [ ] 조회수 증가
- [ ] 댓글 수정
- [ ] 목록의 댓글 수 조회 시 N+1 문제 확인 후 해결 (`show-sql` 로그로 확인 → fetch join / `@EntityGraph` / `@BatchSize`)
- [ ] `@RestControllerAdvice`로 예외를 한 곳에서 처리하고 에러 응답 JSON 통일
- [ ] Service 단위 테스트 (Mockito) 작성

## 7. 학습 포인트

0. **도메인 설계**: 엔티티, 연관관계, 생성 시각 처리 (`@PrePersist`/`@PreUpdate` 또는 Auditing)
1. **계층 분리**: Controller → Service → Repository, 각 계층의 책임
2. **DTO**: 요청/응답 DTO를 엔티티와 분리하는 이유, 엔티티 → DTO 변환
3. **ResponseEntity**: 상태 코드(200/201/204)를 의도에 맞게 쓰기
4. **Bean Validation**: `@Valid`, `@NotBlank`, `@Size`
5. **JPA 변경 감지**: `update()` 후 `save()` 없이도 반영되는 이유 (트랜잭션, 영속 상태)
6. **연관관계/영속성 전이**: 연관관계 주인, `cascade` (`orphanRemoval`은 심화 선택)
7. **예외 → HTTP 상태 코드**: `@ResponseStatus`

## 8. 힌트

<details>
<summary>JPA 키워드 힌트 (도메인 설계)</summary>

`@Entity`, `@Id` + `@GeneratedValue`, `@Column(nullable, length)`, `@ManyToOne` / `@OneToMany(mappedBy)`,
`cascade`, `@OrderBy`, `@PrePersist` / `@PreUpdate`(또는 Spring Data Auditing).
(`orphanRemoval`은 몰라도 됩니다. 아래 "댓글 삭제" 힌트 참고)
</details>

<details>
<summary>201 Created 응답은 어떻게 만드나요?</summary>

`ResponseEntity.status(HttpStatus.CREATED).body(dto)` 또는 `ResponseEntity.created(uri).body(dto)`.
삭제의 204는 `ResponseEntity.noContent().build()` 입니다.
</details>

<details>
<summary>DTO는 어떻게 만드나요?</summary>

Java 17+의 `record`를 쓰면 간단합니다. 요청 DTO의 필드에 `@NotBlank`, `@Size`를 붙이고,
컨트롤러 파라미터에 `@Valid @RequestBody`를 붙이면 검증됩니다. (검증 실패 시 400이 자동 응답됩니다.)
</details>

<details>
<summary>수정 시 save()를 호출해야 하나요?</summary>

`@Transactional` 안에서 조회한 엔티티는 영속 상태라, 필드를 바꾸면 커밋 시점에 자동 UPDATE 됩니다.
단, 이때 `updatedAt`이 응답에 즉시 반영되길 원하면 `flush()`가 필요할 수 있습니다. 왜 그런지 생각해 보세요.
</details>

<details>
<summary>글 삭제 시 댓글은 어떻게 같이 지우나요?</summary>

방법은 두 가지입니다. 편한 쪽을 쓰세요.

1. **cascade**: `@OneToMany(mappedBy = "post", cascade = CascadeType.ALL)` 처럼 설정하면 글을 삭제할 때 댓글도 같이 삭제됩니다.
2. **직접 삭제**: `cascade`를 안 쓰고, 서비스에서 그 글의 댓글을 `commentRepository`로 먼저 지운 뒤 글을 지웁니다.
   (예: 글에 속한 댓글을 조회해서 `deleteAll`, 또는 `deleteByPostId` 같은 쿼리 메서드)
</details>

<details>
<summary>댓글 삭제는 어떻게?</summary>

가장 쉬운 방법은 `commentRepository.delete(comment)` 입니다.
(심화: `@OneToMany(orphanRemoval = true)`를 설정하면 글의 댓글 리스트에서 제거하는 것만으로도 DELETE가 나갑니다.
이 옵션은 몰라도 됩니다. 쓴다면 메모리의 리스트와 DB 상태가 어긋나지 않는지 생각해 보세요.)

그리고 삭제 전에 **그 댓글이 요청한 글(postId)에 속하는지** 꼭 확인하세요. (테스트 `다른_글의_댓글은_삭제할수_없다`)
</details>

<details>
<summary>404는 어떻게 만드나요?</summary>

`@ResponseStatus(HttpStatus.NOT_FOUND)`를 붙인 `RuntimeException` 하위 클래스를 만들어 던지면 됩니다.
</details>

## 9. 막혔을 때

1. 에러 메시지의 **첫 `Caused by`** 를 읽는다. (전체 스택트레이스를 다 읽을 필요 없음)
2. 실행 로그의 SQL(`show-sql`)로 어떤 쿼리가 나갔는지 확인한다. H2 콘솔에서 데이터를 직접 볼 수 있다.
3. 테스트 이름이 곧 요구사항이다. 실패한 테스트의 이름과 메시지를 먼저 본다.
4. 브라우저 F12 → Network 탭, 또는 `requests.http`로 API 응답을 직접 확인한다.
5. 그래도 안 되면 힌트(8장) → 팀원에게 질문 순으로.

## 10. 제출/리뷰

- `./gradlew test` 전체 통과 로그
- 스터디에서 서로의 코드를 리뷰합니다. 테스트가 못 잡는 **설계**를 아래 기준으로 확인합니다.

### 코드 리뷰 체크리스트

**도메인**
- [ ] 엔티티에 불필요한 public setter가 없고, 변경이 의미 있는 메서드(`update`, `addComment` 등)로 표현되는가
- [ ] 기본 생성자 접근 제한(`protected`) 등 엔티티 기본 규칙을 지켰는가
- [ ] 연관관계의 주인이 외래키를 가진 쪽(Comment.post)인가, 양방향 연관관계의 양쪽 값을 함께 맞췄는가
- [ ] 글 삭제 시 댓글 삭제 방식(`cascade` 또는 직접 삭제)을 선택한 이유를 설명할 수 있는가
- [ ] 생성/수정 시각을 직접 `new` 하지 않고 한 곳에서 일관되게 처리했는가

**계층 분리**
- [ ] Controller에 비즈니스 로직/Repository 직접 호출이 없는가
- [ ] Service에 `@Transactional`이 적절히 붙었는가 (조회는 `readOnly = true`)
- [ ] 요청/응답 DTO와 엔티티가 분리되어 있는가 (엔티티를 그대로 반환하지 않는가)

**REST**
- [ ] 상태 코드가 의도에 맞는가 (생성 201, 삭제 204, 없음 404, 검증 실패 400)
- [ ] URL이 리소스 중심이고 HTTP Method와 잘 맞는가
- [ ] 없는 리소스를 일관된 방식(예외 → 404)으로 처리하는가

**품질**
- [ ] 이름이 의도를 드러내는가 (메서드/변수명 일관성)
- [ ] 불필요한 주석/죽은 코드가 없는가
