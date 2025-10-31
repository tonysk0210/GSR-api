package com.hn2.cms.payload.aca2003;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Aca2003QueryByCardPayload
 * <p>
 * 提供 ACACardNo 查詢毒品濫用或後續關懷資料時的請求結構。
 */
@Data
public class Aca2003QueryByCardPayload {
    @NotBlank
    private String acaCardNo;
}
