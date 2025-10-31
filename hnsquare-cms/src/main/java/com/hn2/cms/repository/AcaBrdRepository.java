package com.hn2.cms.repository;

import com.hn2.cms.model.AcaBrdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AcaBrdRepository extends JpaRepository<AcaBrdEntity, String> {
    Optional<AcaBrdEntity> findByAcaIdNo(String acaIdNo);

    /**
     * 依 ACAIDNo 取得有效（isDeleted=0）的 ACACardNo，供應用層轉查個案資料。
     */
    @Query(value = "SELECT ACACardNo FROM dbo.ACABrd WITH (NOLOCK) WHERE ACAIDNo = :personalId AND (IsDeleted = 0 OR IsDeleted IS NULL) ORDER BY CreatedOnDate DESC", nativeQuery = true)
    List<String> findActiveCardNosByPersonalId(@Param("personalId") String personalId);

    /**
     * 依 ACAIDNo 取得最新一筆有效個案資料，提供後續 NAM_IDNO 映射 SUP_AfterCare。
     */
    @Query(value = "SELECT TOP 1 * FROM dbo.ACABrd WITH (NOLOCK) WHERE ACAIDNo = :personalId AND (IsDeleted = 0 OR IsDeleted IS NULL) ORDER BY CreatedOnDate DESC", nativeQuery = true)
    Optional<AcaBrdEntity> findTopActiveByPersonalId(@Param("personalId") String personalId);

    /**
     * 依 ACACardNo 取得最新一筆有效個案資料，供卡號反查 SUP_AfterCare。
     */
    @Query(value = "SELECT TOP 1 * FROM dbo.ACABrd WITH (NOLOCK) WHERE ACACardNo = :cardNo AND (IsDeleted = 0 OR IsDeleted IS NULL) ORDER BY CreatedOnDate DESC", nativeQuery = true)
    Optional<AcaBrdEntity> findTopActiveByAcaCardNo(@Param("cardNo") String cardNo);

    @Query(value = "select RIGHT(REPLICATE('0', 5) + CAST( isnull(SUBSTRING(max(id),8,5),0)        + 1 AS VARCHAR(5)), 5) as id     from ACABrd where id like :findKey", nativeQuery = true)
    String genNewId(String findKey);

    @Query(value = "select RIGHT(REPLICATE('0', 4) + CAST( isnull(SUBSTRING(max(ACACardNo),4,4),0) + 1 AS VARCHAR(4)), 4)  as aca_id from ACABrd where ACACardNo like :findKey", nativeQuery = true)
    String genNewAcaCardNo(String findKey);

}
