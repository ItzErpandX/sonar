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

package xyz.jonesdev.sonar.common.fallback.session;

import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.api.fallback.FallbackUser;
import xyz.jonesdev.sonar.common.fallback.protocol.FallbackPacket;
import xyz.jonesdev.sonar.common.fallback.protocol.captcha.CaptchaPreparer;
import xyz.jonesdev.sonar.common.fallback.protocol.captcha.ItemType;
import xyz.jonesdev.sonar.common.fallback.protocol.captcha.MapCaptchaInfo;
import xyz.jonesdev.sonar.common.fallback.protocol.packets.play.*;
import xyz.jonesdev.sonar.common.util.exception.QuietDecoderException;

import static xyz.jonesdev.sonar.common.fallback.protocol.FallbackPreparer.*;

/**
 * Flow for this session handler
 *
 * <li>
 *   {@link SetContainerSlotPacket} and {@link MapDataPacket} packets are sent to the client,
 *   therefore, setting the player's item to a map with a code on it (CAPTCHA).
 *   <br>
 *   See more: {@link FallbackCAPTCHASessionHandler}, {@link MapCaptchaInfo}
 * </li>
 * <li>
 *   Then, we wait for the player to enter the {@link FallbackCAPTCHASessionHandler#answer} in chat.
 * </li>
 */
public final class FallbackCAPTCHASessionHandler extends FallbackSessionHandler {

  public FallbackCAPTCHASessionHandler(final @NotNull FallbackUser user,
                                       final @NotNull String username) {
    super(user, username);

    // Disconnect the player if there is no CAPTCHA available at the moment
    if (!CaptchaPreparer.isCaptchaAvailable()) {
      user.disconnect(Sonar.get().getConfig().getVerification().getCurrentlyPreparing());
      throw QuietDecoderException.INSTANCE;
    }

    this.tries = Sonar.get().getConfig().getVerification().getMap().getMaxTries();

    // If the player is on Java, set the 5th slot (ID 4) in the player's hotbar to the map
    // If the player is on Bedrock, set the 1st slot (ID 0) in the player's hotbar to the map
    user.delayedWrite(new SetContainerSlotPacket(user.isGeyser() ? 36 : 40, 1,
      ItemType.FILLED_MAP.getId(user.getProtocolVersion()), SetContainerSlotPacket.MAP_NBT));
    // Send random captcha to the player
    final MapCaptchaInfo captcha = CaptchaPreparer.getRandomCaptcha();
    this.answer = captcha.getAnswer().toLowerCase();
    captcha.delayedWrite(user);
    // Teleport the player to the position above the platform
    user.delayedWrite(CAPTCHA_POSITION);
    // Make sure the player cannot move
    user.delayedWrite(user.isGeyser() ? CAPTCHA_ABILITIES_BEDROCK : CAPTCHA_ABILITIES);
    // Make sure the player knows that they have to enter the code in chat
    user.delayedWrite(enterCodeMessage);
    // Send all packets in one flush
    user.getChannel().flush();
  }

  private final String answer;
  private int tries, lastCountdownIndex, keepAliveStreak;

  @Override
  public void handle(final @NotNull FallbackPacket packet) {
    // Check if the player took too long to enter the CAPTCHA
    final int maxDuration = Sonar.get().getConfig().getVerification().getMap().getMaxDuration();
    checkState(!user.getLoginTimer().elapsed(maxDuration), "took too long to enter CAPTCHA");

    if (packet instanceof SystemChatPacket) {
      final SystemChatPacket chat = (SystemChatPacket) packet;
      // Finish the verification if the player entered the correct code
      if (chat.getMessage().toLowerCase().equals(answer)) {
        finishVerification();
        return;
      }
      // Decrement the number of tries left
      checkState(tries-- > 0, "failed CAPTCHA too often");
      // Send the player a chat message to let them know that the code they entered is incorrect
      user.write(incorrectCaptcha);
    } else if (packet instanceof SetPlayerPositionPacket
      || packet instanceof SetPlayerPositionRotationPacket) {
      // A position packet is sent approximately every second
      if (Sonar.get().getConfig().getVerification().getGamemode().isSurvivalOrAdventure()) {
        final long difference = maxDuration - user.getLoginTimer().delay();
        final int index = (int) (difference / 1000D);
        // Make sure we can actually safely get and send the packet
        if (lastCountdownIndex != index && index >= 0 && xpCountdown.length > index) {
          // Send the countdown using the experience bar
          user.write(xpCountdown[index]);
        }
        lastCountdownIndex = index;
      }
      // Send a KeepAlive packet every few seconds
      if (keepAliveStreak++ > 20) {
        keepAliveStreak = 0;
        // Send a KeepAlive packet to prevent timeout
        user.write(CAPTCHA_KEEP_ALIVE);
      }
    }
  }
}
