package com.hn2.cms.dto.aca4001;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class Aca4001EraseQueryDto {
    @JsonProperty("isOver18")
    private boolean isOver18;       // 是否滿18
    @JsonProperty("isLatestProRecClosed")
    private boolean isLatestProRecClosed;      // 是否結案(最新的保護紀錄是否已結案)
    @JsonProperty("isErased")
    private boolean isErased;      // 個案是否已塗銷
    private List<CrmRec> crmRecList; // 犯罪紀錄ID清單
    private List<ProRec> proRecListBefore18; // 保護紀錄ID清單
    private List<ACADrugUse> acaDrugUseList; // 保護紀錄ID清單

    @Data
    public static class CrmRec {
        private String id;                 // CrmRec.ID
        private String recordDate;         // CreatedOnDate -> 民國 yyy/MM/dd
        private String branchName;         // CreatedByBranchID
        private String jailAgency;         // ProNoticeDep
        private String crimeName1;         // CrmCrime1
        private String crimeName2;         // CrmCrime2
        private String crimeName3;         // CrmCrime3
        private String noJailReason;       // Crm_NoJail
        private String verdictDate;        // Crm_VerdictDate -> 民國 yyy/MM/dd
        private String sentenceType;       // Crm_Sentence
        private String termText;           // CrmTerm
        private String prisonInDate;       // CrmChaDate -> 民國 yyy/MM/dd
        private String releasePlanDate;    // Crm_ReleaseDate -> 民國 yyy/MM/dd
        private String prisonOutDate;      // CrmDisDate -> 民國 yyy/MM/dd
        private String prisonOutReason;    // CrmDischarge
        private String remission;          // CrmRemission
        private String trainType;          // CrmTrain
        private String memo;               // CrmMemo
    }

    @Data
    public static class ProRec {
        private String id;                 // ProRec.ID
        // 分會（Lists.ParentID=26）
        private String branchName;         // Lists.Text(ParentID=26, Value=ProRec.createdByBranchID)
        // 受案來源
        private String sourceText;         // Lists.Text(ListName='ACA_SOURCE', Value=ProRec.ProSource)
        // 體能狀況（A001~A006 -> 中文）
        private String proHealthText;      // 依 ProRec.ProHealth 對應中文
        private String protectLevel1;      // ProDtl.ProItem -> Lists.Text('ACA_PROTECT')
        private String protectLevel2;      // ProDtl.Interview -> Lists.Text('ACA_PROTECT')（可為 null）
        private String protectLevel3;      // ProDtl.ProPlace -> Lists.Text('ACA_PROTECT')（可為 null）
        // 申請(通知)日期、保護日期（民國 yyy/MM/dd）
        private String proNoticeDate;      // ProRec.ProNoticeDate -> 民國
        private String proDate;            // ProRec.ProDate -> 民國
        // 認輔狀況
        @JsonProperty("isAdopt")
        private Boolean adopt;             // ProRec.isAdopt（用 Boolean 以容許 null）
        // 家支狀況（有符合條件即回 "家支"，否則 ""）
        private String homeSupportTag;
        // 毒品專案
        private String drugProjectText;    // Lists.Text('PROJ_DRUG', Value=ProRec.DrugForm)
        // 結案狀態
        private Boolean closed;            // ProRec.IsClosed（或 ProCloseDate != null）
        // 工作人員（建立者顯示名）
        private String staffDisplayName;   // join CaseManagementDnn.dbo.Users by CreatedByUserID
        // 輔導員（機構區域文字 + 機構名稱 + (實習)）
        private String counselorInstDisplay; // ex: "北區 臺北OO(實習)"
        //private String counselorWorkerId;  // （除錯用，可視需求保留/拿掉）
        private String archiveName;          // 歸檔名稱（ProRec.ProFile)
    }

    @Data
    public static class ACADrugUse {
        private String id;
        private String recordDate;         // CreatedOnDate -> 民國 yyy/MM/dd
        private String branchName;           // 分會 Lists.Text(ParentID=26, Value=AcaDrugUse.createdByBranchID)
        private String drgUserText;          // 施用毒品
        private String oprFamilyText;        // 未來共居家屬
        private String oprFamilyCareText;    // 關懷需求
        private String oprSupportText;       // 家庭支持度
        private String oprContactText;       // 同意更生保護會及其分會聯繫
        private String oprReferText;         // 就業轉介需求
        private String addr;                 // 通訊地址
        private String oprAddr;              // 出監後擬住地址
    }

    /**
     * Service 層內部使用的簡單資料結構，承載個案的生日與「滿 18 歲當日」的日期。
     * - birthDate：生日，只保存日期，不含時間。
     * - eighteenthStart：滿 18 歲的那一天（同樣只保存日期）。
     */
    @Data
    public static class PersonBirth {
        private LocalDate birthDate;       // 只保留日期
        private LocalDate eighteenthStart; // 18歲當天00:00
    }
}
