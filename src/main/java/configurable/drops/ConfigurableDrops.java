package configurable.drops;

import com.mojang.logging.LogUtils;
import configurable.drops.parser.loot.LootParser;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

@Mod(ConfigurableDrops.modId)
public class ConfigurableDrops
{
    public static final String modId = "configurable_drops";
    public static final Logger logger = LogUtils.getLogger();
    public ConfigurableDrops(IEventBus modEventBus)
    {
        NeoForge.EVENT_BUS.register(this);
    }
    @SubscribeEvent
    public void registerCommandsEvent(RegisterCommandsEvent registerCommandsEvent)
    {
        Registry.registerCommands(registerCommandsEvent.getDispatcher());
    }
    @SubscribeEvent
    public void serverStartedEvent(ServerStartedEvent serverStartedEvent)
    {
        LootParser.instance.reload();
    }
    @SubscribeEvent
    public void livingDeathEvent(LivingDeathEvent livingDeathEvent)
    {
        if(livingDeathEvent.getEntity() instanceof Monster)
        {
            LootParser.instance.dropForLivingEntity(livingDeathEvent.getEntity());
        }
    }
}
