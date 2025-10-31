package com.hn2.cms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper=false)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table
@Entity(name = "ACABrd")
@EntityListeners(AuditingEntityListener.class)
public class AcaBrdEntity {


    // 個案代碼
    @Id
    @Column(name = "ID")
    private String id;

    // 建檔編號
    @Column(name = "ACACardNo")
    private String acaCardNo;

    @Column(name = "FamCardNo")
    private String famCardNo;

    // 國民身分證統一編號
    @Column(name = "ACAIDNo")
    private String acaIdNo;
    
    // 國籍
    @Column(name = "ACA_Nationality")
    private String acaNationality;

    // 護照(居留證)號碼
    @Column(name = "ACA_Passport")
    private String acaPassport;

    // 姓名
    @Column(name = "ACAName")
    private String acaName;

    // 性別
    @Column(name = "ACASex")
    private String acaSex;

    // 出生日期
    @Column(name = "ACABirth")
    private LocalDate acaBirth;

    // 出生地(城市)
    @Column(name = "ACAArea")
    private String acaArea;

    // 電話1
    @Column(name = "ACATel")
    private String acaTel;

    // 行動電話1
    @Column(name = "ACAMobile")
    private String acaMobile;

    // 電話2
    @Column(name = "ACATel2")
    private String acaTel2;

    // 行動電話2
    @Column(name = "ACAMobile2")
    private String acaMobile2;

    // E-mail
    @Column(name = "ACAEmail")
    private String acaEmail;

    // 傳真
    @Column(name = "ACAFax")
    private String acaFax;

    // 居住地地址
    @Column(name = "ResidenceAddress")
    private String residenceAddress;

    // 居住地郵遞區號
    @Column(name = "ResidencePostal")
    private String residencePostal;

    // 戶籍地地址
    @Column(name = "PermanentAddress")
    private String permanentAddress;

    // 戶籍地郵遞區號
    @Column(name = "PermanentPostal")
    private String permanentPostal;

    // 緊急聯絡人姓名
    @Column(name = "ACALiaison")
    private String acaLiaison;

    // 緊急聯絡人電話
    @Column(name = "ACALiaisonTel")
    private String acaLiaisonTel;

    // 緊急聯絡人關係
    @Column(name = "ACALiaisonRelation")
    private String acaLiaisonRelation;

    // 聯絡人手機
    @Column(name = "ACALiaisonMobile")
    private String acaLiaisonMobile;

    // 聯絡人地址
    @Column(name = "ACALiaisonAddr")
    private String acaLiaisonAddr;

    // 教育程度
    @Column(name = "ACAEdu")
    private String acaEdu;

    // 婚姻狀況
    @Column(name = "ACAMarry")
    private String acaMarry;

    // 宗教
    @Column(name = "ACARelig")
    private String acaRelig;

    // 職業
    @Column(name = "ACACareer")
    private String acaCareer;

    // 專長或其他工作經驗
    @Column(name = "ACASkill")
    private String acaSkill;

    // 其他
    @Column(name = "ACAOther")
    private String acaOther;

    // 家庭經濟狀況
    @Column(name = "ACA_Economic")
    private String acaEconomic;

    // 家庭狀況
    @Column(name = "ACAHome")
    private String acaHome;

    // 興趣嗜好
    @Column(name = "ACA_Interest")
    private String acaInterest;

    // 黑名單
    @Column(name = "IsBlackList")
    private String isBlackList;

    // 資料所屬分會
    @Column(name = "CreatedByBranchID")
    private String createdByBranchId;

    // 建檔人員
    @Column(name = "CreatedByUserID")
    private String createdByUserId;

    // 建檔時間
    @Column(name = "CreatedOnDate")
    private LocalDate createdOnDate;

    // 修檔人員
    @Column(name = "ModifiedByUserID")
    private String modifiedByUserId;

    // 修檔時間
    @Column(name = "ModifiedOnDate")
    private LocalDate modifiedOnDate;

    // 刪除註記
    @Column(name = "IsDeleted")
    private int isDeleted;

    // 塗銷註記
    @Column(name = "isERASE")
    private int isErase;

}
