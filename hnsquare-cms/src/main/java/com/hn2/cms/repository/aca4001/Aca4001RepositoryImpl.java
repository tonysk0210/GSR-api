package com.hn2.cms.repository.aca4001;

import com.hn2.cms.dto.aca4001.Aca4001AuditQueryDto;
import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto;
import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto.CrmRec;
import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto.ProRec;
import com.hn2.cms.dto.aca4001.Aca4001EraseQueryDto.PersonBirth;
import com.hn2.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hn2.util.DateUtil.DateFormat.yyyMMdd_slash;

@Repository
@RequiredArgsConstructor
public class Aca4001RepositoryImpl implements Aca4001Repository {

    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate npJdbc;
    private final org.sql2o.Sql2o sql2o;

    /*eraseQuery API*/

    /**
     * 依 ACACardNo 查詢個案生日與滿 18 歲起算日（當天 00:00，以 LocalDate 表示）。
     * - 查無資料：回傳 null
     * - 查到資料：回傳 PersonBirth；其中 birthDate / eighteenthStart 可能為 null（若 ACABirth 為 null）
     *
     * @param acaCardNo 個案卡號（呼叫端應先做 null/blank 驗證）
     * @return PersonBirth 或 null（查無資料）
     */
    @Override
    public PersonBirth findPersonBirth(String acaCardNo) {
        String sql = "SELECT TOP 1 " +
                "  CAST(ACABirth AS date) AS BirthDate, " + // 生日（只留日期）
                "  CASE WHEN ACABirth IS NOT NULL " +
                "       THEN DATEADD(YEAR, 18, CAST(ACABirth AS date)) " + // 18 歲當日（日期型別）
                "       ELSE NULL END AS EighteenthStart " +
                "FROM dbo.ACABrd " +
                "WHERE ACACardNo = ? AND IsDeleted = 0";
        return jdbc.query(sql, ps -> ps.setString(1, acaCardNo), (ResultSet rs) -> {
            if (!rs.next()) return null;
            var birthSql = rs.getDate("BirthDate");
            var e18Sql = rs.getDate("EighteenthStart");

            PersonBirth pb = new PersonBirth();
            pb.setBirthDate(birthSql == null ? null : birthSql.toLocalDate());
            pb.setEighteenthStart(e18Sql == null ? null : e18Sql.toLocalDate());
            return pb;
        });
    }

    /**
     * 依個案卡號查詢「滿 18 歲當日 00:00 以前」的 ProRec.ID 清單，
     * 並可選用 [startTs, endExclusive] 的時間區間過濾 ProNoticeDate。
     * 條件：
     * - IsDeleted = 0
     * - ProNoticeDate < eighteenthStart          (嚴格小於 18 歲當日，排除滿 18 當天)
     * 回傳：
     * - 依 ProNoticeDate 由早到晚排序的 ID 清單
     *
     * @param acaCardNo       個案卡號
     * @param eighteenthStart 滿 18 歲當日 00:00（上界排除；僅取此之前）
     * @param startTs         選用：起始時間（含）
     * @param endInclusive    選用：結束時間（含）
     * @return 符合條件的 ProRec.ID 清單
     */
    @Override
    public List<String> findProRecIdsBefore18(String acaCardNo, LocalDateTime eighteenthStart, LocalDateTime startTs, LocalDateTime endInclusive) {
        String sql = "SELECT PR.ID " +
                "FROM dbo.ProRec PR " +
                "WHERE PR.IsDeleted = 0 " +  // 僅未刪除資料
                "AND PR.ACACardNo = ? " +    // 指定個案卡號
                "AND PR.ProNoticeDate  < ? " +  // 嚴格小於 18 歲日（排除當天）
                "AND (? IS NULL OR PR.ProNoticeDate  >= ?) " + // 若有起始：ProNoticeDate >= startTs
                "AND (? IS NULL OR PR.ProNoticeDate  <= ?) " + // 若有結束：ProNoticeDate <= endExclusive（※ 實作為「含」）
                "ORDER BY PR.ProNoticeDate "; // 由早到晚排序
        return jdbc.query(sql, ps -> {
            ps.setString(1, acaCardNo);
            ps.setObject(2, eighteenthStart);
            ps.setObject(3, startTs);
            ps.setObject(4, startTs);
            ps.setObject(5, endInclusive);
            ps.setObject(6, endInclusive);
        }, (rs, i) -> rs.getString("ID"));
    }

    /**
     * 依個案卡號查詢「滿 18 歲當日 00:00 以前」的 CrmRec.ID 清單，
     * 並可選用 [startTs, endExclusive] 的時間區間過濾 CreatedOnDate。
     * 條件：
     * - IsDeleted = 0
     * - CreatedOnDate < eighteenthStart            嚴格小於滿 18 歲當日（排除當天）
     * 回傳：
     * - 依 CreatedOnDate 由早到晚排序的 ID 清單
     *
     * @param acaCardNo       個案卡號
     * @param eighteenthStart 滿 18 歲當日 00:00（上界排除；僅取此之前）
     * @param startTs         選用：起始時間（含）
     * @param endInclusive    選用：結束時間（含）
     * @return 符合條件的 CrmRec.ID 清單
     */
    @Override
    public List<String> findCrmRecIdsBefore18(String acaCardNo, LocalDateTime eighteenthStart, LocalDateTime startTs, LocalDateTime endInclusive) {
        String sql = "SELECT CR.ID " +
                "FROM dbo.CrmRec CR " +
                "WHERE CR.IsDeleted = 0 " + // 只取未刪除
                "AND CR.ACACardNo = ? " +   // 指定卡號
                "AND CR.CreatedOnDate < ? " + // 嚴格小於 18 歲當日（排除當天）
                "AND (? IS NULL OR CR.CreatedOnDate  >= ?) " + // 若有起始：含下界
                "AND (? IS NULL OR CR.CreatedOnDate  <= ?) " + // 若有結束：含上界
                "ORDER BY CR.CreatedOnDate "; // 由早到晚排序
        return jdbc.query(sql, ps -> {
            ps.setString(1, acaCardNo);
            ps.setObject(2, eighteenthStart);
            ps.setObject(3, startTs);
            ps.setObject(4, startTs);
            ps.setObject(5, endInclusive);
            ps.setObject(6, endInclusive);
        }, (rs, i) -> rs.getString("ID"));
    }

    /**
     * 依一組 CrmRec 主鍵 ID 清單查詢所需欄位，取得對應的 CrmRec DTO 清單
     *
     * @param ids CrmRec 主鍵 ID 清單（不可包含 null/空字串）
     * @return 對應的 CrmRec DTO 清單，依 ids 原順序排列
     */
    @Override
    public List<CrmRec> findCrmRecsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        // 依 ids 長度動態組 IN 的佔位符，例如 "?,?,?,?"
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        String sql =
                "SELECT " +
                        "  CR.ID, " +
                        "  CAST(CR.CreatedOnDate AS date)   AS RecordDate, " +      // 紀錄日期
                        "  L_BR.Text                        AS BranchName, " +      // 分會別（ParentID=26）
                        "  L_J.Text                         AS JailAgency, " +      // 執行機關
                        "  L_C1.Text                        AS CrimeName1, " +      // 罪名1
                        "  L_C2.Text                        AS CrimeName2, " +      // 罪名2
                        "  L_C3.Text                        AS CrimeName3, " +      // 罪名3
                        "  L_NJ.Text                        AS NoJailReason, " +    // 未入獄原因
                        "  CAST(CR.Crm_VerdictDate AS date) AS VerdictDate, " +     // 執行日期
                        "  CR.Crm_Sentence                  AS SentenceType, " +    // 刑期種類
                        "  CR.CrmTerm                       AS TermText, " +        // 刑期(文字)
                        "  CAST(CR.CrmChaDate AS date)      AS PrisonInDate, " +    // 入獄時間
                        "  CAST(CR.Crm_ReleaseDate AS date) AS ReleasePlanDate, " + // 預定獲釋日
                        "  CAST(CR.CrmDisDate AS date)      AS PrisonOutDate, " +   // 出獄日期
                        "  L_DIS.Text                       AS PrisonOutReason, " + // 出獄原因
                        "  L_REM.Text                       AS Remission, " +       // 減刑案
                        "  CR.CrmTrain                      AS TrainType, " +       // 受訓種類
                        "  CR.CrmMemo                       AS Memo " +             // 備註
                        "FROM dbo.CrmRec CR " +
                        "LEFT JOIN dbo.Lists L_BR  " +
                        "  ON L_BR.ParentID = 26 " +
                        " AND L_BR.Value = CAST(CR.CreatedByBranchID AS NVARCHAR(50)) " +
                        "LEFT JOIN dbo.Lists L_J   " +
                        "  ON L_J.ListName = 'ACA_JAIL_TYPE' AND L_J.Value = CR.ProNoticeDep " +
                        "LEFT JOIN dbo.Lists L_C1  " +
                        "  ON L_C1.ListName = 'ACA_CRIME' AND L_C1.Value = CR.CrmCrime1 " +
                        "LEFT JOIN dbo.Lists L_C2  " +
                        "  ON L_C2.ListName = 'ACA_CRIME' AND L_C2.Value = CR.CrmCrime2 " +
                        "LEFT JOIN dbo.Lists L_C3  " +
                        "  ON L_C3.ListName = 'ACA_CRIME' AND L_C3.Value = CR.CrmCrime3 " +
                        "LEFT JOIN dbo.Lists L_NJ  " +
                        "  ON L_NJ.ListName = 'ACA_NOJAIL' AND L_NJ.Value = CR.Crm_NoJail " +
                        "LEFT JOIN dbo.Lists L_DIS " +
                        "  ON L_DIS.ListName = 'ACA_DISCHARGE' AND L_DIS.Value = CR.CrmDischarge " +
                        "LEFT JOIN dbo.Lists L_REM " +
                        "  ON L_REM.ListName = 'ACA_REMISSION' AND L_REM.Value = CR.CrmRemission " +
                        "WHERE CR.IsDeleted = 0 " +
                        "  AND CR.ID IN (" + placeholders + ")";
        Object[] params = ids.toArray(); // 將 ID 清單轉為綁定參數陣列

        // 將結果集映射到 CrmRec DTO，並把 date 欄位轉民國字串（DateUtil）
        List<CrmRec> rows = jdbc.query(sql, params, (rs, i) -> {
            var c = new CrmRec();
            c.setId(rs.getString("ID"));

            var d1 = rs.getDate("RecordDate");
            c.setRecordDate(d1 == null ? null : DateUtil.date2Roc(DateUtil.date2LocalDate(d1), yyyMMdd_slash));
            c.setBranchName(rs.getString("BranchName"));
            c.setJailAgency(rs.getString("JailAgency"));
            c.setCrimeName1(rs.getString("CrimeName1"));
            c.setCrimeName2(rs.getString("CrimeName2"));
            c.setCrimeName3(rs.getString("CrimeName3"));
            c.setNoJailReason(rs.getString("NoJailReason"));

            var d2 = rs.getDate("VerdictDate");
            c.setVerdictDate(d2 == null ? null : DateUtil.date2Roc(DateUtil.date2LocalDate(d2), yyyMMdd_slash));
            c.setSentenceType(rs.getString("SentenceType"));
            c.setTermText(rs.getString("TermText"));

            var d3 = rs.getDate("PrisonInDate");
            c.setPrisonInDate(d3 == null ? null : DateUtil.date2Roc(DateUtil.date2LocalDate(d3), yyyMMdd_slash));

            var d4 = rs.getDate("ReleasePlanDate");
            c.setReleasePlanDate(d4 == null ? null : DateUtil.date2Roc(DateUtil.date2LocalDate(d4), yyyMMdd_slash));

            var d5 = rs.getDate("PrisonOutDate");
            c.setPrisonOutDate(d5 == null ? null : DateUtil.date2Roc(DateUtil.date2LocalDate(d5), yyyMMdd_slash));
            c.setPrisonOutReason(rs.getString("PrisonOutReason"));
            c.setRemission(rs.getString("Remission"));
            c.setTrainType(rs.getString("TrainType"));
            c.setMemo(rs.getString("Memo"));
            return c;
        });

        // 依呼叫端給的 ids 原順序重排（IN(...) 不保證順序）
        Map<String, Integer> order = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) order.put(ids.get(i), i);
        rows.sort(Comparator.comparingInt(r -> order.getOrDefault(r.getId(), Integer.MAX_VALUE)));

        return rows;
    }

    /**
     * 依一組 ProRec 主鍵 ID 清單查詢所需欄位，取得對應的 ProRec DTO 清單
     *
     * @param ids ProRec 主鍵 ID 清單
     * @return 對應的 ProRec DTO 清單，依 ids 原順序排列
     */
    @Override
    public List<ProRec> findProRecsByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        // 使用 named parameter 的 IN (:ids)；SQL 前置分號避免與前一語句相黏
        String sql =
                ";SELECT " +
                        "    PR.ID, " +
                        "    L_BR.[Text]                           AS BranchName, " + // 分會（Lists.ParentID=26）
                        "    L_SRC.[Text]                          AS SourceText, " + // 來源（Lists.ACA_SOURCE）
                        // 健康狀況：以 CASE 轉對應中文，亦可改成 Lists 對照
                        "    CASE PR.ProHealth " +
                        "         WHEN 'A001' THEN N'良好' " +
                        "         WHEN 'A002' THEN N'普通' " +
                        "         WHEN 'A003' THEN N'舊制身心障礙(16類)' " +
                        "         WHEN 'A004' THEN N'欠佳' " +
                        "         WHEN 'A005' THEN N'具精神異常傾向' " +
                        "         WHEN 'A006' THEN N'新制身心障礙(8類)' " +
                        "         ELSE NULL END                    AS ProHealthText, " +

                        // 三層保護等級：各自取 ProDtl 最新一筆（依 PD.ID DESC）
                        "    OA1.L1Text                            AS ProtectLevel1, " +
                        "    OA2.L2Text                            AS ProtectLevel2, " +
                        "    OA3.L3Text                            AS ProtectLevel3, " +

                        // 重要日期（僅保留日期部分）
                        "    CAST(PR.ProNoticeDate AS date)        AS ProNoticeDate, " +
                        "    CAST(PR.ProDate       AS date)        AS ProDate, " +

                        // 其他屬性
                        "    PR.IsAdopt                              AS Adopt, " + // bit -> Boolean
                        "    CASE WHEN EXISTS ( " +                                // 家支標籤（是否有特定 ProjectRec）
                        "        SELECT 1 FROM dbo.ProjectRec P " +
                        "        WHERE P.LinkTableID = PR.ID AND P.LinkTableType = 'P' " +
                        "          AND P.ProjectID = 'A20130400094' AND P.IsDeleted = 0 " +
                        "    ) THEN N'家支' ELSE N'' END           AS HomeSupportTag, " +
                        "    L_DRUG.[Text]                         AS DrugProjectText, " +      // 毒品方案
                        "    CASE WHEN PR.ProCloseDate IS NULL THEN 0 ELSE 1 END AS Closed, " + // 是否結案
                        "    U.DisplayName                         AS StaffDisplayName, " +     // 建檔者顯示名（跨庫 Users）

                        // CounselorInstDisplay = 區域 + 空白 + 機構名稱 + (實習/正式)
                        "    COALESCE( " +
                        "      NULLIF( " +
                        "        CONCAT( " +
                        "          ISNULL(LA.[Text], N''), " +   // 區域（Lists: ACA_INSTAREA）
                        "          CASE WHEN NULLIF(LA.[Text], N'') IS NOT NULL AND NULLIF(IB.InstName, N'') IS NOT NULL THEN N' ' ELSE N'' END, " + // 區域與機構名皆非空時才加空白
                        "          ISNULL(IB.InstName, N''), " + // 機構名稱（InstBrd.InstName）
                        "          CASE " +
                        "            WHEN OM.WorkerID IS NULL " +
                        "                 OR (NULLIF(LA.[Text], N'') IS NULL AND NULLIF(IB.InstName, N'') IS NULL) THEN N'' " + // 沒有輔導員或兩者皆空：不加尾註（避免只顯示「(正式)」）
                        "            WHEN COALESCE(IB.IsUnofficial, 0) = 1 THEN N'(實習)' " +
                        "            ELSE N'(正式)' " +
                        "          END " +
                        "        ), N'' " + // CONCAT 結果若為空字串，轉 NULL
                        "      ), N'' " +   // 再把 NULL 轉回空字串
                        "    ) AS CounselorInstDisplay, " +
                        "    OM.WorkerID AS CounselorWorkerId, " + // 由 OUTER APPLY 取得的輔導員卡號
                        "    PR.ProFile AS ArchiveName " +         // 歸檔名稱
                        "FROM dbo.ProRec PR " +
                        // 來源、毒品方案等 Lists 對照
                        "LEFT JOIN dbo.Lists L_BR  " +
                        "       ON L_BR.ParentID = 26 " +
                        "      AND L_BR.Value = CAST(PR.CreatedByBranchID AS NVARCHAR(50)) " +
                        "LEFT JOIN dbo.Lists L_SRC " +
                        "       ON L_SRC.ListName = 'ACA_SOURCE' " +
                        "      AND L_SRC.Value    = PR.ProSource " +
                        "LEFT JOIN dbo.Lists L_DRUG " +
                        "       ON L_DRUG.ListName = 'PROJ_DRUG' " +
                        "      AND L_DRUG.Value    = PR.DrugForm " +
                        // 建檔者顯示名稱（跨 DB）
                        "LEFT JOIN [CaseManagementDnnDB].dbo.Users U " +
                        "       ON U.UserID = PR.CreatedByUserID " +

                        // L1：ProItem 最新一筆
                        "OUTER APPLY ( " +
                        "    SELECT TOP (1) L1.[Text] AS L1Text " +
                        "    FROM dbo.ProDtl PD " +
                        "    LEFT JOIN dbo.Lists L1 ON L1.ListName = 'ACA_PROTECT' AND L1.Value = PD.ProItem " +
                        "    WHERE PD.IsDeleted = 0 AND PD.ProRecID = PR.ID AND PD.ProItem IS NOT NULL " +
                        "    ORDER BY PD.ID DESC " +       //最新一筆
                        ") OA1 " +

                        // L2：Interview 最新一筆
                        "OUTER APPLY ( " +
                        "    SELECT TOP (1) L2.[Text] AS L2Text " +
                        "    FROM dbo.ProDtl PD " +
                        "    LEFT JOIN dbo.Lists L2 ON L2.ListName = 'ACA_PROTECT' AND L2.Value = PD.Interview " +
                        "    WHERE PD.IsDeleted = 0 AND PD.ProRecID = PR.ID AND PD.Interview IS NOT NULL " +
                        "    ORDER BY PD.ID DESC " +       //最新一筆
                        ") OA2 " +

                        // L3：ProPlace 最新一筆
                        "OUTER APPLY ( " +
                        "    SELECT TOP (1) L3.[Text] AS L3Text " +
                        "    FROM dbo.ProDtl PD " +
                        "    LEFT JOIN dbo.Lists L3 ON L3.ListName = 'ACA_PROTECT' AND L3.Value = PD.ProPlace " +
                        "    WHERE PD.IsDeleted = 0 AND PD.ProRecID = PR.ID AND PD.ProPlace IS NOT NULL " +
                        "    ORDER BY PD.ID DESC " +       //最新一筆
                        ") OA3 " +

                        // 取第一位非 EP 成員（當作輔導員）
                        "OUTER APPLY ( " +
                        "    SELECT TOP (1) PRM.WorkerID " +
                        "    FROM dbo.ProRecMember PRM " +
                        "    WHERE PRM.ProRecID = PR.ID " +
                        "      AND PRM.MemberType <> 'EP' " +
                        "      AND PRM.IsDeleted = 0 " +
                        "    ORDER BY PRM.ID " +
                        ") OM " +
                        // 輔導員 WorkerID -> 機構資料/區域
                        "LEFT JOIN dbo.InstBrd IB " +
                        "       ON IB.InstCardNo = OM.WorkerID " +
                        "      AND IB.IsDeleted = 0 " +
                        "LEFT JOIN dbo.Lists LA " +
                        "       ON LA.ListName = 'ACA_INSTAREA' " +
                        "      AND LA.Value    = IB.InstArea " +
                        "WHERE PR.IsDeleted = 0 " +
                        "  AND PR.ID IN (:ids)";

        // 綁定 named 參數 :ids
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("ids", ids);

        // 查詢並映射到 DTO
        List<Aca4001EraseQueryDto.ProRec> rows = npJdbc.query(sql, params, (rs, i) -> {
            var p = new Aca4001EraseQueryDto.ProRec();
            p.setId(rs.getString("ID"));
            p.setBranchName(rs.getString("BranchName"));
            p.setSourceText(rs.getString("SourceText"));
            p.setProHealthText(rs.getString("ProHealthText"));
            p.setProtectLevel1(rs.getString("ProtectLevel1"));
            p.setProtectLevel2(rs.getString("ProtectLevel2"));
            p.setProtectLevel3(rs.getString("ProtectLevel3"));

            var d1 = rs.getDate("ProNoticeDate");
            p.setProNoticeDate(d1 == null ? null : DateUtil.date2Roc(DateUtil.date2LocalDate(d1), yyyMMdd_slash));

            var d2 = rs.getDate("ProDate");
            p.setProDate(d2 == null ? null : DateUtil.date2Roc(DateUtil.date2LocalDate(d2), yyyMMdd_slash));

            // IsAdopt: bit -> Boolean（允許 null）
            Object adoptObj = rs.getObject("Adopt");
            p.setAdopt(adoptObj == null ? null : (Boolean) adoptObj);

            p.setHomeSupportTag(rs.getString("HomeSupportTag"));
            p.setDrugProjectText(rs.getString("DrugProjectText"));

            // Closed: 0/1 -> Boolean（允許 null）
            Object closedObj = rs.getObject("Closed");
            p.setClosed(closedObj == null ? null : ((Integer) closedObj) == 1);

            p.setStaffDisplayName(rs.getString("StaffDisplayName"));
            p.setCounselorInstDisplay(rs.getString("CounselorInstDisplay"));
            //p.setCounselorWorkerId(rs.getString("CounselorWorkerId")); // 如需回傳可打開
            p.setArchiveName(rs.getString("ArchiveName"));
            return p;
        });

        // 依輸入 ids 還原順序（IN 不保證順序）
        Map<String, Integer> order = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) order.put(ids.get(i), i);
        rows.sort(Comparator.comparingInt(r -> order.getOrDefault(r.getId(), Integer.MAX_VALUE)));

        return rows;
    }

    /**
     * 依 AcaDrugUse 主鍵集合查詢塗銷檢視所需欄位。
     * <p>
     * 僅回傳 IsDeleted=0 的有效資料，並將 CreatedByBranchID 轉換為 Lists.Text 顯示名稱。
     * 查詢結果會依輸入的 drgIds 順序回傳，以維持前端顯示一致性。
     *
     * @param drgIds AcaDrugUse.ID 清單（允許為 null 或空集合）
     * @return 依輸入順序排列的 AcaDrugUse DTO 清單；若未提供 ID 則回傳空清單
     */
    @Override
    public List<Aca4001EraseQueryDto.ACADrugUse> findAcaDrugUsesByIds(List<String> drgIds) {
        // 呼叫端未提供 ID 時直接回空清單以避免不必要的 SQL 查詢
        if (drgIds == null || drgIds.isEmpty()) return List.of();

        String sql =
                ";SELECT " +
                        "       ADU.ID                               AS id, " +               // 主鍵供排序用
                        "       L_BR.[Text]                          AS branchName, " +        // Lists 轉分會名稱
                        "       CAST(ADU.CreatedOnDate AS date)      AS RecordDate, " +      // 取建檔日期(僅日期)做為顯示用紀錄日
                        "       ADU.DrgUserText                      AS drgUserText, " +
                        "       ADU.OprFamilyText                    AS oprFamilyText, " +
                        "       ADU.OprFamilyCareText                AS oprFamilyCareText, " +
                        "       ADU.OprSupportText                   AS oprSupportText, " +
                        "       ADU.OprContactText                   AS oprContactText, " +
                        "       ADU.OprReferText                     AS oprReferText, " +
                        "       ADU.Addr                             AS addr, " +
                        "       ADU.OprAddr                          AS oprAddr " +
                        "FROM dbo.AcaDrugUse ADU " +
                        "LEFT JOIN dbo.Lists L_BR " +
                        "       ON L_BR.ParentID = 26 " +
                        "      AND L_BR.Value = CAST(ADU.CreatedByBranchID AS NVARCHAR(50)) " +
                        "WHERE ADU.IsDeleted = 0 " +                                        // 僅取未刪除資料
                        "  AND ADU.ID IN (:ids)";

        // 以 named parameter 綁定 ID 清單，避免字串拼接注入風險
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("ids", drgIds);

        // 查詢後先保留主鍵，稍後依輸入順序重建清單
        List<java.util.AbstractMap.SimpleEntry<String, Aca4001EraseQueryDto.ACADrugUse>> rows =
                npJdbc.query(sql, params, (rs, i) -> {
                    String id = rs.getString("id");
                    var dto = new Aca4001EraseQueryDto.ACADrugUse();
                    dto.setId(id); // ✅ 加上這行，DTO 就會包含主鍵
                    // recordDate -> 民國 yyy/MM/dd（若為 null 則回 null）
                    java.sql.Date d1 = rs.getDate("RecordDate");
                    dto.setRecordDate(d1 == null ? null : DateUtil.date2Roc(DateUtil.date2LocalDate(d1), yyyMMdd_slash));
                    dto.setBranchName(rs.getString("branchName"));
                    dto.setDrgUserText(rs.getString("drgUserText"));
                    dto.setOprFamilyText(rs.getString("oprFamilyText"));
                    dto.setOprFamilyCareText(rs.getString("oprFamilyCareText"));
                    dto.setOprSupportText(rs.getString("oprSupportText"));
                    dto.setOprContactText(rs.getString("oprContactText"));
                    dto.setOprReferText(rs.getString("oprReferText"));
                    dto.setAddr(rs.getString("addr"));
                    dto.setOprAddr(rs.getString("oprAddr"));
                    return new java.util.AbstractMap.SimpleEntry<>(id, dto);
                });

        Map<String, Aca4001EraseQueryDto.ACADrugUse> mapped = new HashMap<>();
        for (var entry : rows) {
            mapped.put(entry.getKey(), entry.getValue());
        }

        // 建立 ID -> DTO 的映射後依輸入順序重建結果，確保回傳順序與 drgIds 一致
        // IN 子句不保證順序，因此依輸入 ID 順序組裝結果
        List<Aca4001EraseQueryDto.ACADrugUse> ordered = new ArrayList<>(drgIds.size());
        for (String id : drgIds) {
            var dto = mapped.get(id);
            if (dto != null) {
                ordered.add(dto);
            }
        }

        return ordered;
    }

    /**
     * 查詢某個 ACACardNo 在 ProRec 中「最新一筆（依 ProDate 由新到舊）」是否已結案。
     * 定義：
     * - 是否結案：ProCloseDate 非 NULL 視為結案，NULL 視為未結案。
     * 回傳：
     * - true  ：最新一筆為結案
     * - false ：最新一筆未結案
     * - null  ：查無任何符合（IsDeleted=0 且指定卡號）的紀錄
     *
     * @param acaCardNo 個案卡號
     * @return Boolean：true/false 或查無時回 null
     */
    @Override
    public Boolean findLatestProRecClosed(String acaCardNo) {
        String sql = "SELECT TOP 1 " +
                "CASE WHEN ProCloseDate IS NULL THEN 0 ELSE 1 END AS Closed " +
                "FROM dbo.ProRec " +
                "WHERE IsDeleted = 0 AND ACACardNo = ? " +
                "ORDER BY ProDate DESC"; // 以 ProDate 由新到舊取最新

        return jdbc.query(sql, ps -> ps.setString(1, acaCardNo), rs -> {
            if (!rs.next()) return null; // 查無任何列 → 回傳 null
            return rs.getInt("Closed") == 1; // 有資料：0/1 轉成 boolean
        });
    }

    /*eraseQuery API & restoreQuery API*/

    /**
     * 檢查某個 ACACardNo 在 ACABrd 主檔是否已被標記為「塗銷」。
     * 定義：
     * - 以 ACACardNo + IsDeleted=0 查詢 ACABrd。
     * - 若有資料：IsErase=1 視為已塗銷，IsErase=0 視為未塗銷。
     * - 若查無任何資料：回傳 null（表示找不到該個案）。
     * 回傳：
     * - true  ：已塗銷（IsErase=1）
     * - false ：未塗銷（IsErase=0）
     * - null  ：查無該個案（或無未刪除資料）
     *
     * @param acaCardNo 個案卡號
     * @return Boolean：true/false；查無回 null
     */
    @Override
    public Boolean findPersonErased(String acaCardNo) {
        String sql = "SELECT TOP 1 " +
                "CASE WHEN IsErase = 1 THEN 1 ELSE 0 END AS Erased " +
                "FROM dbo.ACABrd " +
                "WHERE ACACardNo = ? AND IsDeleted = 0";

        return jdbc.query(sql, ps -> ps.setString(1, acaCardNo), rs -> {
            if (!rs.next()) return null;                    // 查無資料 → 回傳 null
            return rs.getInt("Erased") == 1;     // 有資料 → 0/1 轉 boolean
        });
    }

    /**
     * 依個案卡號取得該個案所有 CrmRec 的主鍵 ID 清單。
     *
     * @param acaCardNo 個案卡號（非空）
     * @return 該卡號底下所有 CrmRec 的 ID 清單（可能為空）
     */
    @Override
    public List<String> findAllCrmRecIdsByAcaCardNo(String acaCardNo) {
        String sql = "SELECT ID FROM dbo.CrmRec WHERE ACACardNo = :aca";
        try (var con = sql2o.open()) {
            return con.createQuery(sql).addParameter("aca", acaCardNo).executeAndFetch(String.class);
        }
    }

    /**
     * 依個案卡號取得該個案所有 AcaDrugUse 的主鍵 ID 清單。
     *
     * @param acaCardNo 個案卡號（非空）
     * @return 該卡號底下所有 AcaDrugUse 的 ID 清單（可能為空）
     */
    @Override
    public List<String> findAllAcaDrugUseIdsByAcaCardNo(String acaCardNo) {
        String sql = "SELECT ID FROM dbo.AcaDrugUse WHERE ACACardNo = :aca";
        try (var con = sql2o.open()) {
            return con.createQuery(sql).addParameter("aca", acaCardNo).executeAndFetch(String.class);
        }
    }

    /*erase API*/

    /**
     * 依個案卡號取得該個案所有 ProRec 的主鍵 ID 清單。
     *
     * @param acaCardNo 個案卡號（非空）
     * @return 該卡號下所有 ProRec 的 ID 清單（可能為空）
     */
    @Override
    public List<String> findAllProRecIdsByAcaCardNo(String acaCardNo) {
        String sql = "SELECT ID FROM dbo.ProRec WHERE ACACardNo = :aca";
        try (var con = sql2o.open()) {
            return con.createQuery(sql).addParameter("aca", acaCardNo).executeAndFetch(String.class);
        }
    }

    /*auditQuery API*/

    /**
     * 讀取 ACA_EraseAudit 塗銷異動表（不彙總，每列即一次動作），並連到 DNN 使用者表取顯示名稱。
     * - 依建立時間新到舊排序。
     * 回傳：
     * - 對應 Aca4001AuditQueryDto.Row 的清單；查無資料時回空清單。
     */
    @Override
    public List<Aca4001AuditQueryDto.Row> findAuditRows() {
        String sql =
                "SELECT " +
                        "  A.AuditID                                  AS auditId, " +
                        // 將時間標準化為 DATETIME2(0)，方便 Java 端映射與顯示
                        "  CAST(A.CreatedOnDate AS DATETIME2(0))      AS createdOn, " +
                        "  A.ACACardNo                                AS acaCardNo, " +
                        "  A.ActionType                               AS action, " +
                        // docNum 轉成 INT（若欄位是可空或非數字，CAST 失敗會丟錯；可視情況改 TRY_CONVERT）
                        "  CAST(A.DocNum AS INT)                      AS docNum, " +
                        "  A.EraseReason                              AS eraseReason, " +
                        "  A.RestoreReason                            AS restoreReason, " +
                        // 將可能為 INT 或 NVARCHAR 的欄位統一以字串型別回傳，利於 DTO 映射
                        "  CAST(A.CreatedByUserID AS NVARCHAR(50))    AS userId, " +
                        "  CAST(A.UserIP AS NVARCHAR(64))             AS userIp, " +
                        // 連到 DNN Users 取顯示名稱；若 CreatedByUserID 不是純數字，TRY_CONVERT 會回 NULL，LEFT
                        "  U.DisplayName                              AS userName " +
                        "FROM dbo.ACA_EraseAudit A " +
                        "LEFT JOIN CaseManagementDnnDB.dbo.Users U " +
                        "  ON U.UserID = TRY_CONVERT(INT, A.CreatedByUserID) " +
                        "ORDER BY A.CreatedOnDate DESC";

        try (var con = sql2o.open()) {
            return con.createQuery(sql).executeAndFetch(Aca4001AuditQueryDto.Row.class);
        }
    }


}
