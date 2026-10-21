package com.taskflow.project.infrastructure;

import com.taskflow.project.domain.Project;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcProjectRepository {

    private final JdbcTemplate jdbc;

    public JdbcProjectRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Creates a project and its owner membership atomically via the
     * create_project_with_owner stored procedure (V6 migration).
     */
    public Project save(long ownerId, String name, String description) {
        return jdbc.execute((ConnectionCallback<Project>) conn -> {
            try (CallableStatement cs = conn.prepareCall(
                    "CALL public.create_project_with_owner(?, ?, ?, ?, ?, ?)")) {
                cs.setLong(1, ownerId);
                cs.setString(2, name);
                if (description != null) cs.setString(3, description);
                else cs.setNull(3, Types.VARCHAR);
                cs.setNull(4, Types.BIGINT);
                cs.registerOutParameter(4, Types.BIGINT);
                cs.setNull(5, Types.TIMESTAMP);
                cs.registerOutParameter(5, Types.TIMESTAMP);
                cs.setNull(6, Types.TIMESTAMP);
                cs.registerOutParameter(6, Types.TIMESTAMP);
                cs.execute();
                long id = cs.getLong(4);
                Timestamp createdAtTs = cs.getTimestamp(5);
                Timestamp updatedAtTs = cs.getTimestamp(6);
                return new Project(id, ownerId, name, description,
                        createdAtTs.toInstant(), updatedAtTs.toInstant());
            }
        });
    }

    public Optional<Project> findById(long id) {
        return jdbc.execute((ConnectionCallback<Optional<Project>>) conn -> {
            callWithCursor(conn, "CALL public.GetProjectById(?, ?)",
                    ps -> ps.setLong(1, id), "get_project_by_id_cursor");
            return fetchOptional(conn, "get_project_by_id_cursor", rowMapper());
        });
    }

    public List<Project> findAll(int limit, int offset) {
        return jdbc.execute((ConnectionCallback<List<Project>>) conn -> {
            callWithCursor(conn, "CALL public.GetAllProjects(?, ?, ?)",
                    ps -> { ps.setInt(1, limit); ps.setInt(2, offset); },
                    "get_all_projects_cursor");
            return fetchList(conn, "get_all_projects_cursor", rowMapper());
        });
    }

    public List<Project> findByMemberId(long userId, int limit, int offset) {
        return jdbc.execute((ConnectionCallback<List<Project>>) conn -> {
            callWithCursor(conn, "CALL public.GetProjectsByMemberId(?, ?, ?, ?)",
                    ps -> { ps.setLong(1, userId); ps.setInt(2, limit); ps.setInt(3, offset); },
                    "get_projects_by_member_cursor");
            return fetchList(conn, "get_projects_by_member_cursor", rowMapper());
        });
    }

    public long countByMemberId(long userId) {
        return jdbc.execute((ConnectionCallback<Long>) conn -> {
            try (CallableStatement cs = conn.prepareCall(
                    "CALL public.CountProjectsByMemberId(?, ?)")) {
                cs.setLong(1, userId);
                cs.setNull(2, Types.BIGINT);
                cs.registerOutParameter(2, Types.BIGINT);
                cs.execute();
                return cs.getLong(2);
            }
        });
    }

    public long count() {
        return jdbc.execute((ConnectionCallback<Long>) conn -> {
            try (CallableStatement cs = conn.prepareCall(
                    "CALL public.CountAllProjects(?)")) {
                cs.setNull(1, Types.BIGINT);
                cs.registerOutParameter(1, Types.BIGINT);
                cs.execute();
                return cs.getLong(1);
            }
        });
    }

    public Optional<Project> update(long id, String name, String description) {
        return jdbc.execute((ConnectionCallback<Optional<Project>>) conn -> {
            callWithCursor(conn, "CALL public.UpdateProject(?, ?, ?, ?)", ps -> {
                ps.setLong(1, id);
                ps.setString(2, name);
                if (description != null) ps.setString(3, description);
                else ps.setNull(3, Types.VARCHAR);
            }, "update_project_cursor");
            return fetchOptional(conn, "update_project_cursor", rowMapper());
        });
    }

    public boolean deleteById(long id) {
        return jdbc.execute((ConnectionCallback<Boolean>) conn -> {
            try (CallableStatement cs = conn.prepareCall(
                    "CALL public.DeleteProject(?, ?)")) {
                cs.setLong(1, id);
                cs.setNull(2, Types.BIGINT);
                cs.registerOutParameter(2, Types.BIGINT);
                cs.execute();
                return cs.getLong(2) > 0;
            }
        });
    }

    // -----------------------------------------------------------------------
    // Shared helpers
    // -----------------------------------------------------------------------

    private <T> List<T> fetchList(java.sql.Connection conn, String cursorName,
                                   RowMapper<T> mapper) throws java.sql.SQLException {
        List<T> results = new ArrayList<>();
        try (Statement fetch = conn.createStatement();
             ResultSet rs = fetch.executeQuery("FETCH ALL FROM \"" + cursorName + "\"")) {
            while (rs.next()) {
                results.add(mapper.mapRow(rs, results.size()));
            }
        }
        try (Statement close = conn.createStatement()) {
            close.execute("CLOSE \"" + cursorName + "\"");
        }
        return results;
    }

    private <T> Optional<T> fetchOptional(java.sql.Connection conn, String cursorName,
                                           RowMapper<T> mapper) throws java.sql.SQLException {
        List<T> rows = fetchList(conn, cursorName, mapper);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @FunctionalInterface
    private interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private void callWithCursor(java.sql.Connection conn, String sql,
                                 ParamSetter setter, String cursorName)
            throws SQLException {
        PGobject cursor = new PGobject();
        cursor.setType("refcursor");
        cursor.setValue(cursorName);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setter.set(ps);
            ps.setObject(ps.getParameterMetaData().getParameterCount(), cursor);
            ps.execute();
        }
    }

    private RowMapper<Project> rowMapper() {
        return (rs, rowNum) -> new Project(
                rs.getLong("id"),
                rs.getLong("owner_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant()
        );
    }
}
