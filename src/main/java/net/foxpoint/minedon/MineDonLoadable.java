package net.foxpoint.minedon;

import net.minecraftforge.fml.common.Mod;

@Mod("minedon")
public class MineDonLoadable {
    public MineDonLoadable() {
        MineDon.getInstance();
    }
}
