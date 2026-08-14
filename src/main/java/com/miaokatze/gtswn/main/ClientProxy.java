package com.miaokatze.gtswn.main;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import com.miaokatze.gtswn.client.QuantumNodeHighlightRenderer;
import com.miaokatze.gtswn.client.WirelessTapHighlightRenderer;
import com.miaokatze.gtswn.client.gui.GuiNetworkInfoPanel;
import com.miaokatze.gtswn.client.gui.GuiQuantumTerminal;
import com.miaokatze.gtswn.client.render.RenderNetworkInfoPanel;
import com.miaokatze.gtswn.client.render.RenderNetworkQuantumNode;
import com.miaokatze.gtswn.common.block.BlockNetworkQuantumNode;
import com.miaokatze.gtswn.common.hud.WirelessMonitorHUD;
import com.miaokatze.gtswn.common.quantum.QuantumNetworkData;
import com.miaokatze.gtswn.common.tile.TileEntityNetworkInfoPanel;
import com.miaokatze.gtswn.network.PacketSyncAEMonitorData;
import com.miaokatze.gtswn.network.PacketSyncQuantumTerminalData;

import cpw.mods.fml.client.registry.ClientRegistry;
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
        // 注册无线链路终端辅助线渲染器（DrawBlockHighlightEvent，与 GT 扳手/覆盖板工具相同机制）
        MinecraftForge.EVENT_BUS.register(new WirelessTapHighlightRenderer());
        // v1.6.1 问题 2：注册量子节点放置预览框渲染器（手持已绑定量子终端瞄准可放置位置时画青色预览盒）
        MinecraftForge.EVENT_BUS.register(new QuantumNodeHighlightRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityNetworkInfoPanel.class, new RenderNetworkInfoPanel());

        // v1.6.1 问题 1：注册量子节点 ISBRH（线缆形态：小核心 + 朝 AE 网格宿主的连接臂）。
        // 【双端安全】renderId 先由 register() 申请并注册渲染器，再回写到 Block 类的静态 int 字段，
        // Block.getRenderType 只读该 int，Block 类不引用任何 client 包类，服务端加载安全。
        RenderNetworkQuantumNode.register();
        BlockNetworkQuantumNode.renderId = RenderNetworkQuantumNode.INSTANCE.getRenderId();

        // 注：原 PlayerLoggedOutEvent 监听器用于保存便携式 HUD 历史到物品 NBT，
        // 已随 WirelessMonitorHUD.saveHistoryToItemStack 删除而移除（用户确认便携式随退出登录重置）。
        // HUD 状态会在下次 findMonitorInInventory 时自动重置：
        // - 无监视器 → clearCache() 清空所有缓存
        // - 有监视器 → 重新初始化，靠 gap 检测和首次检测重建数据集
    }

    /**
     * 【hotfix v1.5.14】客户端处理 EU 响应包：调度到主线程后写入 HUD 缓存。
     * <p>
     * 【线程安全】1.7.10 的 SimpleChannelHandlerWrapper.channelRead0 直接在 Netty 网络线程
     * 调用 onMessage，而 HUD 渲染在客户端主线程读取 static 缓存，两者并发会竞争 static 字段。
     * 故用 {@link Minecraft#func_152344_a(Runnable)}（1.7.10 中 addScheduledTask 的 SRG 名）
     * 把写操作调度到主线程。
     * <p>
     * 【类加载安全】本方法在 ClientProxy 中，ClientProxy 只在客户端被 @SidedProxy 机制加载，
     * 服务端不会加载本类，故可安全引用 {@code Minecraft} 等客户端类。
     */
    @Override
    public void handleResponseEU(String euStr) {
        // 1.7.10 API：func_152344_a 等价于 1.8+ 的 addScheduledTask，调度到客户端主线程
        Minecraft.getMinecraft()
            .func_152344_a(() -> WirelessMonitorHUD.receiveSyncedEU(euStr));
    }

    /**
     * 【hotfix v1.5.14】客户端处理 AE 监控数据同步包：定位信息屏 TileEntity，切回主线程后写入 AE 缓存。
     * <p>
     * 【线程安全】SimpleChannelHandlerWrapper 在 Netty 网络线程调用 onMessage，而 GUI/TESR
     * 在客户端主线程读取 TileEntity 字段，故用 {@link Minecraft#func_152344_a(Runnable)}
     * 把写操作调度到主线程，避免并发读到半更新状态。
     * <p>
     * 【类加载安全】本方法在 ClientProxy 中，ClientProxy 只在客户端被 @SidedProxy 机制加载，
     * 服务端不会加载本类，故可安全引用 {@code Minecraft.getMinecraft().theWorld}
     * （theWorld 字段类型为 WorldClient，@SideOnly(Side.CLIENT)）。
     */
    @Override
    public void handleSyncAEMonitorData(PacketSyncAEMonitorData msg) {
        // 获取客户端世界（WorldClient，仅在客户端可访问）
        final World world = Minecraft.getMinecraft().theWorld;
        if (world == null) {
            return;
        }

        // 先在网络线程定位 TileEntity（避免主线程 world 快照不一致）
        final TileEntity te = world.getTileEntity(msg.getX(), msg.getY(), msg.getZ());
        if (!(te instanceof TileEntityNetworkInfoPanel)) {
            return;
        }

        // 切到主线程再修改 TileEntity 字段
        Minecraft.getMinecraft()
            .func_152344_a(() -> {
                TileEntityNetworkInfoPanel panel = (TileEntityNetworkInfoPanel) world
                    .getTileEntity(msg.getX(), msg.getY(), msg.getZ());
                if (panel != null) {
                    panel.receiveAEMonitorData(msg.getChartSamples(), msg.getMonitorLatest(), msg.getMonitorAvg300s());
                }
            });
    }

    /**
     * 客户端处理量子终端数据同步包：切主线程后写入 GUI 静态缓存。
     * <p>
     * 【线程安全】与 {@link #handleSyncAEMonitorData} 同理：onMessage 运行在 Netty 网络线程，
     * 而 GuiQuantumTerminal 在客户端主线程读取静态缓存，故用
     * {@link Minecraft#func_152344_a(Runnable)} 把写操作调度到主线程。
     * <p>
     * 【类加载安全】本方法在 ClientProxy 中，仅客户端加载，可安全引用 Minecraft 与 GUI 类。
     */
    @Override
    public void handleSyncQuantumTerminalData(PacketSyncQuantumTerminalData msg) {
        final QuantumNetworkData data = msg.getData();
        if (data == null) {
            return;
        }
        // 1.7.10 API：func_152344_a 等价于 1.8+ 的 addScheduledTask，调度到客户端主线程
        Minecraft.getMinecraft()
            .func_152344_a(() -> GuiQuantumTerminal.receiveData(data));
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == GTSimpleWirelessNetwork.GUI_NETWORK_INFO_PANEL) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileEntityNetworkInfoPanel) {
                return new GuiNetworkInfoPanel((TileEntityNetworkInfoPanel) tile);
            }
        }
        // 2.8.4 分支（0.5.0）：量子终端 GUI 客户端本地打开（openQuantumTerminalGui），无 openGui 分支
        return null;
    }

    /** 射线命中方块提示冷却表（2.8.4 分支 0.5.0）：玩家 UUID → 上次提示的客户端世界 tick */
    private static final Map<UUID, Long> lastAimBlockedHintTick = new HashMap<>();

    /** 射线命中方块提示冷却（tick），防连点刷屏 */
    private static final long AIM_BLOCKED_HINT_COOLDOWN_TICKS = 40L;

    /**
     * 打开量子终端 GUI（客户端逻辑，2.8.4 分支 0.5.0）。
     * <p>
     * 准星判定使用客户端本地 {@code mc.objectMouseOver}（真·选块数据，零误差）：
     * 命中方块 = 方块手势（控制器/机器等），不开终端 GUI，仅给提示（带冷却）；
     * 未命中（空气）→ 本地 {@code displayGuiScreen} 打开 GUI，无服务端容器/开窗往返。
     */
    @Override
    public void openQuantumTerminalGui(EntityPlayer player) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            // 准星选中方块 → 方块手势，不开 GUI；提示避免"没反应"的困惑
            long tick = mc.theWorld.getTotalWorldTime();
            Long last = lastAimBlockedHintTick.get(player.getUniqueID());
            if (last == null || tick - last >= AIM_BLOCKED_HINT_COOLDOWN_TICKS) {
                lastAimBlockedHintTick.put(player.getUniqueID(), tick);
                player.addChatMessage(
                    new ChatComponentText(StatCollector.translateToLocal("gtswn.chat.quantum.aim_blocked")));
            }
            return;
        }
        mc.displayGuiScreen(new GuiQuantumTerminal());
    }
}
