package com.aizuda.snail.ai.persistence.config;

import java.text.MessageFormat;

import com.aizuda.snail.ai.persistence.enums.BizDbTypeEnum;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * 业务库存储自动配置。
 *
 * @author suiyaner
 * @date 2026-06-17
 */
@AutoConfiguration
@AutoConfigureBefore(MybatisPlusAutoConfiguration.class)
@MapperScan("com.aizuda.snail.ai.persistence")
public class SnailAiBizStorageAutoConfiguration {

    @Bean
    public MybatisPlusPropertiesCustomizer bizMapperLocationsCustomizer(Environment environment) {
        return properties -> {
            String dbType = BizDbTypeEnum.from(environment).getDb();
            properties.setMapperLocations(new String[]{
                    MessageFormat.format("classpath*:/{0}/mapper/*.xml", dbType),
                    "classpath*:/template/mapper/*.xml"
            });
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(Environment environment) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        String db = BizDbTypeEnum.from(environment).getDb();
        DbType dbType = "postgresql".equals(db) ? DbType.POSTGRE_SQL : DbType.MYSQL;
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(dbType));
        // 建议增加防全表更新拦截器
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }
}
