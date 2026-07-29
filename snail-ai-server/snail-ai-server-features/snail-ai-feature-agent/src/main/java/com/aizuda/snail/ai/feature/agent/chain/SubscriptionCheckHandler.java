package com.aizuda.snail.ai.feature.agent.chain;

import com.aizuda.snail.ai.persistence.admin.po.UserPO;
import com.aizuda.snail.ai.persistence.agent.mapper.UserAgentMapper;
import com.aizuda.snail.ai.persistence.agent.po.AgentPO;
import com.aizuda.snail.ai.persistence.agent.po.UserAgentPO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 订阅校验 Handler：在 InitContextHandler 之后校验用户是否已订阅当前智能体。
 * <p>
 * 校验规则：
 * <ul>
 *   <li>嵌入模式（OpenAPI，ctx.openId != null）：一律必须订阅，无豁免</li>
 *   <li>Admin 后台：自己创建的智能体（creatorId == userId）免订阅（调试模式）</li>
 *   <li>Admin 后台：非创建者必须订阅</li>
 * </ul>
 *
 * @author opensnail
 * @since 2026-07-29
 */
@Slf4j
@Component
@Order(15)
@RequiredArgsConstructor
public class SubscriptionCheckHandler implements AgentChatHandler {

    private final UserAgentMapper userAgentMapper;

    @Override
    public void handle(AgentChatContext ctx) {
        AgentPO agent = ctx.getAgent();
        UserPO user = ctx.getUser();

        // 嵌入模式（OpenAPI）：一律必须订阅，无豁免
        if (ctx.getOpenId() != null) {
            checkSubscription(ctx, agent, user);
            return;
        }

        // Admin 后台：自己创建的智能体 → 免订阅（调试模式）
        if (agent.getCreatorId() != null && agent.getCreatorId().equals(user.getId())) {
            log.debug("用户是智能体创建者，豁免订阅检查 userId={} agentId={}", user.getId(), agent.getId());
            return;
        }

        // Admin 后台：非创建者 → 必须订阅
        checkSubscription(ctx, agent, user);
    }

    private void checkSubscription(AgentChatContext ctx, AgentPO agent, UserPO user) {
        Long count = userAgentMapper.selectCount(
                new LambdaQueryWrapper<UserAgentPO>()
                        .eq(UserAgentPO::getUserId, user.getId())
                        .eq(UserAgentPO::getAgentId, agent.getId()));
        if (count == 0) {
            log.warn("用户未订阅智能体，拒绝对话 userId={} agentId={}", user.getId(), agent.getId());
            ctx.getStreamWriter().send("Error: 请先订阅该智能体");
            ctx.getStreamWriter().complete();
            ctx.setTerminated(true);
        }
    }
}