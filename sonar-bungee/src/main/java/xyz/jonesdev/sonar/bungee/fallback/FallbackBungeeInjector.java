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

package xyz.jonesdev.sonar.bungee.fallback;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import lombok.experimental.UtilityClass;
import net.md_5.bungee.netty.PipelineUtils;
import sun.misc.Unsafe;
import xyz.jonesdev.sonar.api.Sonar;
import xyz.jonesdev.sonar.common.fallback.netty.FallbackInjectedChannelInitializer;

import java.lang.reflect.Field;

import static net.md_5.bungee.netty.PipelineUtils.PACKET_DECODER;
import static xyz.jonesdev.sonar.api.fallback.FallbackPipelines.FALLBACK_PACKET_HANDLER;

@UtilityClass
public class FallbackBungeeInjector {
  public void inject() {
    try {
      final Field childField = PipelineUtils.class.getField("SERVER_CHILD");
      childField.setAccessible(true);

      // Make sure to store the original channel initializer
      final ChannelInitializer<Channel> originalInitializer = PipelineUtils.SERVER_CHILD;
      final ChannelInitializer<Channel> injectedInitializer = new FallbackInjectedChannelInitializer(
      originalInitializer, pipeline -> pipeline.addAfter(PACKET_DECODER, FALLBACK_PACKET_HANDLER,
        new FallbackBungeeInboundHandler()));

      final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
      unsafeField.setAccessible(true);
      final Unsafe unsafe = (Unsafe) unsafeField.get(null);

      final Object base = unsafe.staticFieldBase(childField);
      final long offset = unsafe.staticFieldOffset(childField);

      // Replace the original channel initializer with our new injected initializer
      unsafe.putObject(base, offset, injectedInitializer);
    } catch (Exception exception) {
      Sonar.get().getLogger().error("An error occurred while injecting {}", exception);
    }
  }
}
