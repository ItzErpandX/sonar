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

/*
 * Copyright 2021 Andrew Steinborn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package xyz.jonesdev.sonar.common.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.Version;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.nbt.*;
import org.jetbrains.annotations.NotNull;
import xyz.jonesdev.sonar.common.util.exception.QuietDecoderException;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.UUID;

// https://github.com/PaperMC/Velocity/blob/dev/3.0.0/proxy/src/main/java/com/velocitypowered/proxy/protocol/ProtocolUtils.java
@UtilityClass
public class ProtocolUtil {
  public static final boolean DEBUG = Boolean.getBoolean("sonar.debug-traces");
  public static final String BRAND_CHANNEL_LEGACY = "MC|Brand";
  public static final String BRAND_CHANNEL = "minecraft:brand";
  private static final int[] VAR_INT_LENGTHS = new int[65];

  static {
    for (int i = 0; i <= 32; ++i) {
      VAR_INT_LENGTHS[i] = (int) Math.ceil((31d - (i - 1)) / 7d);
    }
    VAR_INT_LENGTHS[32] = 1;
  }

  public static int varIntBytes(final int value) {
    return VAR_INT_LENGTHS[Integer.numberOfLeadingZeros(value)];
  }

  public static void checkNettyVersion() {
    final Version version = Version.identify().getOrDefault("netty-all", Version.identify().get("netty-common"));

    // We're pretty much only doing this to avoid incompatibilities on Bukkit,
    // so we don't really care if the version couldn't be resolved.
    if (version == null) {
      return;
    }

    final String[] artifactVersion = version.artifactVersion().split("\\.");
    final int major = Integer.parseInt(artifactVersion[0]);
    final int minor = Integer.parseInt(artifactVersion[1]);

    // Enforce Netty >4.1.x
    if (major < 4 || (major == 4 && minor < 1)) {
      throw new IllegalStateException("Your Netty version is too old to run Sonar! Please use Netty >4.1.x.");
    }
  }

  public static int readVarInt(final @NotNull ByteBuf byteBuf) {
    final int readable = byteBuf.readableBytes();
    if (readable == 0) {
      throw DEBUG ? new DecoderException("Empty buffer") : QuietDecoderException.INSTANCE;
    }

    // We can read at least one byte, and this should be a common case
    int k = byteBuf.readByte();
    if ((k & 0x80) != 128) {
      return k;
    }

    // In case decoding one byte was not enough, use a loop to decode up to the next 4 bytes
    final int maxRead = Math.min(5, readable);
    int i = k & 0x7F;
    for (int j = 1; j < maxRead; j++) {
      k = byteBuf.readByte();
      i |= (k & 0x7F) << j * 7;
      if ((k & 0x80) != 128) {
        return i;
      }
    }
    throw DEBUG ? new DecoderException("Bad VarInt") : QuietDecoderException.INSTANCE;
  }

  public static void writeVarInt(final ByteBuf byteBuf, final int value) {
    // Peel the one and two byte count cases explicitly as they are the most common VarInt sizes
    // that the proxy will write, to improve inlining.
    if ((value & (0xFFFFFFFF << 7)) == 0) {
      byteBuf.writeByte(value);
    } else if ((value & (0xFFFFFFFF << 14)) == 0) {
      int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
      byteBuf.writeShort(w);
    } else {
      writeVarIntFull(byteBuf, value);
    }
  }

  private void writeVarIntFull(final ByteBuf byteBuf, final int value) {
    // See https://steinborn.me/posts/performance/how-fast-can-you-write-a-varint/
    if ((value & (0xFFFFFFFF << 7)) == 0) {
      byteBuf.writeByte(value);
    } else if ((value & (0xFFFFFFFF << 14)) == 0) {
      int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
      byteBuf.writeShort(w);
    } else if ((value & (0xFFFFFFFF << 21)) == 0) {
      int w = (value & 0x7F | 0x80) << 16 | ((value >>> 7) & 0x7F | 0x80) << 8 | (value >>> 14);
      byteBuf.writeMedium(w);
    } else if ((value & (0xFFFFFFFF << 28)) == 0) {
      int w = (value & 0x7F | 0x80) << 24 | (((value >>> 7) & 0x7F | 0x80) << 16)
        | ((value >>> 14) & 0x7F | 0x80) << 8 | (value >>> 21);
      byteBuf.writeInt(w);
    } else {
      int w = (value & 0x7F | 0x80) << 24 | ((value >>> 7) & 0x7F | 0x80) << 16
        | ((value >>> 14) & 0x7F | 0x80) << 8 | ((value >>> 21) & 0x7F | 0x80);
      byteBuf.writeInt(w);
      byteBuf.writeByte(value >>> 28);
    }
  }

  public static void writeVarLong(final ByteBuf byteBuf, final long value) {
    // Peel the one and two byte count cases explicitly as they are the most common VarLong sizes
    // that the proxy will write, to improve inlining.
    if ((value & 0xFFFFFFFFFFFFFF80L) == 0L) {
      byteBuf.writeByte((byte) value);
    } else if ((value & 0xFFFFFFFFFFFFC000L) == 0L) {
      int w = (int) ((value & 0x7FL | 0x80L) << 8 | value >>> 7);
      byteBuf.writeShort(w);
    } else {
      writeVarLongFull(byteBuf, value);
    }
  }

  private void writeVarLongFull(final ByteBuf byteBuf, final long value) {
    if ((value & 0xFFFFFFFFFFFFFF80L) == 0L) {
      byteBuf.writeByte((byte) value);
    } else if ((value & 0xFFFFFFFFFFFFC000L) == 0L) {
      int w = (int) ((value & 0x7FL | 0x80L) << 8 | value >>> 7);
      byteBuf.writeShort(w);
    } else if ((value & 0xFFFFFFFFFFE00000L) == 0L) {
      int w = (int) ((value & 0x7FL | 0x80L) << 16 | (value >>> 7 & 0x7FL | 0x80L) << 8 | value >>> 14);
      byteBuf.writeMedium(w);
    } else if ((value & 0xFFFFFFFFF0000000L) == 0L) {
      int w =
        (int) ((value & 0x7FL | 0x80L) << 24 | (value >>> 7 & 0x7FL | 0x80L) << 16 | (value >>> 14 & 0x7FL | 0x80L) << 8 | value >>> 21);
      byteBuf.writeInt(w);
    } else {
      long l =
        (value & 0x7FL | 0x80L) << 24 | (value >>> 7 & 0x7FL | 0x80L) << 16 | (value >>> 14 & 0x7FL | 0x80L) << 8 | (value >>> 21 & 0x7FL | 0x80L);
      if ((value & 0xFFFFFFF800000000L) == 0L) {
        int w =
          (int) l;
        byteBuf.writeInt(w);
        byteBuf.writeByte((int) (value >>> 28));
      } else if ((value & 0xFFFFFC0000000000L) == 0L) {
        int w =
          (int) l;
        int w2 = (int) ((value >>> 28 & 0x7FL | 0x80L) << 8 | value >>> 35);
        byteBuf.writeInt(w);
        byteBuf.writeShort(w2);
      } else if ((value & 0xFFFE000000000000L) == 0L) {
        int w =
          (int) l;
        int w2 = (int) ((value >>> 28 & 0x7FL | 0x80L) << 16 | (value >>> 35 & 0x7FL | 0x80L) << 8 | value >>> 42);
        byteBuf.writeInt(w);
        byteBuf.writeMedium(w2);
      } else {
        long w =
          (value & 0x7FL | 0x80L) << 56 | (value >>> 7 & 0x7FL | 0x80L) << 48 | (value >>> 14 & 0x7FL | 0x80L) << 40 | (value >>> 21 & 0x7FL | 0x80L) << 32 | (value >>> 28 & 0x7FL | 0x80L) << 24 | (value >>> 35 & 0x7FL | 0x80L) << 16 | (value >>> 42 & 0x7FL | 0x80L) << 8 | value >>> 49;
        if ((value & 0xFF00000000000000L) == 0L) {
          byteBuf.writeLong(w);
        } else if ((value & Long.MIN_VALUE) == 0L) {
          byteBuf.writeLong(w);
          byteBuf.writeByte((byte) (value >>> 56));
        } else {
          int w2 = (int) ((value >>> 56 & 0x7FL | 0x80L) << 8 | value >>> 63);
          byteBuf.writeLong(w);
          byteBuf.writeShort(w2);
        }
      }
    }
  }

  public static @NotNull UUID readUUID(final @NotNull ByteBuf byteBuf) {
    return new UUID(byteBuf.readLong(), byteBuf.readLong());
  }

  public static byte @NotNull [] readByteArray(final ByteBuf byteBuf) {
    return readByteArray(byteBuf, Short.MAX_VALUE);
  }

  public static byte @NotNull [] readByteArray(final ByteBuf byteBuf, final int cap) {
    int length = readVarInt(byteBuf);
    checkState(length >= 0, "Got a negative-length array");
    checkState(length <= cap, "Bad array size");
    checkState(byteBuf.isReadable(length), "Trying to read an array that is too long");
    byte[] array = new byte[length];
    byteBuf.readBytes(array);
    return array;
  }

  public static @NotNull String readString(final ByteBuf byteBuf) throws DecoderException {
    return readString(byteBuf, Short.MAX_VALUE);
  }

  public static @NotNull String readString(final ByteBuf byteBuf,
                                           final int cap) throws DecoderException {
    final int length = readVarInt(byteBuf);
    return readString(byteBuf, cap, length);
  }

  public static @NotNull String readString(final @NotNull ByteBuf byteBuf,
                                           final int cap,
                                           final int length) throws DecoderException {
    checkState(length >= 0, "Got a negative-length string");
    checkState(length <= cap * 3, "Bad string size");
    checkState(byteBuf.isReadable(length), "Tried to read a too-long string");
    final String str = byteBuf.toString(byteBuf.readerIndex(), length, StandardCharsets.UTF_8);
    byteBuf.readerIndex(byteBuf.readerIndex() + length);
    checkState(str.length() <= cap, "Got a too-long string");
    return str;
  }

  public static void writeString(final ByteBuf byteBuf, final @NotNull CharSequence str) {
    final int size = ByteBufUtil.utf8Bytes(str);
    writeVarInt(byteBuf, size);
    byteBuf.writeCharSequence(str, StandardCharsets.UTF_8);
  }

  public static void writeUUID(final @NotNull ByteBuf byteBuf, final @NotNull UUID uuid) {
    byteBuf.writeLong(uuid.getMostSignificantBits());
    byteBuf.writeLong(uuid.getLeastSignificantBits());
  }

  public static void writeUUIDIntArray(final @NotNull ByteBuf byteBuf, final @NotNull UUID uuid) {
    byteBuf.writeInt((int) (uuid.getMostSignificantBits() >> 32));
    byteBuf.writeInt((int) uuid.getMostSignificantBits());
    byteBuf.writeInt((int) (uuid.getLeastSignificantBits() >> 32));
    byteBuf.writeInt((int) uuid.getLeastSignificantBits());
  }

  public static void writeArray(final ByteBuf byteBuf, final byte @NotNull [] bytes) {
    checkState(bytes.length < Short.MAX_VALUE, "Too long array");
    writeVarInt(byteBuf, bytes.length);
    byteBuf.writeBytes(bytes);
  }

  public static void writeStringArray(final ByteBuf byteBuf, final String @NotNull [] stringArray) {
    writeVarInt(byteBuf, stringArray.length);
    for (final String s : stringArray) {
      writeString(byteBuf, s);
    }
  }

  // https://github.com/Nan1t/NanoLimbo/blob/main/src/main/java/ua/nanit/limbo/protocol/ByteMessage.java#L276
  public <E extends Enum<E>> void writeEnumSet(final ByteBuf byteBuf,
                                               final EnumSet<E> enumset,
                                               final @NotNull Class<E> oclass) {
    final E[] enums = oclass.getEnumConstants();
    final BitSet bits = new BitSet(enums.length);

    for (int i = 0; i < enums.length; ++i) {
      bits.set(i, enumset.contains(enums[i]));
    }

    writeFixedBitSet(byteBuf, bits, enums.length);
  }

  private static void writeFixedBitSet(final ByteBuf byteBuf, final @NotNull BitSet bits, final int size) {
    if (bits.length() > size) {
      throw new StackOverflowError("BitSet too large (expected " + size + " got " + bits.size() + ")");
    }
    byteBuf.writeBytes(Arrays.copyOf(bits.toByteArray(), (size + 8) >> 3));
  }

  public static void writeCompoundTag(final @NotNull ByteBuf byteBuf, final @NotNull CompoundBinaryTag compoundTag) {
    try {
      BinaryTagIO.writer().write(compoundTag, (DataOutput) new ByteBufOutputStream(byteBuf));
    } catch (IOException exception) {
      throw new EncoderException("Unable to encode NBT CompoundTag");
    }
  }

  // https://github.com/Nan1t/NanoLimbo/blob/main/src/main/java/ua/nanit/limbo/protocol/ByteMessage.java#L219
  public static void writeNamelessCompoundTag(final @NotNull ByteBuf byteBuf, final @NotNull BinaryTag binaryTag) {
    try (final ByteBufOutputStream output = new ByteBufOutputStream(byteBuf)) {
      // TODO: Find a way to improve this...
      output.writeByte(binaryTag.type().id());
      if (binaryTag instanceof CompoundBinaryTag) {
        CompoundBinaryTag tag = (CompoundBinaryTag) binaryTag;
        tag.type().write(tag, output);
      } else if (binaryTag instanceof ByteBinaryTag) {
        ByteBinaryTag tag = (ByteBinaryTag) binaryTag;
        tag.type().write(tag, output);
      } else if (binaryTag instanceof ShortBinaryTag) {
        ShortBinaryTag tag = (ShortBinaryTag) binaryTag;
        tag.type().write(tag, output);
      } else if (binaryTag instanceof IntBinaryTag) {
        IntBinaryTag tag = (IntBinaryTag) binaryTag;
        tag.type().write(tag, output);
      } else if (binaryTag instanceof LongBinaryTag) {
        LongBinaryTag tag = (LongBinaryTag) binaryTag;
        tag.type().write(tag, output);
      } else if (binaryTag instanceof DoubleBinaryTag) {
        DoubleBinaryTag tag = (DoubleBinaryTag) binaryTag;
        tag.type().write(tag, output);
      } else if (binaryTag instanceof StringBinaryTag) {
        StringBinaryTag tag = (StringBinaryTag) binaryTag;
        tag.type().write(tag, output);
      } else if (binaryTag instanceof ListBinaryTag) {
        ListBinaryTag tag = (ListBinaryTag) binaryTag;
        tag.type().write(tag, output);
      } else if (binaryTag instanceof EndBinaryTag) {
        EndBinaryTag tag = (EndBinaryTag) binaryTag;
        tag.type().write(tag, output);
      }
    } catch (IOException exception) {
      throw new EncoderException("Unable to encode NBT CompoundTag");
    }
  }

  public static int readExtendedForgeShort(final @NotNull ByteBuf byteBuf) {
    int low = byteBuf.readUnsignedShort();
    int high = 0;
    if ((low & 0x8000) != 0) {
      low = low & 0x7FFF;
      high = byteBuf.readUnsignedByte();
    }
    return ((high & 0xFF) << 15) | low;
  }

  private void checkState(final boolean expression, final String message) {
    if (!expression) {
      throw DEBUG ? new DecoderException(message) : QuietDecoderException.INSTANCE;
    }
  }
}
