package com.gali.ae2_auto_pattern_upload.network.upload;

import com.gali.ae2_auto_pattern_upload.util.RecipeNameUtil;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;

/**
 * 清除配方名称数据包 - 服务器通知客户端清除最后捕获的配方名称
 */
public class ClearRecipeNamePacket implements IMessage {

    public ClearRecipeNamePacket() {}

    @Override
    public void fromBytes(io.netty.buffer.ByteBuf buf) {
        // 无数据
    }

    @Override
    public void toBytes(io.netty.buffer.ByteBuf buf) {
        // 无数据
    }

    public static class Handler implements IMessageHandler<ClearRecipeNamePacket, IMessage> {

        @Override
        public IMessage onMessage(ClearRecipeNamePacket message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                // 在客户端清除配方名称
                RecipeNameUtil.clearLastRecipeName();
            }
            return null;
        }
    }
}
