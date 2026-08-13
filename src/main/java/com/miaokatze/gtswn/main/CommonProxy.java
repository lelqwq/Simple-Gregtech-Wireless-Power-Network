package com.miaokatze.gtswn.main;

import static com.miaokatze.gtswn.common.api.enums.GTSWNItemList.GTswn_Cover_Dynamo_Wireless;
import static com.miaokatze.gtswn.common.api.enums.GTSWNItemList.GTswn_Cover_Energy_Wireless;

import com.miaokatze.gtswn.Tags;
import com.miaokatze.gtswn.common.command.CommandGTSWN;
import com.miaokatze.gtswn.common.covers.GTswn_Cover_DynamoWireless;
import com.miaokatze.gtswn.common.covers.GTswn_Cover_EnergyWireless;
import com.miaokatze.gtswn.config.Config;
import com.miaokatze.gtswn.loader.ItemLoader;
import com.miaokatze.gtswn.loader.MachineLoader;
import com.miaokatze.gtswn.network.GTSWNPacketHandler;
import com.miaokatze.gtswn.recipe.CraftingRecipes;
import com.miaokatze.gtswn.register.CreativeTabManager;
import com.miaokatze.gtswn.register.TextureManager;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.covers.CoverRegistry;
import gregtech.api.render.TextureFactory;

/**
 * 通用代理类
 * 处理服务端和客户端共有的逻辑，如配置加载、机器注册、创造模式物品栏初始化等。
 */
public class CommonProxy {

    /**
     * 预初始化阶段 (PreInit)
     * 在此阶段读取配置文件，并将机器注册任务添加到 GregTech 的处理队列中。
     */
    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        GTSimpleWirelessNetwork.LOG.info("GTSimpleWirelessNetwork 开始初始化 (版本: " + Tags.VERSION + ")");

        // 注册物品
        GTSimpleWirelessNetwork.LOG.info("[0/3] 开始注册物品...");
        try {
            ItemLoader.initItems();
            GTSimpleWirelessNetwork.LOG.info("[0/3] 物品注册完成。");
        } catch (Throwable t) {
            GTSimpleWirelessNetwork.LOG.error("[0/3] 物品注册过程中发生严重错误，请检查日志", t);
        }

        // 定义机器注册任务
        Runnable registerRunnable = () -> {
            GTSimpleWirelessNetwork.LOG.info("[1/3] 开始执行机器注册流程...");
            try {
                MachineLoader.initMachines();
                GTSimpleWirelessNetwork.LOG.info("[1/3] 机器注册流程执行完毕。");
            } catch (Throwable t) {
                GTSimpleWirelessNetwork.LOG.error("[1/3] 机器注册过程中发生严重错误，请检查日志", t);
            }
        };

        // 将注册任务添加到 GregTech 的 sAfterGTLoad 队列
        try {
            if (GregTechAPI.sAfterGTLoad == null) {
                GTSimpleWirelessNetwork.LOG.warn("警告: GregTechAPI.sAfterGTLoad 为空，无法添加注册任务。");
            } else {
                int before = GregTechAPI.sAfterGTLoad.size();
                GregTechAPI.sAfterGTLoad.add(registerRunnable);
                int after = GregTechAPI.sAfterGTLoad.size();
                GTSimpleWirelessNetwork.LOG
                    .info("[1/3] 已将机器注册任务加入 GregTech 加载队列 (队列大小: " + before + " -> " + after + ")");
            }
        } catch (Throwable t) {
            GTSimpleWirelessNetwork.LOG.error("无法将注册任务添加到 GregTech 队列", t);
        }

        // 注册网络包通道（修复 SMP 下便携监测终端 HUD 恒显 0 EU 的问题）
        GTSimpleWirelessNetwork.LOG.info("注册网络包通道...");
        GTSWNPacketHandler.register();
    }

    /**
     * 初始化阶段 (Init)
     * 在此阶段完成创造模式物品栏的初始化，并注册服务端 Tick 事件处理器。
     */
    @SuppressWarnings({ "unused" })
    public void init(FMLInitializationEvent event) {
        // 1. 确保机器注册任务已执行（通过 GregTech 队列在 preInit 结束时触发）
        // 2. 初始化创造模式物品栏（此时 GTSWNItemList 应已被 set() 填充）
        GTSimpleWirelessNetwork.LOG.info("[2/3] 开始初始化创造模式物品栏...");

        CreativeTabManager.initCreativeTab();
        GTSimpleWirelessNetwork.LOG.info(
            "[2/3] 创造模式物品栏初始化完成，当前包含 " + CreativeTabManager.getItemsToAdd()
                .size() + " 个物品。");
    }

    /**
     * 后初始化阶段 (PostInit)
     * 处理与其他模组的交互或完成最终设置，如注册测试配方。
     */
    @SuppressWarnings({ "unused" })
    public void postInit(FMLPostInitializationEvent event) {
        GTSimpleWirelessNetwork.LOG.info("[3/3] 开始注册合成配方...");
        try {
            CraftingRecipes.init();
            GTSimpleWirelessNetwork.LOG.info("[3/3] 合成配方注册完成。");
        } catch (Throwable t) {
            GTSimpleWirelessNetwork.LOG.error("[3/3] 合成配方注册过程中发生错误", t);
        }

        // 注册GTswn覆盖板
        GTSimpleWirelessNetwork.LOG.info("[PostInit] 开始注册GTswn覆盖板...");
        try {
            // 注册无线能量覆盖板（输入）-用我们自己的纹理！
            CoverRegistry.registerCover(
                GTswn_Cover_Energy_Wireless.get(1),
                TextureFactory.of(TextureManager.TEX_WIRELESS_CONNECTOR_INPUT),
                context -> new GTswn_Cover_EnergyWireless(context),
                CoverRegistry.INTERCEPTS_RIGHT_CLICK_COVER_PLACER);

            // 注册无线动力覆盖板（输出）-用我们自己的纹理！
            CoverRegistry.registerCover(
                GTswn_Cover_Dynamo_Wireless.get(1),
                TextureFactory.of(TextureManager.TEX_WIRELESS_CONNECTOR_OUTPUT),
                context -> new GTswn_Cover_DynamoWireless(context),
                CoverRegistry.INTERCEPTS_RIGHT_CLICK_COVER_PLACER);

            GTSimpleWirelessNetwork.LOG.info("[PostInit] GTswn覆盖板注册成功！");
        } catch (Throwable t) {
            GTSimpleWirelessNetwork.LOG.error("[PostInit] GTswn覆盖板注册失败", t);
        }
    }

    /**
     * 服务器启动阶段
     * 用于注册服务器端命令。
     */
    @SuppressWarnings({ "unused" })
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandGTSWN());
    }

    /**
     * 模组加载完成阶段
     * 如果之前注册失败，可以在此处进行最后的补救尝试。
     */
    public void loadComplete(cpw.mods.fml.common.event.FMLLoadCompleteEvent event) {}

    /**
     * 处理服务端→客户端 EU 响应包（客户端专用逻辑）。
     * <p>
     * 服务端空实现：此包只发往客户端，服务端收到也不会调用本方法。
     * 客户端逻辑由 {@link ClientProxy#handleResponseEU} 重写。
     * <p>
     * 设计（沿用上游 v1.5.14 hotfix）：不在包 Handler 方法体中直接引用客户端类，
     * 统一通过 @SidedProxy 委托，避免现代 JVM 下类加载触发 SideTransformer 剥离而崩服。
     *
     * @param euStr 服务端传来的 EU 字符串
     */
    public void handleResponseEU(String euStr) {
        // 服务端空实现：此包只发往客户端
    }
}
