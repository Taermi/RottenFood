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

import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public class ItemConfigSerializer implements TypeSerializer<ItemConfig> {

	@Override
	public ItemConfig deserialize(Type type, ConfigurationNode config) throws SerializationException {

		ItemConfig.Builder builder = ItemConfig.builder();

		for (ConfigurationNode item : config.node("items").childrenList()){
			builder.addItem(item.get(ExtendedItemType.TOKEN));
		}

		for (ConfigurationNode state : config.node("states").childrenList()){
			long age = state.node("age").getLong(-1) * 1000;
			if (age < 0) throw new SerializationException("Config-value 'age' is missing or below zero! (must be at or above 0)");

			String sName = state.node("name").getString();
			Component name = null;
			if (sName != null){
				try {
					name = GsonComponentSerializer.gson().deserialize(sName);
				} catch (Exception ex){
					name = LegacyComponentSerializer.legacyAmpersand().deserialize(sName);
				}
			}

			String sLore = state.node("lore").getString();
			Component lore = null;
			if (sLore != null){
				try {
					lore = GsonComponentSerializer.gson().deserialize(sLore);
				} catch (Exception ex){
					lore = LegacyComponentSerializer.legacyAmpersand().deserialize(sLore);
				}
			}

			ExtendedItemType replacement = state.node("replacement-item").get(TypeToken.get(ExtendedItemType.class));

			builder.addAgeState(new ItemAgeStateConfig(age, name, lore, replacement));
		}

		for (ConfigurationNode mod : config.node("ageing-modifier").childrenList()){
			double multiplier = mod.node("multiplier").getDouble(1);

			ExtendedItemType item = mod.node("item").get(TypeToken.get(ExtendedItemType.class));
			if (item == null) throw new SerializationException("Config-value 'item' in 'ageing-modifier' is missing!");

			int minItems = mod.node("min-item-count").getInt(1);
			if (minItems < 0) throw new SerializationException("Config-value 'min-item-count' is at or below zero! (must to be above 0)");

			builder.addAgingModifier(new ItemAgeingModifierConfig(multiplier, item, minItems));
		}

		builder.stackingInterval(config.node("item-stacking-intervall").getLong(60) * 1000);

		builder.showAge(config.node("show-age").getBoolean(false));

		return builder.build();
	}

	@Override
	public void serialize(Type type, @Nullable ItemConfig obj, ConfigurationNode value) throws SerializationException {
		throw new UnsupportedOperationException("Serializing is not supported for ItemConfigs!");
	}

}
