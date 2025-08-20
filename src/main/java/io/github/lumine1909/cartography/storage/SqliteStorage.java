package io.github.lumine1909.cartography.storage;

import io.github.lumine1909.cartography.command.Argument;

import java.sql.*;

public class SqliteStorage {

    private final Connection connection;

    public SqliteStorage(String dbPath) {
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            initTable();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private void initTable() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS map_info (
                        url TEXT NOT NULL,
                        use_dithering INTEGER NOT NULL,
                        length INTEGER NOT NULL,
                        width INTEGER NOT NULL,
                        keep_scale INTEGER NOT NULL,
                        start INTEGER NOT NULL,
                        end INTEGER NOT NULL,
                        PRIMARY KEY (url, use_dithering, length, width, keep_scale)
                    )
                """);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void saveRange(Argument argument, int start, int end) {
        String sql = """
                INSERT INTO map_info (url, use_dithering, length, width, keep_scale, start, end)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(url, use_dithering, length, width, keep_scale)
                DO UPDATE SET start = excluded.start, end = excluded.end
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, argument.url());
            ps.setInt(2, argument.useDithering() ? 1 : 0);
            ps.setInt(3, argument.length());
            ps.setInt(4, argument.width());
            ps.setInt(5, argument.keepScale() ? 1 : 0);
            ps.setInt(6, start);
            ps.setInt(7, end);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int[] loadRange(Argument argument) {
        String sql = """
                SELECT start, end FROM map_info
                WHERE url = ? AND use_dithering = ? AND length = ? AND width = ? AND keep_scale = ?
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, argument.url());
            ps.setInt(2, argument.useDithering() ? 1 : 0);
            ps.setInt(3, argument.length());
            ps.setInt(4, argument.width());
            ps.setInt(5, argument.keepScale() ? 1 : 0);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new int[]{rs.getInt("start"), rs.getInt("end")};
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            connection.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
