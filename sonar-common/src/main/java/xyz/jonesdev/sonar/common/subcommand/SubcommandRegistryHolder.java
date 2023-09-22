/*
 * Copyright (C) 2023 Sonar Contributors
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

package xyz.jonesdev.sonar.common.subcommand;

import xyz.jonesdev.sonar.api.command.subcommand.SubcommandRegistry;
import xyz.jonesdev.sonar.common.subcommand.impl.*;

public final class SubcommandRegistryHolder implements SubcommandRegistry {
  public SubcommandRegistryHolder() {
    register(
      new BlacklistCommand(),
      new VerifiedCommand(),
      new StatisticsCommand(),
      new LockdownCommand(),
      new VerboseCommand(),
      new ReloadCommand(),
      new DumpCommand()
    );
  }
}