# 🔧Servlet에서 Spring Bean(DI) 구조로 리팩토링 정리

`dev.sample` 패키지 기준으로  
**Servlet 내부에 있던 비즈니스 로직을 Spring Bean으로 분리하고 DI 형태로 사용하는 구조**로 변경한 내용 정리입니다.

---

## ⚙️ 1. Spring 설정

**파일** : src/main/resources/applicationContext.xml

```xml
<context:component-scan base-package="dev.sample" />
```
### component-scan 역할

dev.sample 및 하위 패키지를 스캔하여
@Service, @Component, @Repository 등 어노테이션이 붙은 클래스를
Spring Bean으로 자동 등록합니다.

## 📦 2. 등록된 Service Bean
| 클래스 | 패키지 | 어노테이션 | 역할 |
|--------|--------|------------|------|
| `LoginService` | `dev.sample.service` | `@Service` | 로그인 ID 검증 (test1/test2 → TEST1/TEST2/INVALID) |
| `AuthService` | `dev.sample.service` | `@Service` | 세션 기반 인증 정보 조회 (JSON용 데이터 구성) |
| `CsvUploadService` | `dev.sample.service` | `@Service` | CSV 업로드·파싱·메시지 큐 발행 |

기존에는 이 로직이 Servlet 내부에 직접 구현되어 있었지만
현재는 Service Bean으로 분리하여 Servlet은 요청 처리, Service는 비즈니스 로직으로 역할이 분리되었습니다.

##🔌3. Servlet에서 Bean 사용하는 방식

Servlet은 Spring이 관리하는 객체가 아니기 때문에 @Autowired를 사용할 수 없습니다.

따라서 Servlet 초기화 시점(init())에 ApplicationContext를 생성하고
getBean()으로 Service Bean을 조회하여 사용합니다.

### 공통 패턴

1. **init()에서 Spring 컨테이너 생성**

```java
ClassPathXmlApplicationContext context =
    new ClassPathXmlApplicationContext("applicationContext.xml");
```

2. 필요한 Bean 조회
```java
LoginService loginService = context.getBean(LoginService.class);
```
3. 요청 처리 시 Service 호출

doGet() / doPost()에서는 해당 Bean 메서드만 호출

## 🧩 4. Servlet ↔ Service 구조
| Servlet             | URL           | 사용하는 Bean          | 역할         |
| ------------------- | ------------- | ------------------ | ---------- |
| `LoginServlet`      | `/login`      | `LoginService`     | 로그인 처리     |
| `AuthCheckServlet`  | `/auth`       | `AuthService`      | 세션 인증 확인   |
| `DataUploadServlet` | `/api/upload` | `CsvUploadService` | CSV 업로드 처리 |

Client -> Servlet (HTTP 처리) -> Service (비즈니스 로직)

## ⚠️ 5. 현재 구조의 특징

현재 구조에서는 각 Servlet이 init()에서 직접 Spring 컨테이너를 생성합니다.
```java
new ClassPathXmlApplicationContext("applicationContext.xml")
```
Servlet마다 별도의 Spring 컨테이너가 생성되어, 컨테이너가 하나 뜰 때마다 설정 파일 로딩, component-scan, bean 생성 과정이 반복됩니다.

따라서 컨테이너 초기화 비용이 반복되고 메모리 사용량이 증가하는 단점이 있습니다.


## 🚀 6. 개선 가능한 구조

spring-web 라이브러리의 ContextLoaderListener를 사용하면

애플리케이션 시작 시 Spring 컨테이너를 한 번만 생성하고

모든 Servlet이 동일한 WebApplicationContext 공유하는 구조로 만들 수 있습니다.

현재 프로젝트는 spring-web 없이 구현했기 때문에
Servlet에서 ClassPathXmlApplicationContext를 직접 생성하는 방식으로 동작합니다.