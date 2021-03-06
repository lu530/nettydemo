package com.suntek.efacecloud.service;

import com.suntek.eap.common.CommandContext;
import com.suntek.eap.log.ServiceLog;
import com.suntek.eap.pico.annotation.BeanService;
import com.suntek.eap.pico.annotation.LocalComponent;
import com.suntek.eap.util.StringUtil;
import com.suntek.eap.web.RequestContext;
import com.suntek.eaplet.registry.Registry;
import com.suntek.efacecloud.service.face.tactics.async.FrequentAccessAsyncService;
import com.suntek.efacecloud.service.face.tactics.common.FrequentAccessCommonService;
import com.suntek.efacecloud.util.ConfigUtil;
import com.suntek.sp.common.common.BaseCommandEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 人员技战法-频繁出入
 *
 * @author swq
 * @version 2017年07月17日
 */
@LocalComponent(id = "technicalTactics/frequencyAccess")
public class FrequentAccessTacticsService extends FrequentAccessCommonService {

    private FrequentAccessAsyncService asyncService = new FrequentAccessAsyncService();

    @SuppressWarnings("unchecked")
    @BeanService(id = "query", description = "频繁出入查询", since = "1.0.0")
    public void query(RequestContext context) throws Exception {
        Map<String, Object> params = context.getParameters();
        params.put("ALGO_TYPE", ConfigUtil.getAlgoType());
        if (ConfigUtil.getIsNvnAsync()) {
            this.asyncService.query(context);
            return;
        }
        CommandContext commandContext = new CommandContext(context.getHttpRequest());

        String vendor = ConfigUtil.getVendor();

        try {
            commandContext.setOrgCode(context.getUser().getDepartment().getCivilCode());
        } catch (Exception e) {
            ServiceLog.debug("外部调用");
        }



        commandContext.setBody(params);

        ServiceLog.debug("调用sdk参数:" + params);

        Registry registry = Registry.getInstance();

        registry.selectCommand(BaseCommandEnum.frequentAccess.getUri(), "4401",
                vendor).exec(commandContext);

        ServiceLog.debug("调用sdk返回结果code:" + commandContext.getResponse().getCode() + " message:"
                + commandContext.getResponse().getMessage() + " result:" + commandContext.getResponse().getResult());

        long code = commandContext.getResponse().getCode();

        if (0L != code) {
            context.getResponse().setWarn(commandContext.getResponse().getMessage());
            return;
        }

        List<List<Object>> personIds = (List<List<Object>>) commandContext.getResponse().getData("DATA");

        List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();// 返回到前端的结果集
        for (int i = 0; i < personIds.size(); i++) {
            List<Object> ids;
            //huawei-service-proxy返回结构（map）跟pci-service-proxy的返回结构（list）不一致
            if (personIds.get(i) instanceof HashMap) {
                HashMap map = (HashMap) personIds.get(i);

                map.put("PIC",map.get("OBJ_PIC"));
                map.put("FACE_SCORE","0");
                resultList.add(map);
            } else {
                ids = personIds.get(i); // 一个人员出现列表的主键id集合
                resultList.add(handlePersonId(ids));
            }
        }

        context.getResponse().putData("DATA", resultList);

    }

    @BeanService(id = "getMsgByIds", description = "获取一个人员的所有信息", since = "1.0.0")
    public void getMsgByIds(RequestContext context) throws Exception {

        String ids = StringUtil.toString(context.getParameter("IDS"));
        CommandContext commandContext = new CommandContext(context.getHttpRequest());

        commandContext.setOrgCode(context.getUser().getDepartment().getCivilCode());

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("IDS", ids);
        commandContext.setBody(params);

        ServiceLog.debug("调用sdk参数:" + params);

        String vendor = ConfigUtil.getVendor();

        Registry registry = Registry.getInstance();

        registry.selectCommand(BaseCommandEnum.faceQueryByIds.getUri(), "4401",
                vendor).exec(commandContext);

        ServiceLog.debug("调用sdk返回结果code:" + commandContext.getResponse().getCode() + " message:"
                + commandContext.getResponse().getMessage() + " result:" + commandContext.getResponse().getResult());

        long code = commandContext.getResponse().getCode();

        if (0L != code) {
            context.getResponse().setWarn(commandContext.getResponse().getMessage());
            return;
        }

        context.getResponse().putData("DATA", commandContext.getResponse().getData("DATA"));

    }


}
