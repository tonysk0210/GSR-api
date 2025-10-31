package com.hn2.cms.service.aca2003;

import com.hn2.cms.dto.aca2003.Aca2003DetailView;
import com.hn2.cms.dto.aca2003.Aca2003QueryDto;
import com.hn2.cms.dto.aca2003.Aca2003SaveResponse;
import com.hn2.cms.model.AcaBrdEntity;
import com.hn2.cms.model.SupAfterCareEntity;
import com.hn2.cms.model.aca2003.AcaDrugUseEntity;
import com.hn2.cms.payload.aca2003.Aca2003DeletePayload;
import com.hn2.cms.payload.aca2003.Aca2003QueryByCardPayload;
import com.hn2.cms.payload.aca2003.Aca2003QueryByIdPayload;
import com.hn2.cms.payload.aca2003.Aca2003QueryByPersonalIdPayload;
import com.hn2.cms.payload.aca2003.Aca2003SavePayload;
import com.hn2.cms.repository.AcaBrdRepository;
import com.hn2.cms.repository.SupAfterCareRepository;
import com.hn2.cms.repository.aca2003.Aca2003Repository;
import com.hn2.core.dto.DataDto;
import com.hn2.core.dto.ResponseInfo;
import com.hn2.core.payload.GeneralPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * Aca2003ServiceImpl
 * <p>
 * 提供「毒品濫用資料表 (AcaDrugUse)」的應用服務：
 * - save: 新增或更新（軟規則驗證 + 一致性檢查）
 * - queryById: 依 ID 查詢詳情（含 ACABrd 連動欄位）
 * - queryLatestByCardNo: 依 ACACardNo 取最新一筆（ID 最大）
 * - softDelete: 軟刪除（isDeleted=1）
 * <p>
 * 設計要點：
 * - 業務規則集中在 Service（資料層只負責查存）
 * - 軟刪除統一過濾 (isDeleted=0 or null)
 * - 例外訊息採友善字串，便於前端顯示
 */
@Service
public class Aca2003ServiceImpl implements Aca2003Service {

    // ====== 常數訊息 ======
    private static final String MSG_PAYLOAD_EMPTY = "payload 不可為空";
    private static final String MSG_ID_EMPTY = "id 不可為空";
    private static final String MSG_USER_ID_EMPTY = "userId「修檔人員ID」不可為空";
    private static final String MSG_CARD_EMPTY = "acaCardNo「個案編號」不可為空";
    private static final String MSG_DATA_NOT_FOUND = "指定資料不存在";
    private static final String MSG_DATA_DELETED = "指定資料已刪除";
    private static final String MSG_DUP_ACTIVE = "相同「個案編號」的有效資料已存在";
    private static final String MSG_ACABRD_NOT_FOUND = "指定的「個案編號」(ACACardNo) 不存在於有效的 「個案基本資料」ACABrd";
    private static final String MSG_PERSONAL_ID_EMPTY = "personalId「個人身分證號」不可為空";
    private static final String MSG_CARD_BY_PERSONAL_ID_NOT_FOUND = "身分證字號 查無對應 有效個案編號";
    private static final String MSG_MULTIPLE_CARDS_FOUND = "查到多筆有效個案編號，請聯絡系統管理員";
    private static final String MSG_NAM_IDNO_EMPTY = "個案資料 缺少 身分證字號 欄位資料";
    private static final String MSG_AFTER_CARE_NOT_FOUND = "查無矯正署資料";
    private static final String MSG_CARD_NOT_FOUND = "個案編號不存在";


    private final Aca2003Repository repo;
    private final AcaBrdRepository acaBrdRepository;
    private final SupAfterCareRepository supAfterCareRepository;

    @Autowired
    public Aca2003ServiceImpl(Aca2003Repository repo, AcaBrdRepository acaBrdRepository, SupAfterCareRepository supAfterCareRepository) {
        this.repo = repo;
        this.acaBrdRepository = acaBrdRepository;
        this.supAfterCareRepository = supAfterCareRepository;
    }

    // ============================================================
    // save API：新增或更新
    // ============================================================

    /**
     * 新增或更新 AcaDrugUse 資料。
     * - 新增：id = null，需檢查 ACABrd 存在、且不可有相同個案編號的有效資料
     * - 更新：id != null，主鍵欄位不可變更，只能更新業務欄位
     */
    @Override
    @Transactional
    public DataDto<Aca2003SaveResponse> save(GeneralPayload<Aca2003SavePayload> payload) {
        // ---- 0) 入口檢核 ----
        if (payload == null || payload.getData() == null) {
            return fail(MSG_PAYLOAD_EMPTY);
        }
        var p = payload.getData();
        if (p.getUserId() == null) return fail(MSG_USER_ID_EMPTY);
        if (isBlank(p.getAcaCardNo())) return fail(MSG_CARD_EMPTY);

        // 正規化基本欄位（trim）
        final String card = p.getAcaCardNo().trim();

        try {
            // ---- A) 應用層的一致性檢查 ----
            // 1) ACABrd 必須存在且有效
            if (repo.existsActiveAcaBrd(card) == 0) {
                return fail(MSG_ACABRD_NOT_FOUND);
            }

            // ---- B) 分支：新增 or 更新 ----
            if (p.getId() == null) {
                return create(p, card);   // 新增
            } else {
                return update(p, card); // 更新
            }

        } catch (DataIntegrityViolationException ex) {
            // 兜底：DB 層唯一衝突（2627/2601），轉為友善訊息
            if (isUniqueKeyViolation(ex)) {
                return fail(MSG_DUP_ACTIVE);
            }
            throw ex; // 非預期例外：往上丟交由全域處理器
        }
    }

    /**
     * 新增流程（抽出提升可讀性）
     */
    private DataDto<Aca2003SaveResponse> create(Aca2003SavePayload p, String card) {
        // 是否已存在同 ACACardNo 且未刪除的資料
        if (repo.countActive(card) > 0) {
            return fail(MSG_DUP_ACTIVE); // 資料庫中已有相同 ACACardNo 且 IsDeleted=0 的資料時，在寫入前就回傳 MSG_DUP_ACTIVE
        }
        var e = new AcaDrugUseEntity();
        copyFieldsForCreate(p, e); // 鍵值 + 業務欄位
        e.setCreatedByUserId(p.getUserId());
        e.setCreatedOnDate(now());
        e.setIsDeleted(Boolean.FALSE);
        e.setCreatedByBranchId(repo.findCreatedByBranchIdByAcaCardNo(card)); // 由 ACABrd 帶入
        e = repo.save(e);
        return ok(e.getId(), "新增成功");
    }

    /**
     * 建立時：寫入鍵值 + 其它欄位
     */
    private static void copyFieldsForCreate(Aca2003SavePayload p, AcaDrugUseEntity e) {
        // 建立時：寫入鍵值 + 其它欄位
        e.setAcaCardNo(trim(p.getAcaCardNo()));
        e.setDrgUserText(trimToNull(p.getDrgUserText()));
        e.setOprFamilyText(trimToNull(p.getOprFamilyText()));
        e.setOprFamilyCareText(trimToNull(p.getOprFamilyCareText()));
        e.setOprSupportText(trimToNull(p.getOprSupportText()));
        e.setOprContactText(trimToNull(p.getOprContactText()));
        e.setOprReferText(trimToNull(p.getOprReferText()));
        e.setAddr(trimToNull(p.getAddr()));
        e.setOprAddr(trimToNull(p.getOprAddr()));
    }

    /**
     * 更新流程（抽出提升可讀性）
     */
    private DataDto<Aca2003SaveResponse> update(Aca2003SavePayload p, String card) {
        Optional<AcaDrugUseEntity> opt = repo.findById(p.getId());
        if (opt.isEmpty()) return fail(MSG_DATA_NOT_FOUND);

        var exist = opt.get();
        if (Boolean.TRUE.equals(exist.getIsDeleted())) {
            return fail(MSG_DATA_DELETED);
        }
        // 前端帶的鍵值需與資料庫一致（不可修改主鍵/關聯鍵）
        if (!card.equals(exist.getAcaCardNo())) {
            return fail("指定資料(id=" + p.getId() + ") 的 ACACardNo 與輸入不一致；不可修改關聯鍵。");
        }

        // 僅覆寫非鍵值欄位
        copyFieldsForUpdate(p, exist);
        exist.setModifiedByUserId(p.getUserId());
        exist.setModifiedOnDate(now());
        repo.save(exist);
        return ok(p.getId(), "更新成功");
    }

    /**
     * 更新時：不可改鍵值，只覆寫業務欄位
     */
    private static void copyFieldsForUpdate(Aca2003SavePayload p, AcaDrugUseEntity e) {
        // 更新時：不可改鍵值，只覆寫業務欄位
        e.setDrgUserText(trimToNull(p.getDrgUserText()));
        e.setOprFamilyText(trimToNull(p.getOprFamilyText()));
        e.setOprFamilyCareText(trimToNull(p.getOprFamilyCareText()));
        e.setOprSupportText(trimToNull(p.getOprSupportText()));
        e.setOprContactText(trimToNull(p.getOprContactText()));
        e.setOprReferText(trimToNull(p.getOprReferText()));
        e.setAddr(trimToNull(p.getAddr()));
        e.setOprAddr(trimToNull(p.getOprAddr()));
    }

    /**
     * SQL Server 唯一衝突（2627 unique constraint；2601 duplicate index）
     */
    private static boolean isUniqueKeyViolation(DataIntegrityViolationException ex) {
        Throwable t = ex.getMostSpecificCause();
        while (t != null) {
            if (t instanceof java.sql.SQLException) {
                java.sql.SQLException se = (java.sql.SQLException) t;
                int code = se.getErrorCode();
                if (code == 2627 || code == 2601) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }

    // ============================================================
    // query APIs
    // ============================================================

    /**
     * 依 ID 查詢詳情（含 ACABrd 左連結欄位）
     */
    @Override
    public DataDto<Aca2003QueryDto> queryById(GeneralPayload<Aca2003QueryByIdPayload> payload) {
        if (payload == null || payload.getData() == null || payload.getData().getId() == null) {
            return new DataDto<>(null, new ResponseInfo(0, MSG_ID_EMPTY));
        }
        Integer id = payload.getData().getId();

        return repo.findDetailById(id)
                .map(this::toDto)
                .map(dto -> new DataDto<>(dto, new ResponseInfo(1, "查詢成功")))
                .orElseGet(() -> new DataDto<>(null, new ResponseInfo(0, "查無資料")));
    }

    /**
     * 依 ACACardNo 取最新一筆（ID 最大）
     */
    @Override
    public DataDto<Aca2003QueryDto> queryLatestByCardNo(GeneralPayload<Aca2003QueryByCardPayload> payload) {
        if (payload == null || payload.getData() == null || isBlank(payload.getData().getAcaCardNo())) {
            return new DataDto<>(null, new ResponseInfo(0, MSG_CARD_EMPTY));
        }
        final String card = payload.getData().getAcaCardNo().trim();

        return repo.findLatestDetailByCardNo(card)
                .map(this::toDto)
                .map(dto -> new DataDto<>(dto, new ResponseInfo(1, "查詢成功")))
                .orElseGet(() -> new DataDto<>(null, new ResponseInfo(0, "查無資料")));
    }

    /**
     * 透過個人身分證號轉查 ACACardNo 並取得最新毒品濫用紀錄。
     * 步驟：
     * 1) 入口檢核 personalId 不可為空。
     * 2) 查詢 ACABrd 確認是否存在有效卡號。
     * 3) 若查到多筆卡號，回傳錯誤訊息。
     * 4) 重用 queryLatestByCardNo 確保資料查詢邏輯一致。
     */
    @Override
    public DataDto<Aca2003QueryDto> queryByPersonalId(GeneralPayload<Aca2003QueryByPersonalIdPayload> payload) {
        if (payload == null || payload.getData() == null || isBlank(payload.getData().getPersonalId())) {
            return new DataDto<>(null, new ResponseInfo(0, MSG_PERSONAL_ID_EMPTY));
        }
        final String personalId = payload.getData().getPersonalId().trim();

        // 改成查多筆
        List<String> cardNos = acaBrdRepository.findActiveCardNosByPersonalId(personalId);

        if (cardNos.isEmpty()) {
            return new DataDto<>(null, new ResponseInfo(0, MSG_CARD_BY_PERSONAL_ID_NOT_FOUND));
        }

        if (cardNos.size() > 1) {
            return new DataDto<>(null, new ResponseInfo(0, MSG_MULTIPLE_CARDS_FOUND));
        }

        // 組裝 GeneralPayload 以沿用既有的卡號查詢方法，減少重複邏輯
        Aca2003QueryByCardPayload cardPayload = new Aca2003QueryByCardPayload();
        cardPayload.setAcaCardNo(cardNos.get(0));

        GeneralPayload<Aca2003QueryByCardPayload> cardRequest = new GeneralPayload<>();
        cardRequest.setData(cardPayload);
        cardRequest.setPage(payload.getPage()); // 保留呼叫端傳入的分頁資訊（若有）

        // 轉呼叫 queryLatestByCardNo 確保一致性與既有錯誤處理邏輯
        return queryLatestByCardNo(cardRequest);
    }

    /**
     * 依 ACACardNo 取得最新 SUP_AfterCare 或毒品濫用資料對應欄位。
     * <p>
     * 流程：
     * 1) 檢核卡號並正規化空白。
     * 2) 若 AcaDrugUse 已有有效資料，直接沿用 queryLatestByCardNo。
     * 3) 若無，則以卡號查 ACABrd → 取得 NAM_IDNO → 查 SUP_AfterCare。
     * 4) 組裝 DTO（id 固定為 null，卡號回填為輸入值）。
     *
     * @param payload GeneralPayload<Aca2003QueryByCardPayload>
     * @return DataDto<Aca2003QueryDto> 組合後的後續關懷資料
     */
    @Override
    public DataDto<Aca2003QueryDto> queryDrugAfterCareByPersonalId(GeneralPayload<Aca2003QueryByCardPayload> payload) {
        if (payload == null || payload.getData() == null || isBlank(payload.getData().getAcaCardNo())) {
            return new DataDto<>(null, new ResponseInfo(0, MSG_CARD_EMPTY));
        }
        // 正規化卡號，避免後端查詢受到多餘空白影響
        final String cardNo = payload.getData().getAcaCardNo().trim();
        payload.getData().setAcaCardNo(cardNo);

        // 若 AcaDrugUse 已有有效資料，直接沿用既有查詢流程，確保行為一致
        if (repo.existsActiveByCardNo(cardNo) > 0) {
            return queryLatestByCardNo(payload);
        }

        // 回退 SUP_AfterCare 流程：先依卡號取 ACABrd 基本資料
        Optional<AcaBrdEntity> acaOpt = acaBrdRepository.findTopActiveByAcaCardNo(cardNo);
        if (acaOpt.isEmpty()) {
            return new DataDto<>(null, new ResponseInfo(0, MSG_CARD_NOT_FOUND));
        }
        var aca = acaOpt.get();

        // NAM_IDNO 為查詢後續關懷的關鍵欄位，若缺失則視為資料品質問題
        String namIdNo = trim(aca.getAcaIdNo());
        if (isBlank(namIdNo)) {
            return new DataDto<>(null, new ResponseInfo(0, MSG_NAM_IDNO_EMPTY));
        }

        Optional<SupAfterCareEntity> afterCareOpt = supAfterCareRepository.findTopByNamIdNoOrderByCrDateTimeDesc(namIdNo);
        if (afterCareOpt.isEmpty()) {
            return new DataDto<>(null, new ResponseInfo(0, MSG_AFTER_CARE_NOT_FOUND));
        }
        var afterCare = afterCareOpt.get();

        // 組裝回傳 DTO，id 固定為 null（避免誤以為來自 AcaDrugUse）
        Aca2003QueryDto dto = new Aca2003QueryDto();
        dto.setId(null);
        dto.setCreatedOnDate(toTimestamp(afterCare.getCrDateTime()));
        dto.setCreatedByBranchName(trim(afterCare.getProtName()));
        dto.setDrgUserText(trimToNull(afterCare.getDrgUserText()));
        dto.setOprFamilyText(trimToNull(afterCare.getOprFamilyText()));
        dto.setOprFamilyCareText(trimToNull(afterCare.getOprFamilyCareText()));
        dto.setOprSupportText(trimToNull(afterCare.getOprSupportText()));
        dto.setOprContactText(trimToNull(afterCare.getOprContactText()));
        dto.setOprReferText(trimToNull(afterCare.getOprReferText()));
        dto.setAddr(trimToNull(afterCare.getAddr()));
        dto.setOprAddr(trimToNull(afterCare.getOprAddr()));
        dto.setAcaCardNo(cardNo);
        dto.setAcaName(trim(aca.getAcaName()));
        dto.setAcaIdNo(trim(aca.getAcaIdNo()));

        return new DataDto<>(dto, new ResponseInfo(1, "查詢成功"));
    }

    /**
     * 將 Projection 轉為 DTO（Controller 統一輸出此 DTO）
     */
    private Aca2003QueryDto toDto(Aca2003DetailView v) {
        // 將 Repository 投影轉換為 DTO，分會欄位已改為呈現 Lists.Text
        return new Aca2003QueryDto(
                v.getId(),
                v.getCreatedOnDate(),
                v.getCreatedByBranchName(),
                v.getDrgUserText(),
                v.getOprFamilyText(),
                v.getOprFamilyCareText(),
                v.getOprSupportText(),
                v.getOprContactText(),
                v.getOprReferText(),
                v.getAddr(),
                v.getOprAddr(),
                v.getAcaCardNo(),
                v.getAcaName(),
                v.getAcaIdNo()
        );
    }

    // ============================================================
    // delete API：軟刪除
    // ============================================================

    /**
     * 軟刪除（isDeleted=1），保留資料於 DB，不做物理刪除。
     */
    @Override
    @Transactional
    public DataDto<Aca2003SaveResponse> softDelete(GeneralPayload<Aca2003DeletePayload> payload) {
        if (payload == null || payload.getData() == null) {
            return new DataDto<>(null, new ResponseInfo(0, MSG_PAYLOAD_EMPTY));
        }

        var p = payload.getData();
        if (p.getId() == null) return new DataDto<>(null, new ResponseInfo(0, MSG_ID_EMPTY));
        if (p.getUserId() == null) return new DataDto<>(null, new ResponseInfo(0, MSG_USER_ID_EMPTY));

        Optional<AcaDrugUseEntity> opt = repo.findById(p.getId());
        if (opt.isEmpty()) {
            return new DataDto<>(null, new ResponseInfo(0, MSG_DATA_NOT_FOUND));
        }

        var exist = opt.get();
        if (Boolean.TRUE.equals(exist.getIsDeleted())) {
            return new DataDto<>(null, new ResponseInfo(0, MSG_DATA_DELETED));
        }

        exist.setIsDeleted(Boolean.TRUE);
        exist.setModifiedByUserId(p.getUserId());
        exist.setModifiedOnDate(now());
        repo.save(exist);

        return new DataDto<>(new Aca2003SaveResponse(exist.getId()), new ResponseInfo(1, "刪除成功"));
    }

    // ============================================================
    // Helpers：時間、字串、錯誤轉換、回應包裝
    // ============================================================

    /**
     * 統一成功回傳包裝
     */
    private static DataDto<Aca2003SaveResponse> ok(Integer id, String msg) {
        return new DataDto<>(new Aca2003SaveResponse(id), new ResponseInfo(1, msg));
    }

    /**
     * 統一失敗回傳包裝
     */
    private static DataDto<Aca2003SaveResponse> fail(String msg) {
        return new DataDto<>(null, new ResponseInfo(0, msg));
    }

    /**
     * 產生當前時間戳（集中一處，便於日後替換 Clock/TimeProvider）
     */
    private static Timestamp now() {
        return new Timestamp(System.currentTimeMillis());
    }

    /**
     * 將 LocalDate 轉為 Timestamp，統一集中轉換邏輯。
     */
    private static Timestamp toTimestamp(LocalDate date) {
        return date == null ? null : Timestamp.valueOf(date.atStartOfDay());
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trim(String s) {
        return (s == null) ? null : s.trim();
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }


}
