package com.taskflow.membership.infrastructure;

import com.taskflow.membership.domain.MemberRole;
import com.taskflow.membership.domain.ProjectMember;
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
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcMemberRepository {

    private final JdbcTemplate jdbc;

    public JdbcMemberRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ProjectMember add(long projectId, long userId, MemberRole role) {
        return jdbc.execute((ConnectionCallback<ProjectMember>) conn -> {
            callWithCursor(conn, "CALL public.AddProjectMember(?, ?, ?, ?)", ps -> {
                ps.setLong(1, projectId);
                ps.setLong(2, userId);
                ps.setString(3, role.name().toLowerCase());
            }, "add_member_cursor");
            return fetchList(conn, "add_member_cursor", rowMapper()).get(0);
        });
    }

    public List<ProjectMember> findByProjectId(long projectId) {
        return jdbc.execute((ConnectionCallback<List<ProjectMember>>) conn -> {
            callWithCursor(conn, "CALL public.GetMembersByProjectId(?, ?)",
                    ps -> ps.setLong(1, projectId), "get_members_cursor");
            return fetchList(conn, "get_members_cursor", rowMapper());
        });
    }

    public Optional<ProjectMember> findByProjectAndUser(long projectId, long userId) {
        return jdbc.execute((ConnectionCallback<Optional<ProjectMember>>) conn -> {
            callWithCursor(conn, "CALL public.GetMemberByProjectAndUser(?, ?, ?)",
                    ps -> { ps.setLong(1, projectId); ps.setLong(2, userId); },
                    "get_member_cursor");
            return fetchOptional(conn, "get_member_cursor", rowMapper());
        });
    }

    public Optional<ProjectMember> updateRole(long projectId, long userId, MemberRole role) {
        return jdbc.execute((ConnectionCallback<Optional<ProjectMember>>) conn -> {
            callWithCursor(conn, "CALL public.UpdateMemberRole(?, ?, ?, ?)", ps -> {
                ps.setLong(1, projectId);
                ps.setLong(2, userId);
                ps.setString(3, role.name().toLowerCase());
            }, "update_member_cursor");
            return fetchOptional(conn, "update_member_cursor", rowMapper());
        });
    }

    /**
     * Removes a member via the remove_project_member stored procedure (V6).
     * The procedure enforces that owners cannot be removed.
     */
    public boolean remove(long projectId, long userId) {
        jdbc.execute((ConnectionCallback<Void>) conn -> {
            try (CallableStatement cs = conn.prepareCall(
                    "CALL public.remove_project_member(?, ?)")) {
                cs.setLong(1, projectId);
                cs.setLong(2, userId);
                cs.execute();
            }
            return null;
        });
        return true;
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

    private RowMapper<ProjectMember> rowMapper() {
        return (rs, rowNum) -> new ProjectMember(
                rs.getLong("project_id"),
                rs.getLong("user_id"),
                MemberRole.valueOf(rs.getString("role").toUpperCase()),
                rs.getObject("created_at", OffsetDateTime.class).toInstant()
        );
    }
}
