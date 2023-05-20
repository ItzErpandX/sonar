/*
 *  Copyright (c) 2023, jones (https://jonesdev.xyz) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package jones.sonar.bukkit.verbose;

import jones.sonar.api.verbose.Verbose;
import jones.sonar.common.verbose.VerboseAnimation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@RequiredArgsConstructor
public final class ActionBarVerbose implements Verbose {
    @Getter
    private final Collection<String> subscribers = new ArrayList<>();

    public void update() {
        final TextComponent component = new TextComponent("§e§lSonar §7> §f" + VerboseAnimation.nextState());

        for (final String subscriber : subscribers) {
            Optional.ofNullable(Bukkit.getPlayer(subscriber)).ifPresent(player -> {
                // TODO: action bar
            });
        }
    }
}