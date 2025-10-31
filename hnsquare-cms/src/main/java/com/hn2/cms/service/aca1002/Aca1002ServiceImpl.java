package com.hn2.cms.service.aca1002;

import com.hn2.cms.dto.aca1002.Aca1002ComparyAcaDto;
import com.hn2.cms.dto.aca1002.Aca1002QueryDto;
import com.hn2.cms.model.AcaBrdEntity;
import com.hn2.cms.model.SupAfterCareEntity;
import com.hn2.cms.payload.aca2001.Aca2001SavePayload;
import com.hn2.cms.payload.aca1002.*;
import com.hn2.cms.repository.aca1002.Aca1002Repository;
import com.hn2.cms.repository.AcaBrdRepository;
import com.hn2.cms.repository.SupAfterCareRepository;
import com.hn2.core.dto.DataDto;
import com.hn2.core.dto.PageInfo;
import com.hn2.core.dto.ResponseInfo;
import com.hn2.core.payload.GeneralPayload;
import com.hn2.core.util.PagePayloadValidator;
import com.hn2.util.BusinessException;
import com.hn2.util.ErrorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class Aca1002ServiceImpl implements Aca1002Service {
    @Autowired
    PagePayloadValidator pagePayloadValidator;
    @Autowired
    Aca1002Repository Aca1002Repository;
    @Autowired
    SupAfterCareRepository supAfterCareRepository;
    @Autowired
    AcaBrdRepository acaBrdRepository;

    @Override
    public DataDto<List<Aca1002QueryDto>> queryList(GeneralPayload<Aca1002QueryPayload> payload) {
        var dataPayload = payload.getData();
        var pagePayload = payload.getPage();

        int count = Aca1002Repository.countSearch(dataPayload);
        if (pagePayload != null && !pagePayloadValidator.checkPageExist(pagePayload, count))
            throw new BusinessException(ErrorType.RESOURCE_NOT_FOUND, "請求分頁不存在");

        var dataList = Aca1002Repository.queryList(dataPayload, pagePayload);

        PageInfo pageInfo = new PageInfo();
        pageInfo.setTotalDatas((long) count);
        if(pagePayload != null){
            pageInfo.setCurrentPage(pagePayload.getPage());
            pageInfo.setPageItems(pagePayload.getPageSize());
            int i = count % pagePayload.getPageSize() == 0 ? 0 : 1;
            pageInfo.setTotalPages(count / pagePayload.getPageSize() + i);
        }

        return new DataDto<>(dataList, pageInfo, new ResponseInfo(1, "查詢成功"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataDto<Void> signList(GeneralPayload<Aca1002SignPayload> payload) {
        Aca1002SignPayload payloadData = payload.getData();
        List<SupAfterCareEntity> entityList = supAfterCareRepository.findAllById(payloadData.getItemIdList());

        for (SupAfterCareEntity v : entityList){
            if("0".equals(v.getAcaState()) || v.getAcaState() == null){
                v.setAcaReceiptDate(payloadData.getAcaReceiptDate());
                v.setAcaUser(payloadData.getAcaUser());
                v.setAcaState("1");


                //todo 需要寫入正式資料
                //insertAca(v.getId());
            }
        }

        supAfterCareRepository.saveAll(entityList);

        return new DataDto<>(null, new ResponseInfo(1, "儲存成功"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataDto<Void> transPort(GeneralPayload<Aca1002TransPortPayload> payload) {
        var payloadData = payload.getData();
        var entityList = supAfterCareRepository.findAllById(payloadData.getItemIdList());

        for (var v : entityList) {
            v.setSignProtName(payloadData.getSignProtName());
            v.setSignProtNo(payloadData.getSignProtNo());
            v.setAcaReceiptDate(null);
            v.setAcaUser(null);
            v.setAcaState("0");

            v.setSignDate(null);
            v.setSignUser(null);
            v.setSignState("0");

        };

        supAfterCareRepository.saveAll(entityList);

        return new DataDto<>(null, new ResponseInfo(1, "儲存成功"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataDto<Void> goBack(GeneralPayload<Aca1002GoBackPayload> payload) {
        var payloadData = payload.getData();
        var entityList = supAfterCareRepository.findAllById(payloadData.getItemIdList());

        for (var v : entityList) {
            v.setAcaReceiptDate(null);
            v.setAcaUser(null);
            v.setAcaState("0");
        };

        supAfterCareRepository.saveAll(entityList);

        return new DataDto<>(null, new ResponseInfo(1, "儲存成功"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataDto<Void> reassign(GeneralPayload<Aca1002ReassignPayload> payload) {
        var payloadData = payload.getData();
        var entityList = supAfterCareRepository.findAllById(payloadData.getItemIdList());

        for (var v : entityList) {
            v.setAcaReceiptDate(null);
            v.setAcaUser(payloadData.getAcaUser());
            v.setAcaState("0");

        };

        supAfterCareRepository.saveAll(entityList);

        return new DataDto<>(null, new ResponseInfo(1, "儲存成功"));
    }

    @Override
    public DataDto<Aca1002ComparyAcaDto> compareAca(GeneralPayload<Aca1002CompareAcaPayload> payload) {
        Aca1002CompareAcaPayload dataPayload = payload.getData();
        String itemId = dataPayload.getItemId();

        //1.查詢出A:矯正署資料 透過 itemId 查矯正署資料
        SupAfterCareEntity namData = supAfterCareRepository.findById(itemId).orElseThrow( () -> new BusinessException(("查不到資料")));

        //2.查詢出B:個案資料 鈄過查矯正署資料 身分證及簽收機關查詢個案
        Optional<AcaBrdEntity> acaData = acaBrdRepository.findByAcaIdNo( namData.getNamIdNo());


        Aca1002ComparyAcaDto ComparyAca = new Aca1002ComparyAcaDto();
        ComparyAca.setNam(namData);
        if (acaData.isEmpty()){
            ComparyAca.setAca(null);
        } else {
            ComparyAca.setAca(acaData.get());
        }



        return new DataDto<Aca1002ComparyAcaDto>(ComparyAca,null, new ResponseInfo(1, "查詢成功"));
    }

    @Override
    public DataDto<Void> save(GeneralPayload<Aca2001SavePayload> payload) {
        Aca2001SavePayload dataPayload = payload.getData();
        String itemId = dataPayload.getNam().getItemId();

        //1.查詢出A:矯正署資料 透過 itemId 查矯正署資料
        SupAfterCareEntity namData = supAfterCareRepository.findById(itemId).orElseThrow( () -> new BusinessException(("查不到資料")));
        //2.查詢出B:個案資料 鈄過查矯正署資料 身分證及簽收機關查詢個案
        AcaBrdEntity acaData = (AcaBrdEntity) acaBrdRepository.findByAcaIdNo( namData.getNamIdNo())
                .orElseThrow( () -> new BusinessException(("查不到資料")));


        namData.setAcaState("3");
        namData.setSignState("3");
        namData.setUpUser(acaData.getModifiedByUserId());
        namData.setUpDateTime(LocalDate.now());

        supAfterCareRepository.save(namData);




//        Aca1002ComparyAcaDto ComparyAca = new Aca1002ComparyAcaDto();
//        ComparyAca.setNam(namData);
//        ComparyAca.setAca(acaData);


        return new DataDto<>(null, new ResponseInfo(1, "儲存成功"));
    }


}
