package com.gali.ae2_auto_pattern_upload.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldServer;

import com.gali.ae2_auto_pattern_upload.util.QuantumTeleporter;

public class CommandQuantumTeleport extends CommandBase {

    @Override
    public String getCommandName() {
        return "ae2tp";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/ae2tp <x> <y> <z> <dim>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2; // 需要 OP 权限等级 2
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 4) {
            throw new CommandException("用法：/ae2tp <x> <y> <z> <维度 ID>");
        }

        if (!(sender instanceof EntityPlayerMP player)) {
            throw new CommandException("只有玩家可以使用此命令");
        }

        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            int dim = Integer.parseInt(args[3]);

            MinecraftServer server = MinecraftServer.getServer();
            WorldServer currentWorld = server.worldServerForDimension(player.dimension);
            WorldServer targetWorld = server.worldServerForDimension(dim);

            if (targetWorld == null) {
                throw new CommandException("目标维度不存在或未加载");
            }

            if (player.dimension != dim) {
                server.getConfigurationManager()
                    .transferPlayerToDimension(player, dim, new QuantumTeleporter(targetWorld, x, y, z));

                player.playerNetServerHandler.sendPacket(
                    new S1FPacketSetExperience(player.experience, player.experienceTotal, player.experienceLevel));
                player.sendPlayerAbilities();

                // 处理特殊情况：如果从地狱或到地狱传送，需要额外处理（参考 ServerUtilities）
                if (currentWorld.provider.dimensionId == 1 && player.isEntityAlive()) {
                    targetWorld.spawnEntityInWorld(player);
                    targetWorld.updateEntityWithOptionalForce(player, false);
                }

                // 确保位置正确（ServerUtilities 的做法）
                player.playerNetServerHandler.setPlayerLocation(x, y, z, player.rotationYaw, 0.0F);
                player.motionX = player.motionY = player.motionZ = 0.0D;
                player.fallDistance = 0.0F;
            } else {
                player.playerNetServerHandler.setPlayerLocation(x, y, z, player.rotationYaw, 0.0F);
                player.motionX = player.motionY = player.motionZ = 0.0D;
                player.fallDistance = 0.0F;
            }

            sender.addChatMessage(new ChatComponentText("§a已传送到维度 " + dim + " 坐标 (" + x + ", " + y + ", " + z + ")"));
        } catch (NumberFormatException e) {
            throw new CommandException("坐标和维度 ID 必须是数字");
        } catch (Exception e) {
            throw new CommandException("传送失败：" + e.getMessage());
        }
    }
}
