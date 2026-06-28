package com.aizuda.snail.ai.persistence.app.mapper;

import com.aizuda.snail.ai.persistence.app.po.ClientNodePO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ClientNodeMapper extends BaseMapper<ClientNodePO> {

    /**
     * 批量插入客户端节点
     */
    int insertBatch(@Param("list") List<ClientNodePO> list);

    /**
     * 批量更新客户端节点过期时间及状态
     */
    int updateBatchExpireAt(@Param("list") List<ClientNodePO> list);
}
