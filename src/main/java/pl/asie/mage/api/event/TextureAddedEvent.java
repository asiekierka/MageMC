package pl.asie.mage.api.event;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * This event is emitted after a texture has been added to the TextureMap
 * and its data loaded, but before its mipmaps have been generated.
 *
 * You can use this event to modify textures at runtime.
 */
public class TextureAddedEvent extends Event {
	private final TextureAtlasSprite sprite;

	public TextureAddedEvent(TextureAtlasSprite sprite) {
		this.sprite = sprite;
	}

	public TextureAtlasSprite getSprite() {
		return sprite;
	}
}
