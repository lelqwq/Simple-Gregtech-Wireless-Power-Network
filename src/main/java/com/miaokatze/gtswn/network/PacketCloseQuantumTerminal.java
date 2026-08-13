package com.miaokatze.gtswn.network;

import net.minecraft.entity.player.EntityPlayerMP;

import com.miaokatze.gtswn.common.quantum.QuantumTerminalRequestQueue;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 客户端→服务端 关闭请求包：量子终端 GUI 关闭时通知服务端同步关闭容器（discriminator = 7）。
 * <p>
 * 2.8.4 分支修复（0.4.1）：{@code ContainerQuantumTerminal} 是 0 槽容器，客户端关闭
 * GUI 后若服务端容器仍挂着（玩家一直手持终端时 canInteractWith 恒为 true），
 * 客户端后续的点击窗口包（如背包整理 mod 的 C0EPacketClickWindow）会按背包槽位号
 * 命中空 inventorySlots，抛出 IndexOutOfBoundsException 被踢出服务器。
 * 本包由 {@code GuiQuantumTerminal.onGuiClosed()} 发出，服务端主线程收到后执行
 * {@code EntityPlayerMP.closeContainer()}，消除窗口 ID/容器残留窗口。
 */
public class PacketCloseQuantumTerminal implements IMessage {

    /** Forge 反射无参构造（反序列化时必需） */
    public PacketCloseQuantumTerminal() {}

    @Override
    public void fromBytes(ByteBuf buf) {
        // 无字段
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // 无字段
    }

    /**
     * 服务端处理：仅入队，由 {@link QuantumTerminalRequestQueue#drain()} 在主线程关闭容器。
     * <p>
     * 与包 5 相同的「Netty 入队 → 主线程 drain」模式：onMessage 运行在 Netty 网络线程，
     * 不得直接操作玩家容器状态。
     */
    public static class Handler implements IMessageHandler<PacketCloseQuantumTerminal, IMessage> {

        @Override
        public IMessage onMessage(PacketCloseQuantumTerminal msg, MessageContext ctx) {
            if (ctx.side.isServer()) {
                EntityPlayerMP player = ctx.getServerHandler().playerEntity;
                QuantumTerminalRequestQueue.enqueueClose(player);
            }
            return null;
        }
    }
}
