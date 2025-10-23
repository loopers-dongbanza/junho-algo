# 웹 기본 개념 정리

## HTTP와 HTTPS
HTTP(HyperText Transfer Protocol)는 애플리케이션 계층의 텍스트 기반 프로토콜로, 기본적으로 80번 포트를 사용해 요청과 응답을 교환합니다. HTTPS는 동일한 HTTP 메시지를 443번 포트에서 TLS(Transport Layer Security)로 감싸 전송하며, 서버 인증과 메시지 암호화·무결성 검증을 제공합니다. HTTPS를 사용하면 중간자 공격(MITM)에 대한 방어가 가능하고, 현대 브라우저는 로그인·결제 페이지에서 HTTPS를 강제합니다.

### TLS 암호화 흐름
1. 클라이언트가 `ClientHello` 메시지로 지원 가능한 프로토콜 버전과 암호 스위트를 알린다.
2. 서버는 `ServerHello`와 X.509 인증서를 보내 자신을 증명하고 사용할 암호 스위트를 선택한다.
3. 클라이언트는 인증서의 유효성(서명, 도메인, 만료)을 검사한 뒤, 공개키를 이용해 pre-master secret을 암호화하여 전달한다.
4. 양측은 동일한 pre-master secret에서 세션 키(대칭키)를 파생시키고, `Finished` 메시지를 교환해 암호화 채널을 확정한다.
5. 이후의 HTTP 메시지는 협상된 대칭키로 암호화되며, 무결성 검증을 위해 MAC 혹은 AEAD(GCM 등)를 사용한다.

TLS 1.3은 핸드셰이크 라운드트립을 1-RTT로 줄이고, 취약한 암호 스위트를 제거해 보안을 강화했습니다. 인증서는 보통 루트 → 중간 → 서버 인증서 순으로 체인이 구성되며, 중간 인증서 미제공 시 검증 실패가 발생할 수 있습니다. 세션 재사용(Session Resumption)과 0-RTT(재연결 시 요청 1개를 동시에 전송)는 지연을 줄이지만, 재전송 공격을 고려해 민감한 작업에는 주의해야 합니다. 최신 서버는 ALPN(Application-Layer Protocol Negotiation)으로 TLS 핸드셰이크 중 HTTP/2, HTTP/3와 같은 상위 프로토콜을 협상합니다.

보안을 유지하려면 HSTS(HTTP Strict Transport Security)를 사용해 HTTP로의 다운그레이드를 차단하고, 인증서 갱신 자동화(Let's Encrypt + ACME)와 OCSP Stapling을 통해 만료/폐기 상태를 빠르게 전파해야 합니다.

### TLS 세부 개념
- **키 교환**: TLS 1.3은 `ECDHE` 기반의 전방 비밀성(Forward Secrecy)을 강제합니다. 서버는 `ServerHello`에서 선택한 곡선 정보를 제공하고, 클라이언트는 공유 비밀을 계산합니다.
- **Cipher Suite**: `TLS_AES_128_GCM_SHA256`처럼 대칭 암호, 모드, 해시 함수를 지정합니다. TLS 1.3은 AEAD 모드만 허용하며, CBC 기반 스위트는 제거되었습니다.
- **Certificate Pinning**: 모바일 앱이나 기업 내 시스템에서 특정 공개키 해시를 저장해 중간자 공격을 더욱 강하게 차단합니다. HPKP는 운영 부담으로 폐기되었지만, 앱 내부 핀닝은 여전히 사용됩니다.
- **Certificate Transparency(CT)**: 모든 인증서 발급 기록이 공개 로그에 저장되며, 브라우저는 CT 로그 SCT 서명을 요구합니다. 로그 모니터링으로 위조/오발급을 탐지합니다.
- **Encrypted ClientHello(ECH)**: SNI 노출을 막기 위한 초안 기술로, 프런트 도메인(ESNI 키)을 통해 실제 도메인을 암호화해 전송합니다.

HTTP/2는 단일 TCP 연결 위에서 스트림 다중화, 헤더 압축(HPACK)을 제공해 지연을 줄입니다. HTTP/3는 QUIC(UDP 기반)를 사용하여 헤드-오브-라인 블로킹을 완화하고, 커넥션 이동성을 지원합니다.

### HTTP/2와 HTTP/3 심화
- **헤더 압축**: HPACK은 정적·동적 테이블을 유지해 헤더를 압축합니다. 압축 상태는 연결 단위로 공유되므로, CRIME/BREACH와 같은 압축 기반 공격을 막기 위해 허프먼 코딩과 에어 갭을 활용합니다.
- **스트림 우선순위**: HTTP/2는 트리 구조 우선순위를 제공하지만, 실제 브라우저는 단순 가중치 모델로 구현하는 경우가 많습니다. HTTP/3는 가중치 기반 단일 레벨 우선순위로 간소화되었습니다.
- **QUIC 핸드셰이크**: QUIC은 TLS 1.3을 내장하며, 커넥션 ID로 NAT 재연결 및 로밍 중에도 연결 유지가 가능합니다. 패킷 손실은 개별 스트림만 재전송하고, 패킷 번호는 암호화되어 스니핑이 어렵습니다.
- **0-RTT 데이터**: HTTP/3의 0-RTT는 재연결 성능을 높이지만 재전송 공격(replay attack)에 취약합니다. 결제/변경 요청처럼 멱등성이 없는 작업에는 사용하지 않아야 합니다.

## HTTP 메시지 구조
HTTP 메시지는 스타트라인(요청: `METHOD URI VERSION`, 응답: `VERSION STATUS REASON`), 하나 이상의 헤더, 그리고 선택적인 바디로 구성됩니다. 헤더는 `키: 값` 형식이며, 빈 줄로 헤더와 바디를 구분합니다.

```http
GET /search?q=web HTTP/1.1
Host: example.com
User-Agent: Mozilla/5.0
Accept: text/html

```

- **요청 헤더**: `Accept`, `Accept-Language`, `Content-Type`, `Authorization`, `Cookie`, `If-None-Match` 등.
- **응답 헤더**: `Content-Type`, `Cache-Control`, `Set-Cookie`, `ETag`, `Location`(리다이렉트), `Strict-Transport-Security` 등.
- **본문**: `Content-Length` 혹은 `Transfer-Encoding: chunked`로 길이를 표현하며, JSON, HTML, 바이너리 등 다양한 형태가 올 수 있습니다.

콘텐츠 협상(Content Negotiation)은 `Accept`, `Accept-Language`, `Accept-Encoding` 헤더로 표현 형식을 제안하고, 서버는 `Content-Type`, `Content-Language`, `Content-Encoding`으로 응답합니다. 대용량 전송은 `Transfer-Encoding: chunked` 혹은 HTTP/2의 데이터 프레임으로 스트리밍할 수 있으며, 서버푸시(HTTP/2 Server Push)는 더 이상 브라우저에서 널리 지원되지 않습니다.

헤더는 대소문자를 구분하지 않지만, HTTP/2 이상에서는 정규화되어 전송됩니다. 사용자 정의 헤더는 접두사 `X-`를 쓰지 않는 경향이 늘어났으며, 대신 접두사 없이 명확한 의미를 담은 이름을 사용합니다.

### HTTP 헤더 심화
- **General Header**: `Date`, `Connection`, `Transfer-Encoding`과 같은 일반 헤더는 요청/응답에 모두 사용됩니다. HTTP/2 이상에서는 `Connection` 헤더를 사용하지 않으며, 커넥션 제어는 프레임 레벨에서 처리됩니다.
- **Entity Tag**: `ETag`는 Strong(`"abc123"`)과 Weak(`W/"abc123"`)으로 구분됩니다. 약한 ETag는 콘텐츠가 의미적으로 동일하면 동일하다고 판단해, 리소스 압축 방식이 달라도 캐시 적중을 허용합니다.
- **Vary**: 캐시가 어떤 요청 헤더를 고려해야 하는지 지정합니다. `Vary: Accept-Encoding, Origin`은 CDN이나 브라우저가 인코딩 방식과 출처별로 별도 엔트리를 유지하도록 강제합니다.
- **Hop-by-Hop vs End-to-End**: `Proxy-Authorization`, `Keep-Alive` 등은 프록시 한 홉에서만 사용되는 헤더로, 중간 프록시가 다음 홉으로 전달하지 않습니다. `Cache-Control`, `Content-Type`은 엔드투엔드 헤더입니다.
- **트레일러(Trailer)**: `Transfer-Encoding: chunked` 전송 중에 후행 헤더를 추가해 콘텐츠 생성 이후 계산된 `Digest`, `Signature` 값을 전달할 수 있습니다.

## 주요 HTTP 메서드
- **GET**: 리소스 조회. 멱등성(idempotent)과 안전성(safe)을 기대하며, 쿼리스트링으로 파라미터를 전달합니다.
- **POST**: 리소스 생성 또는 프로세스 트리거. 멱등성이 없고 요청 본문에 데이터를 담습니다.
- **PUT**: 전체 리소스 교체. 멱등성을 가지며, 클라이언트가 리소스 전체 표현을 전송합니다.
- **PATCH**: 부분 수정. 멱등성은 구현에 따라 다르며, 변경점만 전송합니다.
- **DELETE**: 리소스 삭제. 멱등성은 기대되지만 실제 구현에서 삭제 대신 소프트 삭제를 할 수 있습니다.
- **OPTIONS/HEAD**: 기능 탐색 및 헤더만 요청하는 보조 메서드입니다.

HTTP 사양은 메서드의 멱등성과 안전성을 보장하지 않지만, API 설계 단계에서 이를 준수해야 클라이언트가 재시도 정책을 세울 수 있습니다. 예를 들어 네트워크 오류로 인한 `PUT` 재시도는 문제가 없어야 하며, `POST`는 멱등성 키(Idempotency-Key)를 도입해 중복 생성을 막기도 합니다. `HEAD`는 본문 없는 GET으로 캐시 메타데이터만 확인할 수 있고, `OPTIONS`는 CORS 프리플라이트뿐 아니라 지원 메서드 탐색에도 활용됩니다. HTTP 사양에는 `TRACE`와 `CONNECT`도 정의되어 있지만, 보안상 대부분의 서버는 비활성화합니다.

### 멱등성과 재시도 전략
- **클라이언트 재전송**: 네트워크 타임아웃이나 5xx 발생 시 재시도 정책을 설계하며, `GET`, `HEAD`, `PUT`, `DELETE`는 기본적으로 재시도가 가능하도록 구현합니다.
- **Idempotency-Key**: Stripe, AWS API처럼 `POST` 요청 헤더에 키를 실어 서버가 이전 요청과 중복 여부를 판별하게 합니다. 키는 일정 기간 캐시해야 하며, TTL 이후엔 중복 처리가 허용됩니다.
- **분산 트랜잭션**: 여러 서비스가 얽힌 작업은 `saga` 패턴이나 아웃박스 패턴을 사용해 멱등성을 확보합니다.
- **안전성(Safe)**: `GET`은 상태 변화가 없어야 브라우저/프록시가 프리패치(pre-fetch)를 안전하게 실행할 수 있으며, 이를 위반하면 캐시가 상태를 망가뜨리는 버그가 발생합니다.

## HTTP 상태 코드
- **1xx (Informational)**: 처리 중임을 알리는 임시 응답, 예) `100 Continue`.
- **2xx (Success)**: 요청 성공. `200 OK`, `201 Created`, `204 No Content` 등.
- **3xx (Redirection)**: 추가 동작 필요. `301 Moved Permanently`, `302 Found`, `304 Not Modified` 등.
- **4xx (Client Error)**: 클라이언트 오류. `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`.
- **5xx (Server Error)**: 서버 오류. `500 Internal Server Error`, `502 Bad Gateway`, `503 Service Unavailable`.

세부적으로는 부분 콘텐츠 전송 시 `206 Partial Content`, 영구 리다이렉트에는 `308 Permanent Redirect`(본문 유지), 일시적 리다이렉트에는 `307 Temporary Redirect`(메서드 유지)를 사용합니다. 클라이언트 오류 영역에서는 유효성 검증 실패에 `422 Unprocessable Entity`, 레이트 리밋 초과에 `429 Too Many Requests`, 인증 토큰 갱신 요구에 `401` 혹은 `419`(커스텀) 등 컨벤션이 존재합니다. 서버 오류는 모니터링 경고를 트리거하므로, 장애 상황에서는 `503 Service Unavailable`과 `Retry-After` 헤더를 함께 반환해 재시도 타이밍을 안내하는 것이 좋습니다.

### 상태 코드 활용 팁
- **조건부 요청**: 사전 조건 실패 시 `412 Precondition Failed`, 엔티티 태그 충돌 시 `409 Conflict`를 사용해 클라이언트가 덮어쓰기를 방지할 수 있습니다.
- **캐시 협업**: `304 Not Modified`는 반드시 `ETag` 또는 `Last-Modified`와 함께 사용되며, 응답에 `Date`, `Cache-Control`, `Expires`를 재전송해야 캐시가 정책을 갱신합니다.
- **클라이언트 힌트**: `429 Too Many Requests`와 함께 `Retry-After`를 보내거나, `503`과 함께 대체 노드 주소를 `Alt-Svc`로 제공할 수 있습니다.
- **API 버전 종료**: 사용 중단 예정인 엔드포인트에는 `299`(Proposed) 같은 비표준 코드 대신 `Deprecation` 헤더, `Sunset` 헤더를 사용해 호환성을 유지합니다.

## HTTP/HTTPS 동작 과정
1. **DNS 조회**: 브라우저가 URL의 도메인을 IP로 변환한다.
2. **TCP 연결**: 3-way 핸드셰이크(`SYN` → `SYN-ACK` → `ACK`)로 신뢰성 있는 연결을 만든다.
3. **TLS 핸드셰이크(HTTPS)**: 인증서 검증, 키 교환, 암호화 채널 확립.
4. **HTTP 메시지 교환**: 요청과 응답을 교환하고, HTTP/2·HTTP/3는 다중화를 통해 지연을 줄인다.
5. **연결 유지/종료**: `Connection: keep-alive`로 재사용하거나, 필요 시 `Connection: close`로 종료한다. HTTP/2 이상은 하나의 연결에서 여러 스트림을 관리한다.

TCP는 전송 제어를 위해 슬로 스타트(Slow Start), 혼잡 회피(Congestion Avoidance)를 수행하므로, 작은 객체라도 초기 혼잡 윈도 크기가 성능에 영향을 줍니다. TLS는 세션 재개(세션 ID, 세션 티켓)로 핸드셰이크 비용을 줄일 수 있고, HTTP/3는 QUIC 위에서 동작해 커넥션이 손실되더라도 스트림이 독립적으로 회복됩니다. 브라우저는 프리커넥트(`<link rel="preconnect">`)로 DNS, TCP, TLS 단계를 미리 수행하거나, 프리로드(`<link rel="preload">`)로 중요한 리소스를 사전에 요청해 체감 속도를 높입니다.

### 연결 최적화와 모니터링
- **HOL Blocking**: HTTP/1.1 파이프라이닝은 헤드-오브-라인 블로킹을 완전히 해결하지 못해, HTTP/2 다중화 도입이 필수가 되었습니다.
- **TCP Fast Open**: 클라이언트가 쿠키를 보유하면 SYN 단계에서 데이터를 전송할 수 있지만, 중간 장비가 차단하는 경우가 있어 실험적으로 적용합니다.
- **Observability**: `Server-Timing` 헤더, `PerformanceObserver` API를 통해 백엔드 처리 시간, 캐시 적중 여부를 노출해 프런트엔드가 병목을 파악할 수 있습니다.
- **RUM과 합성 모니터링**: 실사용자 모니터링(Real User Monitoring)과 WebPageTest 같은 합성 테스트를 병행하면 배포 후 성능 저하를 조기에 감지합니다.

## 브라우저 캐싱과 조건부 요청
- **Cache-Control**: `max-age`, `no-cache`, `no-store`, `public`, `private` 등 캐시 정책을 정의합니다.
- **ETag & If-None-Match**: 리소스 버전 식별자(해시)를 통해 변경 여부를 판단합니다. 캐시된 버전이 최신이면 `304 Not Modified`를 반환합니다.
- **Last-Modified & If-Modified-Since**: 수정 시각 기반 조건부 요청입니다.
- **Service Worker**: PWA에서 네트워크/캐시 전략을 코드로 제어할 수 있는 브라우저 스크립트입니다.

`no-cache`는 반드시 재검증을 의미하며, `no-store`는 저장 자체를 금지합니다. `immutable` 지시자는 파일 이름에 해시를 포함(예: `app.3f1a.js`)했을 때 브라우저가 만료 전 재검증을 생략하도록 돕습니다. 프런트엔드 빌드 파이프라인은 정적 리소스에 `Cache-Control: public, max-age=31536000, immutable`을 붙이고, HTML에는 `no-cache`를 붙여 새 빌드를 감지하게 만듭니다.

조건부 요청은 헤더 조합에 따라 서버에서 `200`과 `304` 사이를 전환하며, CDN은 `ETag`/`Last-Modified`가 다르면 캐시를 무효화합니다. `stale-while-revalidate`와 `stale-if-error` 지시자를 사용하면 만료된 콘텐츠를 임시로 제공하거나, 백엔드 장애 시 캐시된 응답으로 서비스 연속성을 확보할 수 있습니다. 서비스 워커는 `network first`, `cache first`, `stale-while-revalidate` 등 전략을 코드로 구현해 오프라인 경험을 향상시킵니다.

### 캐시 무효화 전략
- **파일 지문(Fingerprint)**: 빌드 시 파일 이름에 해시를 붙여(`app.3f1a.js`) 오래된 캐시를 자동 무효화합니다. HTML은 짧은 TTL로 유지해 새 리소스 경로를 전달합니다.
- **서버 사이드 캐시 버전**: 백엔드는 `Cache-Control`과 별도로 버전 파라미터나 `Surrogate-Key` 헤더를 사용해 CDN에서 특정 그룹의 객체를 일괄 갱신할 수 있습니다.
- **Negative Caching**: HTTP 오류 응답도 TTL 동안 캐시될 수 있으므로, `Cache-Control: no-store`를 설정해 장애 복구 후 바로 정상 응답을 반환하도록 합니다.
- **캐시 파편화(Cache Fragmentation)**: `Vary` 헤더를 과도하게 사용하면 캐시 적중률이 떨어지므로, 필요한 헤더만 포함하도록 설계해야 합니다.

### CDN 캐시 동작
- **Surrogate-Control**: CDN 전용 헤더로 엣지 캐시 TTL을 지정하며, 오리진에는 짧은 TTL을 유지할 수 있습니다.
- **Shielding**: 상위 캐시 레이어(Shield POP)가 오리진 요청을 통합해 캐시 적중률을 높입니다.
- **캐시 키 구성**: 경로, 쿼리, 헤더 조합을 커스터마이즈해 동적 콘텐츠도 캐싱할 수 있으나, 인증 헤더는 키에서 제외해야 합니다.
- **Signed URL/쿠키**: 프라이빗 콘텐츠는 URL에 서명이나 스토리지 토큰을 부여해 엣지 캐시에서 인증합니다.

## CORS와 동일 출처 정책
동일 출처 정책(Same-Origin Policy)은 프로토콜, 호스트, 포트가 모두 일치하지 않으면 스크립트가 응답을 읽지 못하도록 제한합니다. CORS(Cross-Origin Resource Sharing)는 서버가 특정 출처에서의 접근을 허용하도록 명시하는 메커니즘입니다.

- **Simple Request**: `GET/HEAD/POST`와 제한된 헤더만 사용하는 요청은 사전 검사 없이 진행되며, 응답의 `Access-Control-Allow-Origin`이 조건을 만족해야 브라우저가 데이터를 제공한다.
- **Preflight Request**: `PUT`, `DELETE`, 커스텀 헤더 등 민감한 요청은 브라우저가 먼저 `OPTIONS` 메서드로 허용 여부를 확인한다. 서버는 `Access-Control-Allow-Methods`, `Access-Control-Allow-Headers`, `Access-Control-Max-Age` 등을 응답해야 한다.
- **Credentialed Request**: 쿠키·인증 정보를 포함하려면 `withCredentials=true`와 `Access-Control-Allow-Credentials: true`가 필요하며, 이때 `Access-Control-Allow-Origin`에는 와일드카드 `*`를 사용할 수 없습니다.

프리플라이트 응답은 `Access-Control-Max-Age`로 캐싱할 수 있으나, 브라우저마다 최대 허용 시간이 다릅니다. 실제 응답에서 커스텀 헤더를 읽으려면 `Access-Control-Expose-Headers`로 노출 목록을 지정해야 하며, 미지정 시 기본 안전 헤더만 접근 가능합니다. 또한 CORS는 서버 응답이 허용하는지 여부만 제어할 뿐, CSRF를 막아주지 않으므로 별도의 토큰·SameSite 정책이 필요합니다.

### CORS 실전 포인트
- **Preflight 예시**: 브라우저가 `OPTIONS /api/notes`를 전송하고, `Access-Control-Request-Method: PUT`, `Access-Control-Request-Headers: Content-Type`를 포함합니다. 서버는 `HTTP/1.1 204`, `Access-Control-Allow-Methods: GET,PUT`, `Access-Control-Allow-Headers: Content-Type`, `Access-Control-Allow-Origin: https://example.com`으로 응답해야 합니다.
- **Vary 헤더**: `Access-Control-Allow-Origin` 값이 요청에 따라 달라지면 반드시 `Vary: Origin`을 설정해 캐시 간 오염을 방지합니다.
- **Private Network Access**: 로컬 네트워크 리소스를 호출할 경우 최신 브라우저는 `Access-Control-Allow-Private-Network: true`를 요구합니다.
- **Redirect와 CORS**: 리다이렉트 응답은 최종 리소스의 CORS 정책을 따르며, 중간 단계가 301이라도 최종 응답이 허용하지 않으면 실패합니다.

## 쿠키(Cookie)와 세션(Session)
쿠키는 클라이언트에 저장되는 최대 4KB 정도의 키-값 쌍이며, 서버가 `Set-Cookie` 헤더로 내려보냅니다. 쿠키에는 다음과 같은 속성이 있습니다.
- `Expires`/`Max-Age`: 만료 시각. 미지정 시 세션 쿠키(브라우저 종료 시 삭제)입니다.
- `Domain`/`Path`: 전송할 도메인과 경로 범위를 제한합니다.
- `Secure`: HTTPS 연결에서만 전송합니다.
- `HttpOnly`: JavaScript(`document.cookie`)에서 접근을 막아 XSS 위험을 줄입니다.
- `SameSite`: `Strict`, `Lax`, `None`으로 크로스 사이트 요청에 쿠키를 동반할지 제어합니다.

세션은 서버가 사용자별 상태를 저장하는 데이터 구조이며, 세션 ID를 쿠키나 URL 토큰으로 전달해 식별합니다. 세션 저장소는 메모리, 데이터베이스, Redis 등으로 구현하며, 만료 정책과 동시 로그인을 관리해야 합니다. 토큰 기반 인증(JWT 등)은 서버 세션 상태를 줄이기 위해 사용되기도 합니다.

세션 보안 관점에서 세션 고정(Session Fixation)과 하이재킹을 막기 위해 로그인 직후 세션 ID를 회전시키고, 민감한 액션마다 재인증을 요구할 수 있습니다. JWT는 자체적으로 만료와 서명을 포함하지만, 무효화가 어려워 리프레시 토큰 전략이나 블랙리스트를 병행해야 합니다. 브라우저 저장소 선택 시, `localStorage`는 XSS에 취약하므로 인증 토큰은 가능한 한 `HttpOnly` 쿠키로 관리하고, CSRF 토큰이나 Double Submit Cookie 패턴을 통해 요청 위조를 차단합니다.

### SameSite 규칙 이해하기
- **Lax 기본값**: 최신 브라우저는 `SameSite=Lax`를 기본 적용하므로, 크로스 사이트 폼 제출은 허용되지만 `GET`을 제외한 대부분의 크로스 사이트 요청은 쿠키가 제외됩니다.
- **SameSite=None**: 서드파티 컨텍스트에서 쿠키를 사용하려면 `SameSite=None; Secure`를 함께 설정해야 하며, HTTPS가 아닌 경우 브라우저가 쿠키를 거부합니다.
- **Strict**: 다른 사이트에서 열리는 링크 클릭조차 쿠키를 보내지 않으므로, 민감한 관리 페이지나 금융 서비스에 사용합니다.

### 토큰 수명과 회전 전략
- **Access Token**: 짧은 만료 시간(분 단위)을 유지해 탈취 피해를 최소화합니다. 서버는 만료 시간이 남았더라도 토큰 목록을 캐시해 블랙리스트 검증을 빠르게 수행합니다.
- **Refresh Token**: 장기 수명을 가지며, 탈취 대응을 위해 1회 사용 후 회전(Rotate)하고, 이전 토큰 재사용 시 계정 잠금을 고려합니다.
- **키 회전(Key Rotation)**: JWT 서명 키는 JWKS 엔드포인트로 공개하며, 키가 교체될 때 구 키를 일정 기간 유지해 배포 간 불일치를 방지합니다.

## DNS
DNS(Domain Name System)는 계층 구조를 가진 분산 데이터베이스입니다.
- **재귀 리졸버(Resolver)**: ISP나 OS가 제공하며, 클라이언트 대신 전체 탐색을 수행한다.
- **루트 네임서버**: 최상위 도메인(TLD) 서버 위치를 알려준다.
- **TLD 네임서버**: `.com`, `.kr` 등 특정 최상위 도메인의 권한 있는 네임서버 정보를 제공한다.
- **권한 있는 네임서버(Authoritative)**: 최종적으로 A/AAAA/CNAME 등 레코드를 응답한다.

브라우저와 OS는 TTL(Time To Live) 동안 응답을 캐싱해 반복 조회 비용을 줄입니다. DNS over HTTPS/ TLS(DoH/DoT)는 DNS 질의 자체를 암호화하여 감청을 막습니다.

일반적인 레코드 타입에는 IPv4 주소용 `A`, IPv6 주소용 `AAAA`, 별칭을 만드는 `CNAME`, 메일 서버용 `MX`, 도메인 소유 검증과 SPF, DKIM 정보를 담는 `TXT`가 있습니다. CDN은 글로벌 Anycast IP와 짧은 TTL을 사용해 사용자와 가장 가까운 엣지 서버를 반환합니다. DNSSEC은 응답에 서명을 추가해 위조를 방지하지만, 모든 리졸버가 지원하지는 않습니다. 대규모 서비스는 헬스체크 기반 가중치 라우팅, 지리적 라우팅(GeoDNS)을 조합해 고가용성과 성능을 확보합니다.

### DNS 고급 주제
- **Negative TTL**: 존재하지 않는 레코드(NXDOMAIN)도 TTL 동안 캐시되어 도메인 생성 직후 전파가 지연될 수 있습니다.
- **Zone Transfer(AXFR/IXFR)**: 권한 있는 네임서버 간 존 데이터를 동기화하며, 접근 제어가 느슨하면 DNS 정보가 외부로 유출될 수 있습니다.
- **Split Horizon DNS**: 내부/외부 사용자에게 다른 IP를 제공해 네트워크 경계를 구분합니다.
- **DoH/DoT**: DNS over HTTPS/TLS를 사용하면 중간자 감청을 방지할 수 있지만, 기업 환경에서는 보안 모니터링을 위해 트래픽 가시성이 줄어듭니다.
- **EDNS Client Subnet(ECS)**: 리졸버가 클라이언트 IP 프리픽스를 오리진에게 전달해 더 적절한 CDN 노드를 반환하게 하지만, 개인 정보 유출 가능성이 있어 제한적으로 사용됩니다.

## REST와 RESTful API 설계
REST(Representational State Transfer)는 자원(Resource)을 URI로 식별하고, 표현(Representation)을 전송하는 아키텍처 스타일입니다. 주요 제약 조건은 다음과 같습니다.
- **클라이언트-서버 구조**: 관심사를 분리해 확장성과 유연성을 확보합니다.
- **무상태성(Stateless)**: 각 요청은 필요한 모든 정보를 포함하며, 서버는 세션 상태를 저장하지 않습니다.
- **캐시 가능(Cacheable)**: 응답에 캐시 가능 여부를 명시해야 합니다.
- **균일한 인터페이스(Uniform Interface)**: 표준 메서드와 상태 코드를 활용해 일관된 API를 제공합니다.
- **계층화(Layered System)**: 프록시, 로드밸런서 등 중간 계층이 있어도 클라이언트는 이를 의식하지 않아야 합니다.
- **HATEOAS**: 응답이 다음 행동으로 이어지는 링크를 포함할 수 있습니다.

실무에서는 다음과 같은 설계 원칙을 따릅니다.
- URI는 명사형 복수 사용(`GET /users/123/orders`).
- 컬렉션과 단일 자원을 구분(`GET /products`, `GET /products/42`).
- 상태 코드를 일관되게 매핑하고, 에러 응답에는 코드와 메시지를 포함한다.
- 버전 관리는 URL(`/v1/`) 혹은 헤더(`Accept: application/vnd.example.v1+json`)로 명시한다.
- 필요한 경우 쿼리 파라미터로 필터링, 페이징, 정렬을 제공한다.

API 문서화는 OpenAPI(Swagger) 스펙을 사용하면 자동화된 스키마 검증과 코드 생성을 지원합니다. 페이징은 `offset`/`limit`, 커서 기반(cursor-based) 등 비즈니스에 맞는 전략을 선택하고, 대량 처리에는 배치 엔드포인트를 고려합니다. 에러 응답 포맷은 표준화(`code`, `message`, `details`)해 클라이언트가 UI에 일관되게 표시할 수 있도록 합니다. 결제나 주문과 같이 중복 처리가 문제가 되는 도메인은 `Idempotency-Key` 헤더를 받아 서버 측에서 재실행을 필터링합니다. API 보안을 위해 인증(세션, JWT, OAuth2)과 인가(Role-Based Access Control, Attribute-Based Access Control)를 명확히 분리하고, 속도 제한(rate limiting), 감사 로그를 적용합니다.

하이퍼미디어(HATEOAS)는 리소스를 탐색할 수 있는 링크(`_links`)와 가능한 액션 정보를 제공해 API 버전을 업그레이드해도 클라이언트가 동적으로 기능을 발견하도록 돕습니다. 실무에선 완전한 하이퍼미디어 구현이 드물지만, 최소한 페이지네이션 링크(`next`, `prev`) 제공으로 사용성을 높일 수 있습니다.

### API 디자인 심화
- **Partial Response**: `fields` 파라미터나 GraphQL과 유사한 Projection 기능으로 전송량을 줄입니다.
- **Bulk API**: 대량 작업 시 여러 요청을 한 번에 처리할 수 있는 `/bulk` 엔드포인트를 제공하지만, 부분 실패 처리와 트랜잭션을 명확히 정의해야 합니다.
- **Rate Limit 헤더**: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `Retry-After`를 제공하면 클라이언트가 자가 조절할 수 있습니다.
- **API Versioning**: 브라우저 캐시 영향이 큰 경우 URL 버저닝이 명확하며, 마이크로서비스 환경에서는 헤더 버저닝 + 게이트웨이 라우팅을 조합합니다.

### API 게이트웨이와 백엔드 연동
- **Gateway**: 인증, 라우팅, 캐싱, 변환(Protocol Translation)을 담당하며, Kong, NGINX, Envoy 등이 사용됩니다.
- **Backends for Frontends(BFF)**: 모바일/웹 클라이언트별 맞춤 API 층을 두어 전송량과 출시 속도를 최적화합니다.
- **GraphQL/REST 혼용**: GraphQL을 도입하더라도 핵심 웹훅이나 퍼블릭 API는 REST로 유지해 단순한 통합을 제공합니다.

### 테스트 및 모니터링
- **Contract Test**: 소비자 주도 계약 테스트(Consumer-Driven Contract)로 클라이언트와 서버의 스키마 일관성을 검증합니다.
- **Synthetic Monitor**: Postman, k6 등을 이용해 SLA를 확인하고, 분산 트레이싱(OpenTelemetry)으로 요청 흐름을 시각화합니다.
- **Dark Launch**: 새 API를 그림자 환경에 배포해 실제 트래픽을 복제하고, 로그를 비교해 문제를 조기에 발견합니다.

---

## 웹 보안 기초
- **XSS(Cross-Site Scripting)**: 사용자 입력을 반드시 이스케이프하고, 템플릿 엔진의 자동 이스케이프 기능을 활용합니다. CSP(Content-Security-Policy)로 스크립트 출처를 제한하면 XSS 피해를 줄일 수 있습니다.
- **CSRF(Cross-Site Request Forgery)**: 동일 출처 정책을 우회하는 공격으로, `SameSite=Lax/Strict`, CSRF 토큰, Double Submit Cookie, 사용자 재인증 등을 조합해 방어합니다.
- **Clickjacking**: `X-Frame-Options: DENY` 또는 `Content-Security-Policy: frame-ancestors`로 외부 도메인이 iframe으로 페이지를 감싸지 못하게 합니다.
- **보안 헤더**: `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`, `Referrer-Policy`, `Permissions-Policy` 등을 적극 적용해 공격 면을 줄입니다.
- **입력 검증**: 서버는 클라이언트 입력을 신뢰하지 않고, 화이트리스트 기반 검증과 출력 컨텍스트별 이스케이프를 수행해야 합니다.

### XSS 심화
- **저장형/반사형/DOM 기반**: 저장형은 DB에 저장된 악성 스크립트가 다른 사용자에게 전달될 때 발생하고, 반사형은 즉시 응답에 포함됩니다. DOM 기반은 클라이언트에서 DOM 조작 시 발생하므로 CSP와 DOMPurify 같은 라이브러리로 방어합니다.
- **CSP 설정**: `default-src 'self'; script-src 'self' 'nonce-xyz'; object-src 'none'`처럼 기본 정책을 엄격히 설정하고, 인라인 스크립트는 Nonce 기반으로 허용합니다.
- **Trusted Types**: 최신 브라우저의 Trusted Types를 사용하면 DOM API에 안전한 인풋만 전달하도록 강제할 수 있습니다.

### CSRF 방어 심화
- **Double Submit Cookie**: 쿠키와 요청 파라미터/헤더에 동일한 토큰을 전송해 교차 검증합니다.
- **SameSite 조합**: `SameSite=Lax`는 대부분의 GET 요청을 차단하지만, OAuth 리디렉션과 같은 특별한 시나리오에선 `Lax+POST` 예외를 고려해야 합니다.
- **Origin 검증**: API 서버는 `Origin` 혹은 `Referer` 헤더를 검사해 합법적인 도메인인지 확인합니다.

### 취약점 관리
- **SAST/DAST**: 정적 분석(SAST)과 동적 스캐닝(DAST)을 CI 파이프라인에 포함해 코드/런타임 취약점을 자동 탐지합니다.
- **Bug Bounty/공격 시뮬레이션**: 포상 프로그램과 침투 테스트로 자동화 도구가 놓치는 리스크를 보완합니다.
- **비밀 관리**: 환경 변수나 Secret Manager를 사용하고, 깃 커밋에 비밀이 유출되면 즉시 키를 회전합니다.

## 인증과 인가
- **세션 기반 인증**: 서버가 세션 저장소를 유지하며, 확장 시 세션을 공유 저장소(예: Redis)로 이동하거나 스티키 세션을 사용합니다.
- **토큰 기반 인증**: JWT, PASETO 등 서명 토큰으로 상태를 클라이언트에 위임합니다. 만료 관리, 키 회전, 블랙리스트 전략이 필요합니다.
- **OAuth 2.0**: 리소스 소유자 대신 서드파티 클라이언트가 접근하도록 허용하는 프레임워크입니다. Authorization Code + PKCE가 모바일/SPA에서 권장되며, Implicit Flow는 보안상 거의 사용하지 않습니다.
- **OpenID Connect**: OAuth 2.0 위에 사용자 인증 정보를 표준화한 프로토콜로, ID Token을 통해 사용자 프로필을 전달합니다.
- **인가 모델**: RBAC(Role-Based), ABAC(Attribute-Based), PBAC(Policy-Based) 등 시스템 규모와 도메인에 맞는 정책을 설계합니다.

### OAuth 플로우 상세
- **Authorization Code with PKCE**: SPA/모바일 앱은 `code_verifier`를 생성해 `code_challenge`로 전송합니다. 토큰 교환 시 `code_verifier`를 검증해 Authorization Code 탈취를 막습니다.
- **Client Credentials**: 서버-투-서버 통신에서 사용하며, 사용자 컨텍스트가 없으므로 권한을 최소화한 기기/서비스 계정을 발급합니다.
- **Device Authorization Grant**: TV 장치 등 입력이 불편한 환경에서 `user_code`, `verification_uri`를 통해 사용자 인증을 위임합니다.
- **Refresh Token Rotation**: OAuth 2.1 초안은 모든 새 요청에 새 리프레시 토큰을 발급하고, 이전 토큰 재사용 시 차단하도록 권장합니다.

### 인가 정책 설계
- **RBAC 한계**: 단순 역할 체계는 권한 폭발(Role Explosion)을 야기하므로 속성 기반 접근제어(ABAC)나 정책기반(PBAC)으로 확장합니다.
- **OPA/Policy Engine**: Open Policy Agent(OPA)나 Cedar와 같은 정책 엔진으로 `allow/deny`를 코드와 분리해 관리합니다.
- **Contextual Access**: 위치, 디바이스, 시간 등을 고려한 조건부 접근(Conditional Access)을 통해 Zero Trust 모델을 구현합니다.

### 인증 인프라 운영
- **SSO/SAML**: 기업 환경에선 SAML 2.0 기반 SSO로 기존 디렉터리(AD, LDAP)와 연동합니다.
- **MFA/U2F**: 다중 인증(MFA)와 FIDO2/U2F 보안키를 도입하면 피싱 저항성을 크게 높일 수 있습니다.
- **세션 단축**: 민감 정보 접근 시 재인증을 요구하고, 로그아웃 혹은 토큰 폐기 시 즉시 유효성을 검증하기 위해 백채널 로그아웃을 구현합니다.

## 웹 성능 최적화
- **프론트엔드 최적화**: 번들 분할, 코드 스플리팅, 이미지 최적화(WebP/AVIF), 지연 로딩(Lazy Loading)을 적용합니다.
- **네트워크 최적화**: HTTP/2 다중화, HTTP/3(QUIC), TCP Fast Open, CDN 캐시 활용으로 초기 지연을 줄입니다.
- **압축**: `gzip`, `brotli` 압축으로 텍스트 리소스 크기를 줄이고, `Content-Encoding` 헤더로 알고리즘을 명시합니다.
- **지표 모니터링**: LCP(Largest Contentful Paint), FID(First Input Delay), CLS(Cumulative Layout Shift)와 같은 Core Web Vitals를 추적해 사용자 경험을 정량화합니다.
- **백엔드 최적화**: DB 쿼리 튜닝, 캐시 계층(Redis, Memcached), 비동기 처리(큐)로 응답 시간을 개선합니다.

### 렌더링 파이프라인 이해
- **Critical Rendering Path**: HTML 파싱 → DOM 생성 → CSSOM 생성 → Render Tree → Layout → Paint 순으로 진행되며, CSS 차단 리소스를 최소화하면 렌더링을 빠르게 시작할 수 있습니다.
- **Preload/Prefetch**: `<link rel="preload">`로 폰트·핵심 JS를 미리 가져오고, `<link rel="prefetch">`로 다음 페이지 리소스를 선제 로드합니다.
- **Priority Hints**: `<img fetchpriority="high">`, `<link rel="preload" as="image" imagesrcset=...>`로 브라우저 우선순위를 조정합니다.

### 서버 성능과 확장성
- **캐시 계층**: 애플리케이션 캐시(메모리), 분산 캐시(Redis), CDN을 계층화해 오리진 부하를 줄입니다.
- **Connection Pool**: DB 및 외부 API와의 연결 풀 설정을 모니터링하고, 서킷 브레이커/버섯(Bulkhead) 패턴으로 연쇄 장애를 차단합니다.
- **Edge Compute**: CDN의 Workers/Functions를 사용해 사용자 가까운 곳에서 인증·AB 테스트·A/B 랜딩 페이지를 처리합니다.

### 성능 모니터링 및 자동화
- **CI 성능 테스트**: Lighthouse CI, WebPageTest API를 연동해 빌드마다 성능 리포트를 생성합니다.
- **Budget 설정**: 번들 크기, LCP, TTFB 등 성능 버짓을 정의하고 초과 시 파이프라인을 실패시켜 품질을 유지합니다.
- **Synthetic vs RUM**: 합성 지표는 제어된 환경을 제공하고, RUM은 실제 사용자 환경을 커버합니다. 두 데이터를 결합해 최적화 우선순위를 정합니다.

## 리버스 프록시와 로드 밸런싱
- **Reverse Proxy**: NGINX, HAProxy, Envoy 등은 TLS 종료, 캐싱, 요청 라우팅을 담당합니다. L7 프록시는 URL, 헤더 기반 라우팅을 지원하며, L4 로드 밸런서는 TCP/UDP 수준에서 트래픽을 분산합니다.
- **로드 밸런싱 알고리즘**: 라운드 로빈, 가중치, 최소 연결 수, IP 해시 등 워크로드 특성에 맞는 전략을 선택합니다.
- **헬스 체크**: `HTTP 200` 체크뿐 아니라 응답 시간, 커스텀 상태 엔드포인트(`/healthz`, `/readyz`)를 사용해 노드 상태를 정교하게 판단합니다.
- **서비스 메시**: Istio, Linkerd는 mTLS, 관찰성, 트래픽 제어를 통합 제공하며, 마이크로서비스의 네트워크 계층 일관성을 유지합니다.

## 로깅과 관찰성
- **구조화 로그**: JSON 로그로 필드를 표준화하면 ELK, Loki, Cloud Logging에서 질의가 용이합니다.
- **분산 추적**: OpenTelemetry로 트레이스를 수집하고, TraceID를 로그/메트릭과 연계해 병목 구간을 찾습니다.
- **메트릭**: RED(Request rate, Errors, Duration), USE(Utilization, Saturation, Errors) 같은 메트릭 모델을 적용합니다.
- **에러 예산과 SLO**: 서비스 수준 목표(SLO)를 정의하고, 에러 예산이 소진되면 기능 배포 대신 안정화에 집중합니다.

## 네트워크 디버깅 도구
- **브라우저 DevTools**: Network 패널에서 HTTP/2 스트림, 타임라인, CORS 에러 등을 확인하고, Coverage 탭으로 사용되지 않는 자산을 분석합니다.
- **curl/httpie**: 헤더 조작, TLS 핸드셰이크 정보(`curl -v --tlsv1.3 --ciphersuites`)를 확인할 수 있습니다.
- **Postman/Insomnia**: 인증 토큰, 환경변수 관리와 함께 API 테스트 자동화를 지원합니다.
- **tcpdump/Wireshark**: 패킷 레벨 캡처로 TLS 핸드셰이크, DNS 질의, TCP 재전송을 분석합니다.
- **mitmproxy/Burp Suite**: HTTPS 트래픽을 중간에서 가로채 분석하며, CORS, 인증 흐름을 시뮬레이션합니다.

## 추가 학습 체크리스트
- TLS 1.3 핸드셰이크 패킷 캡쳐를 Wireshark로 분석해보기
- 브라우저 DevTools의 Network 탭에서 캐시 동작과 헤더 값 관찰하기
- curl 혹은 Postman으로 CORS 프리플라이트 시나리오 재현하기
- JWT를 발급·검증하는 간단한 서버/클라이언트 작성해보기
- Lighthouse 혹은 WebPageTest로 성능 측정 후 개선 아이디어 실행
- Envoy/NGINX로 리버스 프록시를 구성하고 헬스 체크, 로드 밸런싱 정책 실험하기
- OpenTelemetry SDK를 적용해 분산 트레이스와 메트릭을 수집하고 대시보드 구축하기
- OAuth 2.0 Authorization Code + PKCE 플로우를 직접 구현하고 토큰 회전 시나리오 테스트하기
- CDN 사업자(Cloudflare/Akamai 등) 무료 플랜을 사용해 엣지 캐시, Workers 활용법 실습하기
- Core Web Vitals를 기준으로 성능 버짓을 정의하고 CI 파이프라인에서 자동 검증 세팅하기

---

필요한 주제가 더 있다면 같은 디렉터리에 문서를 추가하거나, 본 문서를 이어서 확장해 보세요.
