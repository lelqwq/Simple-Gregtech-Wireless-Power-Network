package com.miaokatze.gtswn.common.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import com.miaokatze.gtswn.common.tile.TileEntityNetworkInfoPanel;

public class ContainerNetworkInfoPanel extends Container {

    private final TileEntityNetworkInfoPanel panel;

    public ContainerNetworkInfoPanel(TileEntityNetworkInfoPanel panel) {
        this.panel = panel;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return panel != null && panel.getWorldObj()
            .getTileEntity(panel.xCoord, panel.yCoord, panel.zCoord) == panel
            && player.getDistanceSq(panel.xCoord + 0.5D, panel.yCoord + 0.5D, panel.zCoord + 0.5D) <= 64.0D;
    }

    /**
     * 2.8.4 分支修复（0.4.1）：0 槽容器对任何槽位点击安全返回 null。
     * <p>
     * 与 {@link com.miaokatze.gtswn.common.gui.ContainerQuantumTerminal} 相同的预防：
     * 本容器 inventorySlots 为空，若客户端与服务端的容器关闭存在时序窗口，
     * 点击窗口包会命中空列表抛 IndexOutOfBoundsException 被踢。覆写后无害化。
     */
    @Override
    public ItemStack slotClick(int slotId, int clickedButton, int mode, EntityPlayer player) {
        return null;
    }
}
