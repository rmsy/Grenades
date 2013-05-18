package tc.oc.grenades;

import org.bukkit.plugin.java.JavaPlugin;

public final class GrenadesPlugin extends JavaPlugin {
    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(new GrenadesListener(this), this);
    }
}
