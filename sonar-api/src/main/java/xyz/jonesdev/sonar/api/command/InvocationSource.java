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

package xyz.jonesdev.sonar.api.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

import java.util.UUID;
import java.util.function.Predicate;

@Getter
@RequiredArgsConstructor
public final class InvocationSource {
  private final UUID uuid;
  private final Audience audience;
  private final Predicate<String> permissionFunction;

  /**
   * @return true if {@link InvocationSource#uuid} is not null
   */
  public boolean isPlayer() {
    return uuid != null;
  }

  /**
   * Sends a message to the command executor
   */
  public void sendMessage(final Component component) {
    audience.sendMessage(component);
  }
}
