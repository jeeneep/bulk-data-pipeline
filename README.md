# 💳 카드 데이터 대용량 처리 시스템

## 📌 프로젝트 개요

본 프로젝트는 약 530만 건의 카드 사용 데이터(CSV)를 데이터베이스에 안전하고 빠르게 적재하기 위한 백엔드 아키텍처 실습 프로젝트입니다. 대용량 트래픽 발생 시 서버의 과부하를 막고, 단일 장애 지점(SPOF) 및 DB 다운 상황에 대비하기 위해 **로드밸런싱과 메시지 큐를 활용한 분산 처리 시스템**을 구축했습니다.

## 🏛️ 시스템 아키텍처 (Architecture)

- **클라이언트 (Frontend):** JavaScript를 활용해 대용량 db를 CSV 파일로 분할하여 서버에 전송.
- **웹 서버 (Nginx):** 2대의 WAS(Tomcat)로 트래픽을 분산시키는 로드밸런서 역할. 대용량 파일 업로드 타임아웃 방지 설정 적용.
- **WAS (Tomcat 8080, 8090):** 멀티 서버 환경. 수신한 CSV 데이터를 스트림으로 읽어 파싱한 후 메시지 큐에 발행(Publish).
- **메시지 브로커 (RabbitMQ):** 대용량 DB I/O의 병목을 막아주는 완충재(Buffer) 역할.
- **데이터베이스 (MySQL in Docker):** Source(Master) - Replica(Slave) 구조로 고가용성 확보.

<img width="1683" height="287" alt="image" src="https://github.com/user-attachments/assets/e4624c43-6afa-4313-822c-2c8ac1a61692" />
## 🛠️계층 구조

### 1. Presentation Layer
클라이언트 요청을 수신하고, 트래픽을 분산하는 계층입니다.

- **csv_uploader.js**
    - 530만 건을 1,000건 단위로 분할하여 전송
    - `multipart/form-data` 방식 사용
        - → 서버에서 스트림 기반 처리 가능, 대용량 업로드에 적합
    - 로컬 CSV 파일을 읽어 → 1000개씩 모아 → 가상 CSV 형태로 생성 후 전송
        - → JSON 객체로 보관하지 않아 메모리 사용량 절감 및 전송 효율 향상
- **Nginx**
    - reverse-proxy
    - round-robin 로드밸런싱 → WAS1, WAS2로 트래픽 분산

### 2. Application Layer
비즈니스 로직 처리 및 비동기 분산 처리를 담당하는 계층입니다.

- **Session Clustering**
    - 다중 WAS 환경에서도 세션 불일치 문제 해결
    - 특정 WAS 장애 발생 시에도 세션 유지 가능 (장애 내성)
    - 톰캣의 기본 메모리 세션 저장 방식을 외부 Redis로 분리
        - → WAS 수평 확장 시에도 유연하게 대응 가능한 구조

- **RabbitMq**
    - 수신된 데이터를 파싱한 후 즉시 큐에 발행(Publish)
    - RabbitMQ가 시스템 다운 방지 역할을 하여 트래픽 폭주 시 DB에 직접적인 부하를 주지 않도록 설계
    - Batch Consumer - 큐에 적재된 데이터를 감시하며, 일정 건수(1000건) 단위로 데이터를 묶어 DB에 일괄 저장 → 따라서 단일 건당 발생하는 트랜잭션 오버헤드를 줄이고, 네트워크 왕복 시간을 줄임

### 3. Data Layer (고가용성 분산 DB)

데이터 적재와 조회를 분리하고, 장애 상황에서도 무중단 서비스를 유지하기 위한 계층입니다.

- **ProxySQL (DB Router & Connection Pooler)}**
  - 애플리케이션과 DB 사이의 트래픽 제어 미들웨어
  - Read/Write Split
    - INSERT → Master
    - SELECT → Replica 자동 라우팅
    - → 읽기/쓰기 트래픽 분리
  - Connection Pooling & Multiplexing
    - 다수의 클라이언트 연결을 프록시 단에서 수용
    - 실제 MySQL 연결 최소화
    - → MySQL OOM 및 커넥션 폭증 방지
  - Failover 대응
    - Master 장애 발생 시 쿼리 일시 대기(Hold)
    - 승격된 신규 Master로 안전하게 전달

- **MySQL HA Cluster**
  - 1 Master - 2 Replica 구조
  - Binary Log 기반 복제
  - 데이터 적재(쓰기)와 조회 트래픽(읽기) 물리적 분리

- **Orchestrator**
  - Master DB 상태 모니터링
  - 장애 감지 시 Replica 자동 승격(Auto-Failover)
  - 단일 장애 지점(SPOF) 제거

## 💻기술 스택

**Language & Runtime**

- Java / Node.js

**Server & Infrastructure**

- Nginx: 로드밸런싱
- Tomcat 9.0: 다중 WAS 구성 (8080, 8090)
- Redis: 세션 클러스터링
- RabbitMQ: 메시지 브로커 (Message Queue)
- ProxySQL: DB 트래픽 라우팅 및 Read/Write 자동 분리
- Orchestrator: DB 장애 감지 및 자동 승격(Auto-Failover) Database
- MySQL: Master-Slave 복제 구조 (1 Master - 2 Replica 고가용성 구성)ㄷ

**DevOps**

- Docker & Docker Compose: 인프라 컨테이너화 및 환경 세팅 자동화(IaC)

## 🛠 Troubleshooting

### 🛑 1. ProxySQL 초기화 자동화
- 문제: ProxySQL은 MySQL처럼 init script 자동 실행을 지원하지 않음
- 해결: docker-compose에 일회성 Setup 컨테이너 추가
- 결과: 완전 자동화된 IaC 환경 구축 성공

### 🛑 2. Log Rolling 실패 (Windows 환경)
- 문제: 로그 파일 Rename 실패
- 원인: Windows 파일 핸들 점유 이슈
- 해결: logback.xml의 <file> 태그 제거 후 fileNamePattern 기반 롤링만 사용

### 🛑 3. MySQL Binary Log 용량 증가
- 문제: 테스트 후 디스크 사용량 급증
- 원인: Binary Log 다량 생성
- 조치: 개발 환경용 보관 기간 설정 필요성 확인

## 📝 향후 개선 과제

- Nginx SPOF 문제
