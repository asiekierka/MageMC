package pl.asie.mage.hooks;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.common.MinecraftForge;
import pl.asie.mage.api.event.TextureAddedEvent;

public final class TextureAddedHook {
	private TextureAddedHook() {

	}

	public static void processTexture(TextureAtlasSprite sprite) {
		MinecraftForge.EVENT_BUS.post(new TextureAddedEvent(sprite));
	}
}
