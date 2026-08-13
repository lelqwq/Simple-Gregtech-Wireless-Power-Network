package com.miaokatze.gtswn.main;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

import com.miaokatze.gtswn.common.hud.WirelessMonitorHUD;

import cpw.mods.fml.common.event.FMLInitializationEvent;

/**
 * 客户端代理类
 * 继承自 CommonProxy，用于处理仅在客户端（Client Side）执行的逻辑。
 * 例如：渲染注册、按键绑定、GUI 打开等。
 */
public class ClientProxy extends CommonProxy {

    /**
     * 初始化阶段 (Init)
     * 在此阶段注册客户端特定的事件处理器，如 HUD 渲染器。
     */
    @Override
    public void init(FMLInitializationEvent event) {
        // 调用父类的 init 方法，确保通用逻辑正常执行
        super.init(event);

        // 注册 HUD 渲染器到 Forge 事件总线（仅在客户端）
        // 注意：RenderGameOverlayEvent 是 Forge 事件，必须注册到 MinecraftForge.EVENT_BUS
        GTSimpleWirelessNetwork.LOG.info("[2/2] 注册客户端 HUD 渲染器...");
        MinecraftForge.EVENT_BUS.register(new WirelessMonitorHUD());
    }

    /**
     * 处理服务端→客户端 EU 响应包（客户端逻辑）。
     * <p>
     * 包 Handler 运行在 Netty 网络线程，而 HUD 缓存在客户端主线程渲染读取，
     * 故用 {@link Minecraft#func_152344_a(Runnable)} 把写操作调度到主线程，避免并发读到半更新状态。
     *
     * @param euStr 服务端传来的 EU 字符串
     */
    @Override
    public void handleResponseEU(String euStr) {
        // 1.7.10 API：func_152344_a 等价于 1.8+ 的 addScheduledTask，调度到客户端主线程
        Minecraft.getMinecraft()
            .func_152344_a(() -> WirelessMonitorHUD.receiveSyncedEU(euStr));
    }

}
