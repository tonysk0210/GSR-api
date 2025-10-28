# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Language & Communication

**所有回覆必須使用繁體中文**。保留技術術語原文（如 Controller、Service、Repository、DTO、Payload、bean、IoC）。對每個新建或修改的程式碼，必須提供：
1. **變更摘要（繁體中文）**：說明修改目的、影響範圍、設計取捨與可能副作用
2. **中文註解（內嵌程式碼）**：在關鍵邏輯、非直覺判斷、錯誤處理、邊界條件處加入繁體中文註解，重點在「為何這麼做」
3. **JavaDoc 註解（繁體中文）**：方法與類別層級註解描述用途、參數、回傳與可能例外
4. **測試說明（繁體中文）**：若新增/更新測試，說明測試情境與覆蓋行為

## Project Overview

Spring Boot 2.7.14 多模組專案（Java 11），使用 Gradle 管理依賴與建置。為「個案暨輔導員管理系統」後台 API，資料庫為 MS SQL Server，使用 JPA/Hibernate、Sql2o、Log4jdbc。

### Module Structure

- **hnsquare-app-api**: 產製 WAR 檔的入口模組，包含 REST Controller、ApiApplication 主類別、環境設定檔（application-{dev,uat,prod}.properties）
- **hnsquare-base**: 基礎元件，包含核心 Model、核心 Controller、Handler、Listener、共用 DTO、工具類別
- **hnsquare-cms**: 業務模組，包含個案管理（Case Management）的 Controller、Service、Repository、DTO、Payload、Model
- **hnsquare-config**: 設定元件，包含 Spring Configuration Bean、ApplicationConfig、Swagger 配置
- **hnsquare-utility**: 工具元件，包含跨模組共用的 Utility 類別
- **hnsquare-report**: 報表模組，使用 JasperReports 產生報表
- **hnsquare-evaluation**: 評估模組，處理評估與測量相關邏輯

**依賴關係**：hnsquare-app-api 依賴所有其他模組；其他模組間避免循環依賴。

## Build & Development Commands

```bash
# 完整建置與測試所有模組
./gradlew clean build

# 僅執行測試（不重建）
./gradlew test

# 執行特定模組測試
./gradlew :hnsquare-cms:test

# 本地啟動 API（使用預設 profile）
./gradlew hnsquare-app-api:bootRun

# 產生環境別 WAR 檔（會設定 spring.profiles.active）
./gradlew hnsquare-app-api:dev    # 開發環境 api.war
./gradlew hnsquare-app-api:uat    # UAT 環境 api.war
./gradlew hnsquare-app-api:prod   # 正式環境 api.war

# 診斷測試失敗
./gradlew test --info
```

**重要**：產生 WAR 時，Gradle 任務會自動修改 `application.properties` 中的 `spring.profiles.active`。

## Naming Conventions

### Package 命名
- 依模組：`com.hn2.mail`（郵件）、`com.hn2.cms`（個案管理）
- 依功能代號：`com.hn2.md`（醫材）

### Class 命名規則
- **Controller**: 依功能代號或模組命名，例如 `Mdreg1000Controller`、`FlowController`
- **Service**: 依功能代號或 Model 命名，例如 `Mdreg1000Service`、`RegBaseService`（對應 RegBase Model）
- **Repository**: 依 Model 命名，例如 `RegBaseRepository`；客製化 Native Query 時依功能代號命名，例如 `Mdreg1000CustomerRepository`
- **Payload**: 依功能代號或模組命名，例如 `Mdreg1000Payload`、`FlowPayload`
- **DTO**: 依功能代號或模組命名，例如 `Mdreg1000Dto`、`FlowDto`

### Coding Style
- 4 空格縮排，使用 Lombok 減少 boilerplate（`@Data`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor`）
- 優先使用 Constructor Injection，不使用 `@Autowired` field injection
- REST 層使用 Spring Stereotype（`@RestController`、`@Service`、`@Repository`）

## Data Mapping

- 資料庫 `datetime2(3)` → Java `LocalDateTime`
- 資料庫 `decimal()` → Java `BigDecimal`
- 日期格式：後端使用西元年格式；民國年轉換由前端處理
- 必須輸入欄位檢核：使用 `@Valid` + `@NotNull`/`@NotBlank` 等 Bean Validation

## Controller & API Design

- URL 依程式代號或模組命名，詳細規格依 SD 文件
- 使用 Swagger 文件化（`@Api`、`@ApiOperation`、`@ApiModel`、`@ApiModelProperty`）
- Controller、DTO、Payload 必須加上 Swagger 註解

## Configuration & Security

- 環境設定檔位於 `hnsquare-app-api/src/main/resources/`
  - `application.properties`：主設定檔，包含共用設定
  - `application-dev.properties`：開發環境
  - `application-uat.properties`：UAT 環境
  - `application-prod.properties`：正式環境
- 敏感資訊使用 `jasypt-spring-boot-starter` 加密，**絕不可將明文密碼提交至 Git**
- 資料庫連線使用 Log4jdbc 包裝：`jdbc:log4jdbc:sqlserver://...`

## Testing

- 使用 JUnit 5 + Spring Boot Test（`@SpringBootTest`）
- 測試檔案命名為 `*Test.java`，放置於 `src/test/java`，mirror 對應的 production package
- 重點測試：Payload 驗證、SQL Adapter、Jasper Report Builder、跨模組整合
- 執行測試時加 `--info` 可診斷環境相關問題

## Git & Commit Guidelines

- 提交訊息使用簡潔的現在式動詞，例如 `add logic comments to all methods`、`update cms validation flow`、`mod erase/restore api for AcaDrugUse`
- 必要時註明影響的模組或功能代號
- PR 必須描述：變更範圍、風險、部署計畫，並附上 API 變更的範例 Payload 或截圖

## Key Dependencies & Tools

- Spring Boot 2.7.14（Web、JPA、Validation、Mail、Cache、Thymeleaf）
- MS SQL Server 9.4.1.jre11
- Sql2o 1.6.0 + SimpleFlatMapper
- ModelMapper 2.4.4
- Swagger/Springfox 3.0.0
- JWT (jjwt 0.11.2)
- Jasypt 3.0.3（加密）
- JasperReports 6.17.0
- EhCache（`@EnableCaching`）
- Apache HttpClient 5.2.1
- Log4jdbc 1.2

## Common Development Workflows

**新增 API 端點（例如新增個案資料）**:
1. 在 `hnsquare-cms` 或 `hnsquare-base` 中定義或確認 Model（`@Entity`）
2. 建立 Repository（繼承 `JpaRepository` 或使用 Sql2o 客製查詢）
3. 建立 Payload（輸入驗證用，加上 `@Valid` 註解）
4. 建立 DTO（回傳資料用）
5. 建立 Service（業務邏輯、資料轉換使用 ModelMapper）
6. 在 `hnsquare-app-api` 或 `hnsquare-cms` 的 Controller 新增端點，加上 Swagger 註解
7. 撰寫 Unit Test 或 Integration Test
8. 執行 `./gradlew test` 確認測試通過

**修改既有 API**:
1. 確認影響範圍（Model、Repository、Service、Controller、DTO、Payload）
2. 更新相關類別與測試
3. 確認 Swagger 文件更新
4. 執行測試並檢查副作用

**環境部署**:
1. 確認目標環境設定檔（`application-{dev,uat,prod}.properties`）已正確配置
2. 執行 `./gradlew hnsquare-app-api:{dev|uat|prod}` 產生 WAR
3. 部署 `hnsquare-app-api/build/libs/api.war` 至對應環境
