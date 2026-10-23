// 서버 API 호출 공통 함수 (제공). 수정하지 않아도 됩니다.
async function api(method, url, body) {
  const options = { method, headers: {} };
  if (body !== undefined) {
    options.headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(body);
  }
  const res = await fetch(url, options);
  let data = null;
  const text = await res.text();
  if (text) {
    try { data = JSON.parse(text); } catch (e) { data = null; }
  }
  if (!res.ok) {
    const err = new Error(errorMessage(res.status, data));
    err.status = res.status;
    throw err;
  }
  return data;
}

// 서버가 내려준 에러 JSON에서 사람이 읽을 메시지를 뽑는다.
// Spring 기본 400/404 응답(ProblemDetail의 detail)이나 직접 만든 { "message": "..." } 모두 지원.
function errorMessage(status, data) {
  if (data && data.message) return data.message;
  if (data && data.detail) return data.detail;
  if (status === 400) return '입력값을 확인하세요. (400 Bad Request)';
  if (status === 404) return '대상을 찾을 수 없습니다. (404 Not Found)';
  return '요청에 실패했습니다. (' + status + ')';
}

function formatDate(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  const p = (n) => String(n).padStart(2, '0');
  return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes());
}

function queryId() {
  return new URLSearchParams(location.search).get('id');
}

function showError(message) {
  const el = document.getElementById('error');
  el.textContent = message;
  el.hidden = !message;
}
