package work.crash.fallingalchemy;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import work.crash.fallingalchemy.handler.FallingBlockHandler;

@Mod(
        modid = Tags.MOD_ID,
        name = Tags.MOD_NAME,
        version = Tags.VERSION
)
public class FallingAlchemy {

    public FallingAlchemy() {
        MinecraftForge.EVENT_BUS.register(new FallingBlockHandler());
    }

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Hello From {}!", Tags.MOD_NAME);
    }

}
