package com.hn2.cms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper=false)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table
@Entity(name = "SUP_AfterCare")
@EntityListeners(AuditingEntityListener.class)
public class SupAfterCareEntity {

    /** uuid */
    @Id
    @GenericGenerator(name = "jpa-uuid", strategy = "uuid2")
    @GeneratedValue(generator = "jpa-uuid")
    @Column(name = "Id", columnDefinition = "uniqueidentifier")
    private String id;

    /** 機關代碼 */
    @Column(name = "ORG_CODE")
    private String orgCode;

    /** 虛擬編號 */
    @Column(name = "VIR_NO")
    private Long virNo;

    /** 發文日期 */
    @Column(name = "RS_DT")
    private LocalDate rsDt;

    /** 承辦人 */
    @Column(name = "TR_USER_NAME")
    private String trUserName;

    /** 承辦人聯絡電話 */
    @Column(name = "TR_TEL")
    private String trTel;

    /** 承辦人電子郵件 */
    @Column(name = "TR_EMAIL")
    private String trEmail;

    /** 更生保護分會 */
    @Column(name = "PROT_No")
    private String protNo;

    /** 更生保護分會 */
    @Column(name = "PROT_NAME")
    private String protName;

    /** 呼號 */
    @Column(name = "CALL_NO")
    private String callNo;

    /** 姓名 */
    @Column(name = "NAM_CNAME")
    private String namCname;

    /** 性別 (M=>男、F=>女) */
    @Column(name = "NAM_SEX")
    private String namSex;

    /** 出生日期 */
    @Column(name = "NAM_BRDT")
    private LocalDate namBrdt;

    /** 出生地 */
    @Column(name = "NAM_BONP_TEXT")
    private String namBonpText;

    /** 身分證號 */
    @Column(name = "NAM_IDNO")
    private String namIdNo;

    /** 戶籍地址 */
    @Column(name = "NAM_HADDR_TEXT")
    private String namHaddrText;

    /** 聯絡電話 */
    @Column(name = "NAM_TEL")
    private String namTel;

    /** 以前職業 */
    @Column(name = "DOCU_PROC_TEXT")
    private String docuProcText;

    /** 教育程度 */
    @Column(name = "NAM_EDUC_TEXT")
    private String namEducText;

    /** 罪名 */
    @Column(name = "NAM_CNAMES_TEXT")
    private String namCnamesText;

    /** 刑期 */
    @Column(name = "NAM_PEN_TEXT")
    private String namPenText;

    /** 入監或被收容日期 */
    @Column(name = "NAM_MVDT")
    private LocalDate namMvdt;

    /** 預定獲釋日期 */
    @Column(name = "DOCU_OTDT")
    private LocalDate docuOtdt;

    /** 預定獲釋原因 */
    @Column(name = "DOCU_OTOP_TEXT")
    private String docuOtopText;

    /** 專長 */
    @Column(name = "SKILL_TEXT")
    private String skillText;

    /** 需要何種更生保護 */
    @Column(name = "PROTECT_TEXT")
    private String protectText;

    /** 取得證照職種 */
    @Column(name = "DOCU_LICENSE_TEXT")
    private String docuLicenseText;

    /** 宗教信仰 */
    @Column(name = "RELIG_TEXT")
    private String religText;

    /** 是否有暴力傾向 */
    @Column(name = "DOCU_VIOLENT_TEXT")
    private String docuViolentText;

    /** 經濟狀況 */
    @Column(name = "ECONOMIC_TEXT")
    private String economicText;

    /** 婚姻狀況 */
    @Column(name = "MARRIAGE_TEXT")
    private String marriageText;

    /** 其他(補充事項) */
    @Column(name = "DOCU_REMARK")
    private String docuRemark;

    /** 聯絡人姓名 */
    @Column(name = "RELD_NAME")
    private String reldName;

    /** 聯絡人關係 */
    @Column(name = "RELD_NO_TEXT")
    private String reldNoText;

    /** 聯絡人電話 */
    @Column(name = "RELD_TEL1")
    private String reldTel1;

    /** 聯絡人手機 */
    @Column(name = "RELD_TEL2")
    private String reldTel2;

    /** 聯絡人地址 */
    @Column(name = "RELD_ADDR")
    private String reldAddr;

    /** 毒品使用情形描述 */
    @Column(name = "DRG_USER_TEXT")
    private String drgUserText;

    /** 家庭情形描述 */
    @Column(name = "OPR_FAMILY_TEXT")
    private String oprFamilyText;

    /** 家庭照顧情況描述 */
    @Column(name = "OPR_FAMILYCARE_TEXT")
    private String oprFamilyCareText;

    /** 支持網絡情況描述 */
    @Column(name = "OPR_SUPPORT_TEXT")
    private String oprSupportText;

    /** 聯絡情況描述 */
    @Column(name = "OPR_CONTACT_TEXT")
    private String oprContactText;

    /** 轉介情況描述 */
    @Column(name = "OPR_REFER_TEXT")
    private String oprReferText;

    /** 居住地址 */
    @Column(name = "ADDR")
    private String addr;

    /** 其他地址／接觸地點 */
    @Column(name = "OPR_ADDR")
    private String oprAddr;

    /** 簽收狀態(0:未簽收 1:已簽收) */
    @Column(name = "SIGN_STATE")
    private String signState;

    /** 簽收分會 (預設同[更生保護分會]欄位) */
    @Column(name = "SIGN_PROT_NO")
    private String signProtNo;

    /** 簽收分會 (預設同[更生保護分會]欄位) */
    @Column(name = "SIGN_PROT_NAME")
    private String signProtName;

    /** 簽收日期 */
    @Column(name = "SIGN_DATE")
    private LocalDate signDate;

    /** 簽收人員 */
    @Column(name = "SIGN_USER")
    private String signUser;

    /** 承辦人 */
    @Column(name = "ACA_USER")
    private String acaUser;

    /** 承辦人簽收日期(收文日期) */
    @Column(name = "ACA_RECEIPT_DATE")
    private LocalDate acaReceiptDate;

    /**承辦人簽收狀態 0=>未介簽收 1=>已簽收)*/
    @Column(name = "ACA_STATE")
    private String acaState;


    /** 是否介接更生保護會(0=>未介接1=>已介接) */
    @Column(name = "UPLOAD_STATE")
    private String uploadState;

    /** 新增人員代碼 */
    @Column(name = "CR_USER")
    private String crUser;

    /** 新增日期(收文日期) */
    @Column(name = "CR_DATE_TIME")
    private LocalDate crDateTime;

    /** 新增電腦IP */
    @Column(name = "CR_IP")
    private String crIp;

    /** 修改人員代碼 */
    @Column(name = "UP_USER")
    private String upUser;

    /** 修改日期 */
    @Column(name = "UP_DATE_TIME")
    private LocalDate upDateTime;

    /** 修改電腦IP */
    @Column(name = "UP_IP")
    private String upIp;
//
}
