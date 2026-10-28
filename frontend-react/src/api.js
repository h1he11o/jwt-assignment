// TODO FE-01~04: 공통 API 명세와 구현 체크리스트를 보고 완성하세요.
let session = { accessToken: "", user: null };
// 로그인 정보를 담는 상자, 이름은 session
export const getSession = () => session;
// 다른 파일에서도 쓸 수 있게 session 상자를 돌려줌
export function clearSession() {
  session = { accessToken: "", user: null }; // session 상자를 처음 상태로 되돌림
}
function authorizationHeaders() {
  // TODO FE-02: 메모리에 보관한 토큰을 Bearer 헤더로 보내세요.
  if (!session.accessToken) return {}; // 토큰이 없으면 빈 상자를 반환
  return { Authorization: `Bearer ${session.accessToken}` };
  // 토큰이 존재하면 Authorization 헤더를 만들어서 반환
}
export async function request(path, options = {}) {
  // 보낼 주소 뒷부분을 넘김, 방식과 내용을 (넘김 안넘기면 빈상자로 넘김)
  const response = await fetch(`/api${path}`, {
    ...options,
    // 서버에 요청을 보내고 답장이 올 때까지 기다림
    // ...은 상자 안의 내용을 꺼내서 넣는다는 뜻 ...options은 받은 method와 body를 그대로 넣음
    headers: {
      "Content-Type": "application/json",
      ...authorizationHeaders(),
      ...options.headers,
    },
    // 내용은 JSON, ...authorizationHeaders()로 토큰 헤더를 넣고
    // options에 추가 헤더가 있으면 그것도 넣음
  });
  const data = response.status === 204 ? null : await response.json();
  // 204(삭제 성공)이면 data는 null, 아니면 response를 JSON으로 꺼냄
  if (!response.ok) { // 실패하면 (400, 401...)
    // TODO FE-03: 401이면 메모리의 로그인 상태도 해제하세요.
    if (response.status === 401) clearSession(); // 401이면 로그아웃
    const error = new Error(
        // 오류 문구 만들기 message 있으면 쓰고 message없으면 code를 쓰고 code도 없으면 HTTP를 씀
      data.message || data.code || `HTTP ${response.status}`,
    );
    error.status = response.status; // 응답 코드 적기(코드 구분)
    error.code = data.code;        // 이유 적기
    throw error;                   // 오류 던지기
  }
  return data;   // 성공하면 반환
}
export async function login(email, password) {
  // TODO FE-01: POST /auth/login → 응답의 토큰과 사용자 정보를 session에 저장.
  // request()를 이용할 수 있습니다. 성공하면 user를 반환하세요.
  const data = await request("/auth/login", {
    // 서버에 로그인 요청을 보내고 기다림 (주소는 /api/auth/login)
    method: "POST",        // 로그인 기능이라 요청방식은 POST
    body: JSON.stringify({ email, password }), // 보낼 내용으로 이메일과 비밀번호를 JSON글자로 보냄
  }); // 실패하면 에러, 성공하면 다음 줄
  session = { accessToken: data.accessToken, user: data.user };
  // data의 토큰과 사용자를 session 상자에 저장
  return data.user; // 사용자 정보 반환
}
export function canEdit(post) {
  // TODO FE-04: 로그인 사용자와 작성자를 비교하세요. 서버 검사도 반드시 필요합니다.
  // 시작 코드에서는 CRUD를 체험할 수 있도록 모두 표시합니다.
  return !!session.user && !!post && post.authorId === session.user.id;
  // !!는 null이면 false, 객체면 true로 바꿈
  // 로그인을 했고, 글이 있고, 글쓴이의 번호가 내 번호와 같으면 true 반환
}
export const api = {
  list: () => request("/posts"),           // GET 목록
  detail: (id) => request(`/posts/${id}`), // GET id
  create: (input) =>                            // POST
    request("/posts", { method: "POST", body: JSON.stringify(input) }),
  update: (id, input) =>                        // PUT
    request(`/posts/${id}`, { method: "PUT", body: JSON.stringify(input) }),
  remove: (id) => request(`/posts/${id}`, { method: "DELETE" }), // DELETE
  me: () => request("/me"),               // GET 내 정보
};
