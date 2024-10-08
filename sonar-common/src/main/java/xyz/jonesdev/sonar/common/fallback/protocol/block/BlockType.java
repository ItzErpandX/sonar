/*
 * Copyright (C) 2024 Sonar Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xyz.jonesdev.sonar.common.fallback.protocol.block;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.fallback.protocol.ProtocolVersion;

import java.util.function.Function;

@SuppressWarnings("unused")
@RequiredArgsConstructor
public enum BlockType {
  STONE(protocolVersion -> 1, 1),
  // Useful resources:
  // - https://github.com/PrismarineJS/minecraft-data/blob/master/data/pc/
  // - https://pokechu22.github.io/Burger/
  // - https://github.com/ViaVersion/Mappings/tree/main/mappings
  ENCHANTMENT_TABLE(protocolVersion -> {
    switch (protocolVersion) {
      case MINECRAFT_1_7_2:
      case MINECRAFT_1_7_6:
      default:
        // 1.7.2-1.12.2
        return 116;
      case MINECRAFT_1_13:
      case MINECRAFT_1_13_1:
        return 4612;
      case MINECRAFT_1_13_2:
      case MINECRAFT_1_14:
      case MINECRAFT_1_14_1:
      case MINECRAFT_1_14_2:
      case MINECRAFT_1_14_3:
        return 4613;
      case MINECRAFT_1_14_4:
      case MINECRAFT_1_15:
      case MINECRAFT_1_15_1:
      case MINECRAFT_1_15_2:
        return 5116;
      case MINECRAFT_1_16:
      case MINECRAFT_1_16_1:
        return 5132;
      case MINECRAFT_1_16_2:
      case MINECRAFT_1_16_3:
      case MINECRAFT_1_16_4:
        return 5136;
      case MINECRAFT_1_17:
      case MINECRAFT_1_17_1:
      case MINECRAFT_1_18:
      case MINECRAFT_1_18_2:
        return 5333;
      case MINECRAFT_1_19:
      case MINECRAFT_1_19_1:
        return 5719;
      case MINECRAFT_1_19_3:
        return 7159;
      case MINECRAFT_1_19_4:
        return 7385;
      case MINECRAFT_1_20:
      case MINECRAFT_1_20_2:
      case MINECRAFT_1_20_3:
      case MINECRAFT_1_20_5:
      case MINECRAFT_1_21:
        return 7389;
    }
  }, 0.75f),
  TRAPDOOR(protocolVersion -> {
    switch (protocolVersion) {
      case MINECRAFT_1_7_2:
      case MINECRAFT_1_7_6:
        // We have to use wooden trapdoors for 1.7 since 1.7 doesn't have iron trapdoors
        return 96;
      default:
        // 1.8-1.12.2
        return 167;
      case MINECRAFT_1_13:
      case MINECRAFT_1_13_1:
        return 6509;
      case MINECRAFT_1_13_2:
      case MINECRAFT_1_14:
      case MINECRAFT_1_14_1:
      case MINECRAFT_1_14_2:
      case MINECRAFT_1_14_3:
        return 6510;
      case MINECRAFT_1_14_4:
      case MINECRAFT_1_15:
      case MINECRAFT_1_15_1:
      case MINECRAFT_1_15_2:
        return 7016;
      case MINECRAFT_1_16:
      case MINECRAFT_1_16_1:
        return 7552;
      case MINECRAFT_1_16_2:
      case MINECRAFT_1_16_3:
      case MINECRAFT_1_16_4:
        return 7556;
      case MINECRAFT_1_17:
      case MINECRAFT_1_17_1:
      case MINECRAFT_1_18:
      case MINECRAFT_1_18_2:
        return 7802;
      case MINECRAFT_1_19:
      case MINECRAFT_1_19_1:
        return 8293;
      case MINECRAFT_1_19_3:
        return 9937;
      case MINECRAFT_1_19_4:
        return 10269;
      case MINECRAFT_1_20:
        return 10273;
      case MINECRAFT_1_20_2:
      case MINECRAFT_1_20_3:
      case MINECRAFT_1_20_5:
      case MINECRAFT_1_21:
        return 10414;
    }
  }, 0.1875),
  BARRIER(protocolVersion -> {
    switch (protocolVersion) {
      case MINECRAFT_1_7_2:
      case MINECRAFT_1_7_6:
        // We have to use glass for 1.7 since 1.7 doesn't have barrier blocks
        return 20;
      default:
        // 1.8-1.12.2
        return 166;
      case MINECRAFT_1_13:
      case MINECRAFT_1_13_1:
        return 6493;
      case MINECRAFT_1_13_2:
        return 6494;
      case MINECRAFT_1_14:
      case MINECRAFT_1_14_1:
      case MINECRAFT_1_14_2:
      case MINECRAFT_1_14_3:
      case MINECRAFT_1_14_4:
      case MINECRAFT_1_15:
      case MINECRAFT_1_15_1:
      case MINECRAFT_1_15_2:
        return 7000;
      case MINECRAFT_1_16:
      case MINECRAFT_1_16_1:
        return 7536;
      case MINECRAFT_1_16_2:
      case MINECRAFT_1_16_3:
      case MINECRAFT_1_16_4:
        return 7540;
      case MINECRAFT_1_17:
      case MINECRAFT_1_17_1:
      case MINECRAFT_1_18:
      case MINECRAFT_1_18_2:
        return 7754;
      case MINECRAFT_1_19:
      case MINECRAFT_1_19_1:
        return 8245;
      case MINECRAFT_1_19_3:
        return 9889;
      case MINECRAFT_1_19_4:
        return 10221;
      case MINECRAFT_1_20:
        return 10225;
      case MINECRAFT_1_20_2:
      case MINECRAFT_1_20_3:
      case MINECRAFT_1_20_5:
      case MINECRAFT_1_21:
        return 10366;
    }
  }, 1);

  private final Function<ProtocolVersion, Integer> idFunction;
  // TODO: Implement per-version block heights?
  @Getter
  private final double blockHeight;

  public int getId(final @NotNull ProtocolVersion protocolVersion) {
    return idFunction.apply(protocolVersion);
  }
}
