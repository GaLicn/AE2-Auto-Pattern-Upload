package com.gali.ae2_auto_pattern_upload.command;

import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldServer;

import com.gali.ae2_auto_pattern_upload.util.QuantumTeleporter;

public class CommandTeleportToPlayer extends CommandBase {

    @Override
    public String getCommandName() {
        return "tpp";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/tpp <玩家名>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // 需要 OP 权限等级 2
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new CommandException("用法：/tpp <玩家名>");
        }

        if (!(sender instanceof EntityPlayerMP player)) {
            throw new CommandException("只有玩家可以使用此命令");
        }

        EntityPlayerMP targetPlayer = MinecraftServer.getServer()
            .getConfigurationManager()
            .func_152612_a(args[0]);

        if (targetPlayer == null) {
            throw new CommandException("玩家 '" + args[0] + "' 不在线");
        }

        if (player.getUniqueID()
            .equals(targetPlayer.getUniqueID())) {
            throw new CommandException("不能传送到自己");
        }

        try {
            int targetDim = targetPlayer.dimension;
            double targetX = targetPlayer.posX;
            double targetY = targetPlayer.posY + 0.5;
            double targetZ = targetPlayer.posZ;

            MinecraftServer server = MinecraftServer.getServer();
            WorldServer currentWorld = server.worldServerForDimension(player.dimension);
            WorldServer targetWorld = server.worldServerForDimension(targetDim);

            if (targetWorld == null) {
                throw new CommandException("目标玩家所在维度不存在或未加载");
            }

            if (player.dimension != targetDim) {
                // 跨维度传送
                server.getConfigurationManager()
                    .transferPlayerToDimension(
                        player,
                        targetDim,
                        new QuantumTeleporter(targetWorld, targetX, targetY, targetZ));

                player.playerNetServerHandler.sendPacket(
                    new S1FPacketSetExperience(player.experience, player.experienceTotal, player.experienceLevel));
                player.sendPlayerAbilities();

                // 处理特殊情况：如果从地狱或到地狱传送，需要额外处理（参考 ServerUtilities）
                if (currentWorld.provider.dimensionId == 1 && player.isEntityAlive()) {
                    targetWorld.spawnEntityInWorld(player);
                    targetWorld.updateEntityWithOptionalForce(player, false);
                }

                // 确保位置正确
                player.playerNetServerHandler.setPlayerLocation(targetX, targetY, targetZ, player.rotationYaw, 0.0F);
                player.motionX = player.motionY = player.motionZ = 0.0D;
                player.fallDistance = 0.0F;
            } else {
                // 同维度传送
                player.playerNetServerHandler.setPlayerLocation(targetX, targetY, targetZ, player.rotationYaw, 0.0F);
                player.motionX = player.motionY = player.motionZ = 0.0D;
                player.fallDistance = 0.0F;
            }

            sender.addChatMessage(
                new ChatComponentText(
                    "§a已传送到玩家 §b" + targetPlayer.getCommandSenderName()
                        + " §a所在维度："
                        + targetDim
                        + " §a坐标：("
                        + String.format("%.1f", targetX)
                        + ", "
                        + String.format("%.1f", targetY)
                        + ", "
                        + String.format("%.1f", targetZ)
                        + ")"));
        } catch (Exception e) {
            throw new CommandException("传送失败：" + e.getMessage());
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                MinecraftServer.getServer()
                    .getAllUsernames());
        }
        return super.addTabCompletionOptions(sender, args);
    }
}
