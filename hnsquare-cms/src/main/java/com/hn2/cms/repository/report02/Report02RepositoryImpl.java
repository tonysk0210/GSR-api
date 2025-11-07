package com.hn2.cms.repository.report02;

import com.hn2.cms.dto.report02.Report02Dto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class Report02RepositoryImpl implements Report02Repository {

    private final JdbcTemplate jdbc;

    public Report02RepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 取回「分會 × 機關」的聚合計數（扁平結構 FlatRow）。
     * - 分會名稱 / 排序：Lists(ParentID=26) 依 SIGN_PROT_NO 對應 Value -> Text/SortOrder
     * - 機關名稱：Org_Lists 依 ORG_CODE 對應 ORG_NAME
     * - RS_DT 為 date 型別：直接用 >= from AND <= to（含頭含尾）
     */
    public List<Report02Dto.FlatRow> findAggregates(LocalDate from, LocalDate to) {

        // 核心查詢：
        // 1) agg：在 SUP_AfterCare 依「分會 × 機關」分組，計算三個狀態的數量
        // 2) lists_clean：從 Lists(ParentID=26) 取每個 Value 最新的一筆（避免重覆/歷史值）
        // 3) org_clean：從 Org_Lists 取每個 ORG_CODE 一筆（避免重覆）
        // 4) 將分會名稱/排序與機關名稱 JOIN 回 agg，並依分會排序 + 機關代碼排序輸出
        /*String sql =
                "WITH agg AS ( \n" +
                        "  SELECT \n" +
                        "    a.SIGN_PROT_NO AS branchCode, \n" + // 分會代碼
                        "    a.ORG_CODE     AS orgCode, \n" + // 機關代碼
                        "    SUM(CASE WHEN a.SIGN_STATE = 0 THEN 1 ELSE 0 END) AS pendingCount, \n" +
                        "    SUM(CASE WHEN a.SIGN_STATE = 1 THEN 1 ELSE 0 END) AS signedCount, \n" +
                        "    SUM(CASE WHEN a.SIGN_STATE = 3 THEN 1 ELSE 0 END) AS caseCount \n" +
                        "  FROM dbo.SUP_AfterCare a WITH (NOLOCK) \n" + // 視規範決定是否保留 NOLOCK
                        "  WHERE a.RS_DT >= ? AND a.RS_DT <= ? \n" +    // date 型別直接比較（含頭含尾）
                        "  GROUP BY a.SIGN_PROT_NO, a.ORG_CODE \n" +
                        "), lists_clean AS ( \n" +
                        "  SELECT \n" +
                        "    Value, Text, SortOrder, \n" +
                        "    ROW_NUMBER() OVER (PARTITION BY Value \n" +
                        "      ORDER BY ISNULL(ModifiedOnDate, CreatedOnDate) DESC, EntryID DESC) AS rn \n" + // 每個 Value 取最新一筆
                        "  FROM dbo.Lists \n" +
                        "  WHERE ParentID = 26 \n" + // 分會清單
                        "), org_clean AS ( \n" +
                        "  SELECT \n" +
                        "    ORG_CODE, ORG_NAME, \n" +
                        "    ROW_NUMBER() OVER (PARTITION BY UPPER(LTRIM(RTRIM(ORG_CODE))) \n" +
                        "      ORDER BY ORG_CODE) AS rn \n" + // ORG_CODE 相同只取一筆
                        "  FROM dbo.Org_Lists \n" +
                        ") \n" +
                        "SELECT \n" +
                        "  agg.branchCode, \n" +
                        "  ls.Text      AS branchName, \n" + // 分會名稱
                        "  ls.SortOrder AS sortOrder, \n" + // 分會排序
                        "  agg.orgCode, \n" +
                        "  oc.ORG_NAME  AS orgName, \n" +   // 機關名稱（Org_Lists）
                        "  agg.pendingCount, \n" +
                        "  agg.signedCount, \n" +
                        "  agg.caseCount \n" +
                        "FROM agg \n" +
                        "LEFT JOIN lists_clean ls \n" +
                        "  ON ls.rn = 1 \n" +
                        " AND UPPER(LTRIM(RTRIM(ls.Value))) = UPPER(LTRIM(RTRIM(agg.branchCode))) \n" + // 分會代碼比對（去空白/大小寫忽略）
                        "LEFT JOIN org_clean oc \n" +
                        "  ON oc.rn = 1 \n" +
                        " AND UPPER(LTRIM(RTRIM(oc.ORG_CODE))) = UPPER(LTRIM(RTRIM(agg.orgCode))) \n" + // 機關代碼比對（去空白/大小寫忽略）
                        "ORDER BY \n" +
                        "  ISNULL(ls.SortOrder, 2147483647) ASC, \n" + // 先依分會排序
                        "  TRY_CONVERT(int, agg.orgCode) ASC, agg.orgCode ASC;"; // 再依機關代碼（可數值化則先）*/
        // 查詢所有分會（包含無資料者），並統計每個分會×機關的狀態數量

        String sql =
                // -----------------------------
                // 1️⃣ 聚合來源：SUP_AfterCare
                //    - 依「分會 × 機關」分組
                //    - 計算三種 SIGN_STATE 狀態的筆數
                // -----------------------------
                "WITH agg AS ( \n" +
                        "  SELECT \n" +
                        "    a.SIGN_PROT_NO AS branchCode, \n" +  // 分會代碼
                        "    a.ORG_CODE     AS orgCode, \n" +    // 機關代碼
                        "    SUM(CASE WHEN a.SIGN_STATE = 0 THEN 1 ELSE 0 END) AS pendingCount, \n" + // 未簽署
                        "    SUM(CASE WHEN a.SIGN_STATE = 1 THEN 1 ELSE 0 END) AS signedCount,  \n" + // 已簽署
                        "    SUM(CASE WHEN a.SIGN_STATE = 3 THEN 1 ELSE 0 END) AS caseCount     \n" + // 已成案
                        "  FROM dbo.SUP_AfterCare a WITH (NOLOCK) \n" + // 避免鎖表（依規範可移除）
                        "  WHERE a.RS_DT >= ? AND a.RS_DT <= ? \n" +    // 查詢區間（含頭含尾）
                        "  GROUP BY a.SIGN_PROT_NO, a.ORG_CODE \n" +
                        "), \n" +
                        // -----------------------------
                        // 2️⃣ 分會清單：Lists (ParentID=26)
                        //    - 每個分會代碼 (Value) 取最新一筆
                        //    - 包含分會名稱 (Text) 與排序 (SortOrder)
                        // -----------------------------
                        "lists_clean AS ( \n" +
                        "  SELECT \n" +
                        "    Value, Text, SortOrder, \n" +
                        "    ROW_NUMBER() OVER (PARTITION BY Value \n" +
                        "      ORDER BY ISNULL(ModifiedOnDate, CreatedOnDate) DESC, EntryID DESC) AS rn \n" +
                        "  FROM dbo.Lists \n" +
                        "  WHERE ParentID = 26 \n" +  // 26：分會清單
                        "), \n" +
                        // -----------------------------
                        // 3️⃣ 機關清單：Org_Lists
                        //    - 每個 ORG_CODE 取第一筆（去重）
                        // -----------------------------
                        "org_clean AS ( \n" +
                        "  SELECT \n" +
                        "    ORG_CODE, ORG_NAME, \n" +
                        "    ROW_NUMBER() OVER (PARTITION BY UPPER(LTRIM(RTRIM(ORG_CODE))) \n" +
                        "      ORDER BY ORG_CODE) AS rn \n" +
                        "  FROM dbo.Org_Lists \n" +
                        ") \n" +
                        // -----------------------------
                        // 4️⃣ 主查詢：
                        //    - 以 lists_clean（全部分會）為主體
                        //    - LEFT JOIN agg → 即使該分會沒有資料，也會出現在結果中
                        //    - LEFT JOIN org_clean → 加入機關名稱
                        // -----------------------------
                        "SELECT \n" +
                        "  ls.Value AS branchCode, \n" +
                        "  ls.Text  AS branchName, \n" +
                        "  ls.SortOrder, \n" +
                        "  oc.ORG_CODE AS orgCode, \n" +
                        "  oc.ORG_NAME AS orgName, \n" +
                        "  ISNULL(agg.pendingCount, 0) AS pendingCount, \n" + // 若無資料，補 0
                        "  ISNULL(agg.signedCount, 0)  AS signedCount, \n" +
                        "  ISNULL(agg.caseCount, 0)    AS caseCount \n" +
                        "FROM lists_clean ls \n" +
                        "LEFT JOIN agg \n" +
                        "  ON UPPER(LTRIM(RTRIM(ls.Value))) = UPPER(LTRIM(RTRIM(agg.branchCode))) \n" + // 分會代碼比對
                        "LEFT JOIN org_clean oc \n" +
                        "  ON oc.rn = 1 \n" +
                        " AND UPPER(LTRIM(RTRIM(oc.ORG_CODE))) = UPPER(LTRIM(RTRIM(agg.orgCode))) \n" + // 機關代碼比對
                        "WHERE ls.rn = 1 \n" + // 每個分會只取最新一筆
                        // -----------------------------
                        // 5️⃣ 排序邏輯：
                        //    - 先依分會 SortOrder（無值則放最後）
                        //    - 再依機關代碼（可轉數值的先）
                        // -----------------------------
                        "ORDER BY \n" +
                        "  ISNULL(ls.SortOrder, 2147483647) ASC, \n" +
                        "  TRY_CONVERT(int, oc.ORG_CODE) ASC, oc.ORG_CODE ASC;";


        // 綁定參數與 RowMapper：將每一列結果映射成 FlatRow
        return jdbc.query(sql, ps -> {
                    // JDBC 4.2 起可直接綁 LocalDate；舊 driver 可改成 java.sql.Date.valueOf(from/to)
                    ps.setObject(1, from);
                    ps.setObject(2, to);
                }, (rs, i) -> Report02Dto.FlatRow.builder()
                        .branchCode(rs.getString("branchCode"))
                        .branchName(rs.getString("branchName"))
                        .sortOrder(rs.getObject("sortOrder") == null ? null : rs.getInt("sortOrder"))
                        .orgCode(rs.getString("orgCode"))
                        .orgName(rs.getString("orgName"))
                        .pendingCount(rs.getInt("pendingCount"))
                        .signedCount(rs.getInt("signedCount"))
                        .caseCount(rs.getInt("caseCount"))
                        .build()
        );
    }
}
