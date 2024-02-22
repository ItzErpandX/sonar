/*
 * Copyright (C) 2023-2024 Sonar Contributors
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

package xyz.jonesdev.sonar.api.jvm;

import com.sun.management.OperatingSystemMXBean;
import lombok.experimental.UtilityClass;

import java.lang.management.ManagementFactory;

@UtilityClass
public class JVMProcessInformation {
  private final Runtime RUNTIME = Runtime.getRuntime();
  private final OperatingSystemMXBean MX = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
  private final char[] MEMORY_UNITS = {'k', 'M', 'G', 'T', 'P', 'E'};

  public String formatMemory(final long size) {
    if (size < 1024L) {
      return size + " B";
    }
    final int group = (63 - Long.numberOfLeadingZeros(size)) / 10;
    final double formattedSize = (double) size / (1L << (group * 10));
    final char unit = MEMORY_UNITS[group - 1];
    return String.format("%.1f %sB", formattedSize, unit);
  }

  public int getVirtualCores() {
    return RUNTIME.availableProcessors();
  }

  public double getProcessCPUUsage() {
    return MX.getProcessCpuLoad() * 100;
  }

  public double getAverageProcessCPUUsage() {
    return getProcessCPUUsage() / getVirtualCores();
  }

  public double getSystemCPUUsage() {
    return MX.getSystemCpuLoad() * 100;
  }

  public double getAverageSystemCPUUsage() {
    return getSystemCPUUsage() / getVirtualCores();
  }

  public double getSystemLoadAverage() {
    return MX.getSystemLoadAverage() * 100;
  }

  public long getMaxMemory() {
    return RUNTIME.maxMemory();
  }

  public long getTotalMemory() {
    return RUNTIME.totalMemory();
  }

  public long getFreeMemory() {
    return RUNTIME.freeMemory();
  }

  public long getUsedMemory() {
    return getTotalMemory() - getFreeMemory();
  }
}