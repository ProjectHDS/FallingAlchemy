package work.crash.fallingalchemy.modsupport.crt;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.event.IEventManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;
import work.crash.fallingalchemy.event.FallingConversionEvent;

@ZenRegister
@ZenExpansion("crafttweaker.events.IEventManager")
public class FallingAlchemyEvents {

    @ZenMethod
    public static void onFallingConversionPre(IEventManager manager, IEventHandler<FallingConversionPreEvent> handler) {
        MinecraftForge.EVENT_BUS.register(new PreEventHandlerWrapper(handler));
    }

    @ZenMethod
    public static void onFallingConversionPost(IEventManager manager, IEventHandler<FallingConversionPostEvent> handler) {
        MinecraftForge.EVENT_BUS.register(new PostEventHandlerWrapper(handler));
    }

    @ZenRegister
    public interface IEventHandler<T> {
        void handle(T event);
    }


    public static class PreEventHandlerWrapper {
        private final IEventHandler<FallingConversionPreEvent> handler;

        public PreEventHandlerWrapper(IEventHandler<FallingConversionPreEvent> handler) {
            this.handler = handler;
        }

        @SubscribeEvent
        public void handle(FallingConversionEvent.Pre event) {
            handler.handle(new FallingConversionPreEvent(event));
        }
    }

    public static class PostEventHandlerWrapper {
        private final IEventHandler<FallingConversionPostEvent> handler;

        public PostEventHandlerWrapper(IEventHandler<FallingConversionPostEvent> handler) {
            this.handler = handler;
        }

        @SubscribeEvent
        public void handle(FallingConversionEvent.Post event) {
            handler.handle(new FallingConversionPostEvent(event));
        }
    }
}