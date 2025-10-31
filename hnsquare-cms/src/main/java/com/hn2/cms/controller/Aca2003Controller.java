package com.hn2.cms.controller;

import com.hn2.cms.dto.aca2003.Aca2003QueryDto;
import com.hn2.cms.dto.aca2003.Aca2003SaveResponse;
import com.hn2.cms.payload.aca2003.Aca2003DeletePayload;
import com.hn2.cms.payload.aca2003.Aca2003QueryByCardPayload;
import com.hn2.cms.payload.aca2003.Aca2003QueryByIdPayload;
import com.hn2.cms.payload.aca2003.Aca2003QueryByPersonalIdPayload;
import com.hn2.cms.payload.aca2003.Aca2003SavePayload;
import com.hn2.cms.service.aca2003.Aca2003Service;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * Aca2003Controller
 * <p>
 * 功能：提供「毒品濫用資料表 (AcaDrugUse)」的 API 入口
 * - /save       → 建立或更新資料
 * - /queryById     → 依 ID 查詢單筆詳情
 * - /queryByCardNo → 依 ACACardNo 查詢最新一筆資料
 * - /softDelete → 軟刪除 (isDeleted=1)
 */
@RestController
@RequestMapping("/aca/aca2003")
public class Aca2003Controller {
    private final Aca2003Service service;

    @Autowired
    public Aca2003Controller(Aca2003Service service) {
        this.service = service;
    }

    @PostMapping("/save")
    public DataDto<Aca2003SaveResponse> save(@RequestBody GeneralPayload<Aca2003SavePayload> payload) {
        return service.save(payload);
    }

    /**
     * 查詢詳情 (依 ID)
     * - 用於取得單筆毒品濫用紀錄
     *
     * @param payload GeneralPayload<Aca2003QueryPayload>
     * @return DataDto<Aca2003QueryDto> 查詢結果 DTO
     */
    @PostMapping("/queryById")
    public ResponseEntity<DataDto<Aca2003QueryDto>> queryById(@Valid @RequestBody GeneralPayload<Aca2003QueryByIdPayload> payload) {
        return ResponseEntity.ok(service.queryById(payload));
    }

    /**
     * 查詢最新紀錄 (依 ACACardNo)
     * - 同一 ACACardNo 可能有多筆資料
     * - 只取 ID 最大的那一筆 (視為最新紀錄)
     *
     * @param payload GeneralPayload<Aca2003QueryByCardPayload>
     * @return DataDto<Aca2003QueryDto> 最新一筆紀錄 DTO
     */
    @PostMapping("/queryByCardNo")
    public ResponseEntity<DataDto<Aca2003QueryDto>> queryByCardNo(@Valid @RequestBody GeneralPayload<Aca2003QueryByCardPayload> payload) {
        return ResponseEntity.ok(service.queryLatestByCardNo(payload));
    }

    /**
     * 查詢最新紀錄 (依 personalId 轉卡號)
     * - 先以身分證號對 ACABrd 取得 ACACardNo
     * - 再重用卡號查詢服務取得最新紀錄
     *
     * @param payload GeneralPayload<Aca2003QueryByPersonalIdPayload>
     * @return DataDto<Aca2003QueryDto> 最新一筆紀錄 DTO
     */
    @PostMapping("/queryByPersonalId")
    public ResponseEntity<DataDto<Aca2003QueryDto>> queryByPersonalId(@Valid @RequestBody GeneralPayload<Aca2003QueryByPersonalIdPayload> payload) {
        // 服務層會轉查 ACACardNo 並重用 queryLatestByCardNo 邏輯
        return ResponseEntity.ok(service.queryByPersonalId(payload));
    }

    /**
     * 查詢後續關懷資料 (依 ACACardNo)
     * - 先檢查該卡號是否已有 AcaDrugUse 有效資料
     * - 有：直接沿用毒品濫用既有查詢；無：改以卡號映射 SUP_AfterCare
     *
     * @param payload GeneralPayload<Aca2003QueryByCardPayload>
     * @return DataDto<Aca2003QueryDto> 後續關懷欄位 DTO
     */
    @PostMapping("/queryDrugAfterCareByPersonalId")
    public ResponseEntity<DataDto<Aca2003QueryDto>> queryDrugAfterCareByPersonalId(@Valid @RequestBody GeneralPayload<Aca2003QueryByCardPayload> payload) {
        // 服務層會先檢核卡號是否已有毒品濫用紀錄，必要時再回退 SUP_AfterCare
        return ResponseEntity.ok(service.queryDrugAfterCareByPersonalId(payload));
    }

    /**
     * 軟刪除 (Soft Delete)
     * - 將指定紀錄的 isDeleted 欄位設為 1
     * - 保留資料於 DB，不做物理刪除
     *
     * @param payload GeneralPayload<Aca2003DeletePayload>
     * @return DataDto<Aca2003SaveResponse> 已刪除紀錄 ID 與訊息
     */
    @PostMapping("/softDelete")
    public ResponseEntity<DataDto<Aca2003SaveResponse>> softDelete(@Valid @RequestBody GeneralPayload<Aca2003DeletePayload> payload) {
        return ResponseEntity.ok(service.softDelete(payload));
    }
}
