/*
 * Copyright (C) 2023 jones
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

package jones.sonar.api.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jones.sonar.api.Sonar;
import jones.sonar.api.config.SonarConfiguration;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.Collection;
import java.util.Objects;
import java.util.Vector;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class MySQLDatabase implements Database {
  @Getter
  private @Nullable HikariDataSource dataSource;
  public static final String VERIFIED_TABLE = "verified_ips";
  public static final String BLACKLIST_TABLE = "blacklisted_ips";
  public static final String IP_COLUMN = "ip_address";
  @Getter
  @Setter
  private boolean loadedFromDatabase;

  @Override
  public void initialize(final @NotNull SonarConfiguration config) {
    try {
      // Register MySQL driver
      final URL[] url = new URL[] {
        new URL("https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar"),
        new URL("https://repo1.maven.org/maven2/com/zaxxer/HikariCP/4.0.3/HikariCP-4.0.3.jar")
      };

      try (final URLClassLoader classLoader = new URLClassLoader(url, getClass().getClassLoader())) {
        val driverClass = (Class<? extends Driver>) classLoader.loadClass("com.mysql.cj.jdbc.Driver");
        DriverManager.registerDriver(driverClass.newInstance());

        val configClass = (Class<HikariConfig>) classLoader.loadClass("com.zaxxer.hikari.HikariConfig");
        final HikariConfig hikariConfig = configClass.newInstance();

        hikariConfig.setJdbcUrl(
          "jdbc:mysql://"
            + config.DATABASE_URL
            + ":" + config.DATABASE_PORT
            + "/" + config.DATABASE_NAME
        );
        hikariConfig.setUsername(config.DATABASE_USERNAME);
        hikariConfig.setPassword(config.DATABASE_PASSWORD);

        dataSource = new HikariDataSource(hikariConfig);

        createTable(IP_COLUMN, VERIFIED_TABLE);
        createTable(IP_COLUMN, BLACKLIST_TABLE);
      }
    } catch (Throwable throwable) {
      Sonar.get().getLogger().error("Failed to connect to database: {}", throwable);
    }
  }

  @Override
  public void purge() {
    Objects.requireNonNull(dataSource);

    clear(VERIFIED_TABLE);
    clear(BLACKLIST_TABLE);
  }

  @Override
  public void dispose() {
    Objects.requireNonNull(dataSource);

    dataSource.close();
    dataSource = null;
  }

  public Collection<String> getListFromTable(final @NotNull String table,
                                             final @NotNull String column) {
    Objects.requireNonNull(getDataSource());

    final Collection<String> output = new Vector<>();

    try (final PreparedStatement statement = getDataSource().getConnection().prepareStatement(
      "select `" + column + "` from `" + table + "` limit " + Sonar.get().getConfig().DATABASE_QUERY_LIMIT
    )) {
      final ResultSet resultSet = statement.executeQuery();

      while (resultSet.next()) {
        output.add(resultSet.getString(column));
      }
    } catch (Throwable throwable) {
      throw new IllegalStateException(throwable);
    }
    return output;
  }

  public void addListToTable(final @NotNull String table,
                             final @NotNull String column,
                             final @NotNull Collection<String> collection) {
    Objects.requireNonNull(getDataSource());

    if (collection.isEmpty()) return;

    try (final PreparedStatement insertStatement = getDataSource().getConnection().prepareStatement(
      "insert ignore into `" + table + "` (" + column + ") values (?)");
         final PreparedStatement selectStatement = getDataSource().getConnection().prepareStatement(
           "select count(*) from `" + table + "` where `" + column + "` = ?")
    ) {
      for (final String v : collection) {
        selectStatement.setString(1, v);
        ResultSet resultSet = selectStatement.executeQuery();
        resultSet.next();
        int count = resultSet.getInt(1);

        if (count == 0) {
          insertStatement.setString(1, v);
          insertStatement.addBatch();
        }
      }

      insertStatement.executeBatch();
    } catch (Throwable throwable) {
      Sonar.get().getLogger().error("Error executing addListToTable:");
      throwable.printStackTrace();
      throw new IllegalStateException(throwable);
    }
  }

  @Override
  public void remove(final @NotNull String table,
                     final @NotNull String column,
                     final @NotNull String entry) {
    Objects.requireNonNull(dataSource);

    try (final PreparedStatement statement = dataSource.getConnection().prepareStatement(
      "delete from `" + table + "` where `" + column + "` = ?"
    )) {
      statement.setObject(1, entry);
      statement.execute();
    } catch (SQLException exception) {
      throw new IllegalStateException(exception);
    }
  }

  @Override
  public void clear(final @NotNull String table) {
    Objects.requireNonNull(dataSource);

    try {
      prepareRawStatement(dataSource.getConnection(), "truncate table `" + table + "`");
    } catch (SQLException exception) {
      throw new IllegalStateException(exception);
    }
  }

  @SuppressWarnings("SameParameterValue")
  private void createTable(final String column, final String @NotNull ... tables) throws SQLException {
    Objects.requireNonNull(dataSource);

    for (final String name : tables) {
      try (final PreparedStatement statement = dataSource.getConnection().prepareStatement(
        "create table if not exists " + name + " (`" + column + "` varchar(16))"
      )) {
        statement.execute();
      }
    }
  }

  private static void prepareRawStatement(final @NotNull Connection connection, final @NotNull String sql) {
    try (final PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.execute();
    } catch (Throwable throwable) {
      throw new IllegalStateException(throwable);
    }
  }
}
