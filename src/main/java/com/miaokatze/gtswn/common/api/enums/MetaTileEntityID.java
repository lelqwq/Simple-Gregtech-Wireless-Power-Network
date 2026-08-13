package com.miaokatze.gtswn.common.api.enums;

import com.miaokatze.gtswn.config.Config;

/**
 * 元机器实体 (MTE) ID 枚举
 * <p>
 * 模仿 NH-Utilities 的风格，为每个机器分配全局唯一的整数 ID。
 * 这种集中管理的方式可以有效避免与其他模组的机器 ID 发生冲突。
 * <p>
 * 最终 ID 计算公式：BASE (14600) + Config.metaIdOffset (配置偏移量) + relativeId (枚举内相对 ID)
 */
public enum MetaTileEntityID {

    // --- 无线电网监控机器 ---
    /** LV 等级无线能量监视器 (Tier 1) */
    // 2.8.4 适配：保持 14620（上游 1.6.18 起改为 0；本分支保留以兼容 0.2.x 存档）
    WIRELESS_ENERGY_MONITOR(20),

    ;

    // 最终计算出的全局唯一 ID
    public final int ID;

    // ID 基准值，用于避免与其他模组的机器 ID 冲突
    private static final int BASE = 14600;

    /**
     * 构造函数：根据相对 ID 计算全局 ID
     * 
     * @param relativeId 枚举项在列表中的相对索引
     */
    MetaTileEntityID(int relativeId) {
        this.ID = BASE + Config.metaIdOffset + relativeId;
    }
}
