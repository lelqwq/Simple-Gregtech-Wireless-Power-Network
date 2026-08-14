package com.miaokatze.gtswn.common.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.miaokatze.gtswn.common.tile.TileEntityNetworkInfoPanel;
import com.miaokatze.gtswn.main.GTSimpleWirelessNetwork;

import cpw.mods.fml.common.network.IGuiHandler;

public class GTSWNGuiHandler implements IGuiHandler {

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == GTSimpleWirelessNetwork.GUI_NETWORK_INFO_PANEL) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileEntityNetworkInfoPanel) {
                return new ContainerNetworkInfoPanel((TileEntityNetworkInfoPanel) tile);
            }
        }
        // 2.8.4 分支（0.5.0）：量子终端 GUI 客户端本地打开，无 openGui/容器分支
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return GTSimpleWirelessNetwork.proxy.getClientGuiElement(id, player, world, x, y, z);
    }
}
