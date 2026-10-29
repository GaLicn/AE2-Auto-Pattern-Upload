package com.gali.ae2_auto_pattern_upload.util;

import net.minecraft.entity.Entity;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

public class QuantumTeleporter extends Teleporter {

    private final double posX;
    private final double posY;
    private final double posZ;

    public QuantumTeleporter(WorldServer world, double x, double y, double z) {
        super(world);
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    @Override
    public void placeInPortal(Entity entity, double p_77185_2_, double p_77185_4_, double p_77185_6_,
        float p_77185_8_) {
        this.placeEntity(entity);
    }

    @Override
    public boolean placeInExistingPortal(Entity entity, double p_85188_2_, double p_85188_4_, double p_85188_6_,
        float p_85188_8_) {
        this.placeEntity(entity);
        return true;
    }

    @Override
    public boolean makePortal(Entity entity) {
        return true;
    }

    @Override
    public void removeStalePortalLocations(long worldTime) {}

    private void placeEntity(Entity entity) {
        entity.setLocationAndAngles(this.posX, this.posY, this.posZ, entity.rotationYaw, 0.0F);
        entity.motionX = entity.motionY = entity.motionZ = 0.0D;
        entity.fallDistance = 0.0F;
    }
}
