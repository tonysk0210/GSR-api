package com.hn2.cms.service.report02;

import com.hn2.cms.dto.report02.Report02Dto;
import com.hn2.cms.dto.report02.Report02Dto.*;
import com.hn2.cms.payload.report02.Report02Payload;
import com.hn2.cms.repository.report02.Report02Repository;
import com.hn2.core.dto.DataDto;
import com.hn2.core.dto.ResponseInfo;
import com.hn2.core.payload.GeneralPayload;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class Report02ServiceImpl implements Report02Service {

    private final Report02Repository repo;

    public Report02ServiceImpl(Report02Repository repo) {
        this.repo = repo;
    }

    /**
     * 對外查詢：把 Repository 產生的「扁平列（FlatRow）」轉成指定的巢狀 DTO 結構。
     * 流程：
     * 1) 取得扁平列
     * 2) 依分會（branchCode）分群
     * 3) 依分會 sortOrder → branchCode 排序群組
     * 4) 每群轉成 orgs[]（並依 orgCode 由小到大排序）
     * 5) 計算每群的 totals
     * 6) 組出 items[]，最後回傳 range + items
     */

    public DataDto<Report02Dto> query(GeneralPayload<Report02Payload> payload) {

        // 取出查詢區間
        Report02Payload p = payload.getData();
        LocalDate from = p.getFrom();
        LocalDate to = p.getTo();
        // 1) 查詢扁平結果（每列 = 分會 × 機關 的三個計數 + 名稱）
        List<FlatRow> rows = repo.findAggregates(from, to);

        // 2) 依分會代碼分群（Map<branchCode, List<FlatRow>>）
        Map<String, List<FlatRow>> bySign = rows.stream()
                .collect(Collectors.groupingBy(FlatRow::getBranchCode, LinkedHashMap::new, Collectors.toList()));

        // 3) 將各分會群排序：
        //    先看群內任一列的 sortOrder（找第一個非 null），沒有就排到最後（Integer.MAX_VALUE），再以 branchCode 當次要排序
        List<Map.Entry<String, List<FlatRow>>> ordered = bySign.entrySet().stream()
                .sorted(Comparator
                        .comparing((Map.Entry<String, List<FlatRow>> e) -> {
                            Integer so = e.getValue().stream()
                                    .map(FlatRow::getSortOrder)
                                    .filter(Objects::nonNull)
                                    .findFirst().orElse(Integer.MAX_VALUE);
                            return so;
                        })
                        .thenComparing(Map.Entry::getKey) // 次要用 branchCode 保底
                )
                .collect(Collectors.toList());

        List<Report02Dto.Item> items = new ArrayList<>();

        // 4) 逐分會群轉成 Item
        for (Map.Entry<String, List<FlatRow>> entry : ordered) {
            String branchCode = entry.getKey();
            List<FlatRow> list = entry.getValue();

            // 從群內任一列取分會名稱與 sortOrder（通常每列相同）
            String branchName = list.stream().map(FlatRow::getBranchName).filter(Objects::nonNull).findFirst().orElse("");
            Integer sortOrder = list.stream().map(FlatRow::getSortOrder).filter(Objects::nonNull).findFirst().orElse(null);

            // 4-1) 把每列轉為 orgs[]；排序規則：orgCode 由小到大
            //      先嘗試整數比較，失敗（含字母）再用字串比較
            List<Report02Dto.Org> orgs = list.stream()
                    .filter(r -> r.getOrgCode() != null) // 🔸 新增這行，避免 null 機關出現在 orgs
                    .map(r -> Report02Dto.Org.builder()
                            .orgCode(r.getOrgCode())
                            .orgName(Optional.ofNullable(r.getOrgName()).orElse("")) // JOIN 對不到時以空字串
                            .pendingCount(r.getPendingCount())
                            .signedCount(r.getSignedCount())
                            .caseCount(r.getCaseCount())
                            .build()
                    )
                    .sorted((o1, o2) -> {
                        try {
                            int n1 = Integer.parseInt(o1.getOrgCode());
                            int n2 = Integer.parseInt(o2.getOrgCode());
                            return Integer.compare(n1, n2);
                        } catch (NumberFormatException ex) {
                            return o1.getOrgCode().compareTo(o2.getOrgCode());
                        }
                    })
                    .collect(Collectors.toList());

            // 4-2) 計算 totals（該分會底下 orgs 的合計）
            int pendingSum = orgs.stream().mapToInt(Report02Dto.Org::getPendingCount).sum();
            int signedSum = orgs.stream().mapToInt(Report02Dto.Org::getSignedCount).sum();
            int caseSum = orgs.stream().mapToInt(Report02Dto.Org::getCaseCount).sum();

            Report02Dto.Totals totals = Report02Dto.Totals.builder()
                    .pendingCount(pendingSum)
                    .signedCount(signedSum)
                    .caseCount(caseSum)
                    .orgCount(orgs.size())
                    .build();

            // 4-3) 組成單一分會的 Item
            items.add(Report02Dto.Item.builder()
                    .branchCode(branchCode)
                    .branchName(branchName)
                    .sortOrder(sortOrder == null ? Integer.MAX_VALUE : sortOrder) // 無排序值 → 放最後
                    .orgs(orgs)
                    .totals(totals)
                    .build());
        }

        // 5) 組最終 DTO（range + items），並包成 DataDto 回傳
        Report02Dto dto = Report02Dto.builder()
                .range(new Report02Dto.Range(from, to))
                .items(items)
                .build();

        return new DataDto<>(dto, new ResponseInfo(1, "查詢成功"));
    }
}
