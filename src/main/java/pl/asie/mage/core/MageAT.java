package pl.asie.mage.core;

import net.minecraftforge.fml.common.asm.transformers.AccessTransformer;

import java.io.IOException;

public class MageAT extends AccessTransformer {
	public MageAT() throws IOException {
		super("mage_at.cfg");
	}
}
