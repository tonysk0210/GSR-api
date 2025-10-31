package com.hn2.cms.repository;

import com.hn2.cms.model.SupAfterCareEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupAfterCareRepository extends JpaRepository<SupAfterCareEntity, String> {

    /**
     * 依 NAM_IDNO 取得最新一筆後續關懷資料，供 Aca2003 後續查詢使用。
     *
     * @param namIdNo 更生人身分證號
     * @return Optional<SupAfterCareEntity> 最新一筆 SUP_AfterCare 資料
     */
    Optional<SupAfterCareEntity> findTopByNamIdNoOrderByCrDateTimeDesc(String namIdNo);
}
