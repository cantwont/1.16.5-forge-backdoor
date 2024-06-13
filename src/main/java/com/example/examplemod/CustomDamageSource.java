// not really needed

package com.example.examplemod;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.DamageSource;

public class CustomDamageSource extends DamageSource {

    private String deathMessage;

    public CustomDamageSource(String damageTypeIn) {
        super(damageTypeIn);
    }

    public CustomDamageSource setDeathMessage(String deathMessage) {
        this.deathMessage = deathMessage;
        return this;
    }

    public String getDeathMessage() {
        return deathMessage;
    }

    public Object getLocalizedDeathMessage(ServerPlayerEntity player) {
        String s = this.deathMessage;
        return s == null ? super.getLocalizedDeathMessage(player) : player.getDisplayName().getString() + " was killed by " + s;
    }
}
