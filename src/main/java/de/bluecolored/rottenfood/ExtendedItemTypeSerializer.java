/*
 * The MIT License (MIT)
 *
 * Copyright (c) Blue <https://www.bluecolored.de>
 * Copyright (c) CraftedNature <https://www.craftednature.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.bluecolored.rottenfood;

import org.bukkit.Material;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public class ExtendedItemTypeSerializer implements TypeSerializer<ExtendedItemType> {

	@Override
	public ExtendedItemType deserialize(Type type, ConfigurationNode config) throws SerializationException {
		String itemDesc = config.getString();
		if (itemDesc == null) return null;

		Material material = Material.matchMaterial(itemDesc);
		if (material == null){
			throw new SerializationException("Failed to read item! (No such Material: '" + itemDesc + "')");
		}

		return new ExtendedItemType(material);
	}

	@Override
	public void serialize(Type type, @Nullable ExtendedItemType obj, ConfigurationNode value) throws SerializationException {
		throw new UnsupportedOperationException("Serializing is not supported for ExtendedItemType!");
	}

}
