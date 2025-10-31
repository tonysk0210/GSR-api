package com.hn2.cms.repository.aca2003;

import com.hn2.cms.dto.aca2003.Aca2003DetailView;
import com.hn2.cms.model.aca2003.AcaDrugUseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Aca2003Repository
 * <p>
 * 負責「毒品濫用資料表 (dbo.AcaDrugUse)」的查詢與輔助查核。
 * 設計原則：
 * 1) 讀多寫少：多採用投影 (Interface Projection) 以減少 Entity 轉換成本與欄位耦合。
 * 2) 軟刪除：所有查詢預設過濾掉 isDeleted=1（或只取 isDeleted=0/NULL）。
 * 3) JOIN ACABrd：為了顯示個案名稱/證號，使用 LEFT JOIN（ACABrd 對不到資料仍保留 AcaDrugUse）。
 * <p>
 * 注意：
 * - AcaDrugUse.IsDeleted 為 bit + nullable，業務上視 (0 or NULL) 為有效。
 */
public interface Aca2003Repository extends JpaRepository<AcaDrugUseEntity, Integer> {

    // ---------------------------------------------------------------------
    // save API（輔助查核與資料帶入）
    // ---------------------------------------------------------------------

    /**
     * 依 ACABrd 帶出分會代碼（CreatedByBranchID）
     * 用途：新增 AcaDrugUse 時自動填入 CreatedByBranchID。
     * <p>
     * 過濾條件：ACABrd.IsDeleted=0。
     */
    @Query(value = "SELECT TOP 1 CreatedByBranchID FROM dbo.ACABrd WHERE ACACardNo = :cardNo AND IsDeleted = 0", nativeQuery = true)
    String findCreatedByBranchIdByAcaCardNo(@Param("cardNo") String acaCardNo);

    /**
     * 回傳有效筆數：同 ACACardNo 且未刪除（isDeleted=0 或 NULL）
     * 用途：新增前檢查是否已有「有效重複」。
     * <p>
     * 注意：IsDeleted 為 Boolean wrapper（可為 NULL），故條件需寫成 (false or null)。
     */
    @Query("select count(a) from AcaDrugUseEntity a " +
            "where a.acaCardNo = :cardNo " +
            "and (a.isDeleted = false or a.isDeleted is null)")
    long countActive(@Param("cardNo") String cardNo);

    /**
     * 判斷是否存在有效的毒品濫用資料（IsDeleted、isERASE 皆須為 0 或 NULL）。
     *
     * @param cardNo ACACardNo
     * @return 1 表示存在有效資料，0 表示不存在
     */
    @Query(value = "SELECT CASE WHEN EXISTS (" +
            "  SELECT 1 FROM dbo.AcaDrugUse WITH (NOLOCK) " +
            "  WHERE ACACardNo = :cardNo " +
            "    AND (IsDeleted = 0 OR IsDeleted IS NULL) " +
            "    AND (isERASE = 0 OR isERASE IS NULL)" +
            ") THEN 1 ELSE 0 END", nativeQuery = true)
    int existsActiveByCardNo(@Param("cardNo") String cardNo);

    /**
     * ACABrd 是否存在且有效（IsDeleted=0）
     * 用途：save 時的應用層一致性檢查。
     */
    @Query(
            value = "SELECT CASE WHEN EXISTS (" +
                    "  SELECT 1 FROM dbo.ACABrd " +
                    "  WHERE ACACardNo = :cardNo AND IsDeleted = 0" +
                    ") THEN 1 ELSE 0 END",
            nativeQuery = true
    )
    int existsActiveAcaBrd(@Param("cardNo") String acaCardNo);

    // ---------------------------------------------------------------------
    // query API（投影查詢）
    // ---------------------------------------------------------------------

    /**
     * 依 AcaDrugUse.ID 取得詳情（含 ACABrd 顯示欄位）
     * <p>
     * 說明：
     * - LEFT JOIN ACABrd：若 ACABrd 對不到或已刪除（IsDeleted=1），仍保留 AcaDrugUse 資料。
     * - ACABrd.IsDeleted=0 條件放在 ON 子句，避免誤變 INNER JOIN。
     * - AcaDrugUse 的有效資料條件：d.IsDeleted=0 或 NULL。
     * - LEFT JOIN CaseManagementT.dbo.Lists：將 CreatedByBranchID 對應成分會顯示名稱（ParentID=26）。
     */
    @Query(value =
            "SELECT d.ID                 AS id, " +
                    "       d.CreatedOnDate      AS createdOnDate, " +
                    "       l.Text               AS createdByBranchName, " +
                    "       d.DrgUserText        AS drgUserText, " +
                    "       d.OprFamilyText      AS oprFamilyText, " +
                    "       d.OprFamilyCareText  AS oprFamilyCareText, " +
                    "       d.OprSupportText     AS oprSupportText, " +
                    "       d.OprContactText     AS oprContactText, " +
                    "       d.OprReferText       AS oprReferText, " +
                    "       d.Addr               AS addr, " +
                    "       d.OprAddr            AS oprAddr, " +
                    "       d.ACACardNo          AS acaCardNo, " +
                    "       b.ACAName            AS acaName, " +
                    "       b.ACAIDNo            AS acaIdNo " +
                    "FROM dbo.AcaDrugUse d " +
                    "LEFT JOIN dbo.ACABrd b ON b.ACACardNo = d.ACACardNo AND b.IsDeleted = 0 " +
                    "LEFT JOIN CaseManagementT.dbo.Lists l ON l.Value = d.CreatedByBranchID AND l.ParentID = 26 " + // 透過 Lists 取得分會顯示名稱
                    "WHERE d.ID = :id " +
                    "  AND (d.IsDeleted = 0 OR d.IsDeleted IS NULL) "
            , nativeQuery = true)
    Optional<Aca2003DetailView> findDetailById(@Param("id") Integer id);

    /**
     * 依 ACACardNo 取得「最新一筆」AcaDrugUse（以 ID 最大視為最新）
     * <p>
     * 說明：
     * - 常見需求：同一 ACACardNo 會有多筆紀錄，只需最新一筆給前端顯示。
     * - 以 ID 倒序搭配 TOP(1) 取得最新。
     * - 同樣過濾軟刪除（IsDeleted=0 或 NULL）。
     * - 與 findDetailById 相同，額外 JOIN Lists 將分會代碼轉換為顯示名稱。
     */
    @Query(value =
            "SELECT TOP 1 " +
                    "       d.ID                 AS id, " +
                    "       d.CreatedOnDate      AS createdOnDate, " +
                    "       l.Text               AS createdByBranchName, " +
                    "       d.DrgUserText        AS drgUserText, " +
                    "       d.OprFamilyText      AS oprFamilyText, " +
                    "       d.OprFamilyCareText  AS oprFamilyCareText, " +
                    "       d.OprSupportText     AS oprSupportText, " +
                    "       d.OprContactText     AS oprContactText, " +
                    "       d.OprReferText       AS oprReferText, " +
                    "       d.Addr               AS addr, " +
                    "       d.OprAddr            AS oprAddr, " +
                    "       d.ACACardNo          AS acaCardNo, " +
                    "       b.ACAName            AS acaName, " +
                    "       b.ACAIDNo            AS acaIdNo " +
                    "FROM dbo.AcaDrugUse d " +
                    "LEFT JOIN dbo.ACABrd b ON b.ACACardNo = d.ACACardNo AND b.IsDeleted = 0 " +
                    "LEFT JOIN CaseManagementT.dbo.Lists l ON l.Value = d.CreatedByBranchID AND l.ParentID = 26 " +
                    "WHERE d.ACACardNo = :cardNo " +
                    "  AND (d.IsDeleted = 0 OR d.IsDeleted IS NULL) " +
                    "ORDER BY d.ID DESC",
            nativeQuery = true)
    Optional<Aca2003DetailView> findLatestDetailByCardNo(@Param("cardNo") String cardNo);

}

