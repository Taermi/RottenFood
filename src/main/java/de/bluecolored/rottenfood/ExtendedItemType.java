package de.bluecolored.rottenfood;

import io.leangen.geantyref.TypeToken;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ExtendedItemType {
	public static final TypeToken<ExtendedItemType> TOKEN = TypeToken.get(ExtendedItemType.class);

	private final Material material;

	public ExtendedItemType(Material material) {
		this.material = material;
	}

	public boolean matches(ItemStack is) {
		return is.getType() == material;
	}

	public Material getMaterial() {
		return material;
	}
}
