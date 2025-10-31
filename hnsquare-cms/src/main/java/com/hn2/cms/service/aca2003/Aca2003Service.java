package com.hn2.cms.service.aca2003;


import com.hn2.cms.dto.aca2003.Aca2003QueryDto;
import com.hn2.cms.dto.aca2003.Aca2003SaveResponse;
import com.hn2.cms.payload.aca2003.Aca2003DeletePayload;
import com.hn2.cms.payload.aca2003.Aca2003QueryByCardPayload;
import com.hn2.cms.payload.aca2003.Aca2003QueryByIdPayload;
import com.hn2.cms.payload.aca2003.Aca2003QueryByPersonalIdPayload;
import com.hn2.cms.payload.aca2003.Aca2003SavePayload;
import com.hn2.core.dto.DataDto;
import com.hn2.core.payload.GeneralPayload;

public interface Aca2003Service {
    DataDto<Aca2003SaveResponse> save(GeneralPayload<Aca2003SavePayload> payload);

    DataDto<Aca2003QueryDto> queryById(GeneralPayload<Aca2003QueryByIdPayload> payload);

    DataDto<Aca2003QueryDto> queryLatestByCardNo(GeneralPayload<Aca2003QueryByCardPayload> payload);

    /**
     * 依個人身分證號（personalId）轉查 ACABrd 之 ACACardNo，並返回最新的毒品濫用紀錄。
     *
     * @param payload GeneralPayload<Aca2003QueryByPersonalIdPayload>
     * @return DataDto<Aca2003QueryDto> 轉查後最新一筆紀錄
     */
    DataDto<Aca2003QueryDto> queryByPersonalId(GeneralPayload<Aca2003QueryByPersonalIdPayload> payload);

    /**
     * 依 ACACardNo 自動套用毒品濫用紀錄與後續關懷回退邏輯：
     * 1) 若卡號已有 AcaDrugUse 有效資料，沿用最新卡號查詢結果；
     * 2) 若無，則以卡號映射 ACABrd 與 SUP_AfterCare 取得回退資料。
     *
     * @param payload GeneralPayload<Aca2003QueryByCardPayload>
     * @return DataDto<Aca2003QueryDto> 自動回退後的後續關懷 DTO
     */
    DataDto<Aca2003QueryDto> queryAfterCareAutoFallbackByCardNo(GeneralPayload<Aca2003QueryByCardPayload> payload);

    DataDto<Aca2003SaveResponse> softDelete(GeneralPayload<Aca2003DeletePayload> payload);
}
