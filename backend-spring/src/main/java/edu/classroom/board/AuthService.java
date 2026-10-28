package edu.classroom.board;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HexFormat;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  // JJWT 의존성은 pom.xml에 포함되어 있습니다. app.jwt.* 설정은 application.properties 참조.
  private static final String EXAMPLE_SECRET = "REPLACE_WITH_RANDOM_64_HEX_CHARACTERS"; // 예시 키

  private final SecretKey key; // 비밀 키
  private final String issuer; // iss 값
  private final String audience; // aud 값
  private final int ttl;         // 토큰 유효 시간 초단위
  private final Users users;     // 회원의 객체를 가져옴

  public AuthService(             // 생성자
          @Value("${app.jwt.secret}") String secret,     // 환경변수의 비밀 키 값 JWT_SECRET
          @Value("${app.jwt.issuer}") String issuer,     // "webservice-class" JWT_ISSUER
          @Value("${app.jwt.audience}") String audience, // "board-api" JWT_AUDIENCE
          @Value("${app.jwt.ttl}") int ttl,              // 90초 TOKEN_TTL
          Users users) {                                 // 회원 명단
    if (secret == null                                    // 비밀 키가 비어있나?
            || !secret.matches("[0-9a-fA-F]{64}")         // 0~9, A~F로 된 64글자가 아닌가?
            || secret.equalsIgnoreCase(EXAMPLE_SECRET)) { // 예시 키 인가?
      throw new IllegalStateException("JWT_SECRET에 새로 생성한 64자리 16진수를 설정하세요."); // 셋 중 하나라도 맞다면 오류 출력
    }
    if (ttl <= 0) throw new IllegalStateException("TOKEN_TTL은 1 이상이어야 합니다."); // 토큰 만료 시간이 0초 이하이면 오류 출력

    // 위에서 설정한 private 변수에 값을 넣음
    this.key = Keys.hmacShaKeyFor(HexFormat.of().parseHex(secret));
    // 비밀 키 16진수 64문자열을 32바이트로 바꿈(한글자 씩 4비트를 두글자씩 1바이트로)
    this.issuer = issuer;                                           // "webservice-class"
    this.audience = audience;                                       // "board-api"
    this.ttl = ttl;                                                 // 90초 TOKEN_TTL
    this.users = users;                                             // 회원 명단
  }

  public String issueToken(Users.PublicUser user) {         // User자바 파일의 PublicUser class, 로그인한 회원
    // TODO BE-01: 서버 키로 HS256 JWT 발급. sub, exp, iss, aud 필수.
    long now = System.currentTimeMillis();                  // 지금 시각
    return Jwts.builder()                                   // 토큰 만들기 도구
            .subject(String.valueOf(user.id()))             // sub, 토큰의 주인 (숫자에서 문자열로 바꿈)
            .issuer(issuer)                                 // iss , 발급한 곳 "webservice-class"
            .audience().add(audience).and()                 // aud, 사용처 "board-api"
            .issuedAt(new Date(now))                        // iat, 발급시간은 지금(밀리초)
            .expiration(new Date(now + ttl * 1000L))        // exp 만료시간은 지금 시간 + 90초
            .signWith(key, Jwts.SIG.HS256)                  // 비밀 키와 HS256 방식으로 서명을 만듦
            .compact();                                     // 한 줄 문자열로 만듦
  }

  public long currentUserId(HttpServletRequest request) {   // request가 HTTP요청을 받음
    // TODO BE-02: Bearer 추출 → 서명/알고리즘/만료/클레임 검증 → 실제 사용자 확인.
    // 완성 후 아래 데모 ID 반환을 제거하세요. 토큰 없음/변조/만료는 401입니다.
    String header = request.getHeader("Authorization");         // HTTP의 Authorization 헤더를 꺼내서 header에 줌
    if (header == null || !header.startsWith("Bearer ")) {      // header가 null이거나 "Bearer "로 시작하지 않을 시
      throw new ApiError(401, "TOKEN_REQUIRED", "로그인이 필요합니다."); // 401 TOKEN_REQUIRED 응답코드
    }
    String token = header.substring(7).trim();                  // header의 앞 7글자 "Bearer "를 떼어내고 토큰을 넘김
    if (token.isEmpty()) {                                      // 만약 토큰이 없다면
      throw new ApiError(401, "TOKEN_REQUIRED", "로그인이 필요합니다."); // 401 TOKEN_REQUIRED 응답코드
    }

    Claims claims;               // 토큰의 내용을 담는 타입과 이름
    try {
      Jws<Claims> jws =                          // 검사를 통과한 토큰을 담을 곳
              Jwts.parser()                      // 검사 도구
                      .verifyWith(key)           // 비밀 키로 서명 확인
                      .requireIssuer(issuer)     // iss가 "webservice-class"인지 확인
                      .requireAudience(audience) // aud가 "board-api"인지 확인
                      .build()                   // 검사기 완성
                      .parseSignedClaims(token); // 토큰 검사 실행 (만료도 여기서 확인)
      if (!"HS256".equals(jws.getHeader().getAlgorithm())) {                  // 토큰 헤더의 alg가 HS256이 아니면
        throw new ApiError(401, "INVALID_TOKEN", "허용되지 않은 알고리즘입니다."); // 401 INVALID_TOKEN 응답코드
      }
      claims = jws.getPayload();                 // 검사를 통과한 토큰의 내용을 가져옴
    } catch (ExpiredJwtException e) {             // 토큰이 만료이면
      throw new ApiError(401, "TOKEN_EXPIRED", "로그인이 만료되었습니다. 다시 로그인하세요."); // 401 TOKEN_EXPIRED 응답코드
    } catch (JwtException | IllegalArgumentException e) { // 서명 위조, iss/aud의 값이 다르거나 토큰의 형식이 이상하면
      throw new ApiError(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다.");   // 401 INVALID_TOKEN 응답코드
    }

    String sub = claims.getSubject();                                                   // 토큰의 sub를 꺼냄
    if (claims.getExpiration() == null || sub == null || !sub.matches("[1-9][0-9]{0,15}")) {
      // exp가 없거나, sub가 없거나, 첫자리는 1부터 9까지의 수이고 그 뒤로 0에서 9까지의 숫자를 가지고 최소 0부터 최대 15개의 숫자가 아닌가
      throw new ApiError(401, "INVALID_TOKEN", "유효하지 않은 토큰입니다."); // 401 INVALID_TOKEN 응답코드
    }
    long userId = Long.parseLong(sub);                    // 문자열 sub를 정수로 바꿈
    if (users.find(userId) == null) {                     // 회원 명단에 번호가 없으면
      throw new ApiError(401, "INVALID_TOKEN", "사용자를 확인할 수 없습니다."); // 401 INVALID_TOKEN 응답코드
    }
    return userId; // 유저 번호를 반환 (모든 검사를 통과함)
  }

  public void assertOwner(long userId, long authorId) {     // 요청한 유저 번호, 글쓴 유저 번호
    // TODO BE-03: 다른 작성자이면 403 FORBIDDEN. 현재는 검사하지 않습니다.
    if (userId != authorId) {                               // 요청한 유저의 번호와 글쓴 유저의 번호가 다르다면
      throw new ApiError(403, "FORBIDDEN", "본인이 작성한 글만 수정·삭제할 수 있습니다."); // 403 FORBIDDEN 응답코드
    }
  }
}