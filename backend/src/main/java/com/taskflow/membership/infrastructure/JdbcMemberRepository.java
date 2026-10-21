package com.taskflow.membership.infrastructure;

import com.taskflow.membership.domain.MemberRole;
import com.taskflow.membership.domain.ProjectMember;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.ResultSet;
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
            try (CallableStatement cs = conn.prepareCall(
                    "CALL public.AddProjectMember(?, ?, ?, ?)")) {
                cs.setLong(1, projectId);
                cs.setLong(2, userId);
                cs.setString(3, role.name().toLowerCase());
                cs.registerOutParameter(4, Types.OTHER);
                cs.execute();
                return fetchList(conn, cs.getString(4), rowMapper()).get(0);
            }
        });
    }

    public List<ProjectMember> findByProjectId(long projectId) {
        return jdbc.execute((ConnectionCallback<List<ProjectMember>>) conn -> {
            try (CallableStatement cs = conn.prepareCall(
                    "CALL public.GetMembersByProjectId(?, ?)")) {
                cs.setLong(1, projectId);
                cs.registerOutParameter(2, Types.OTHER);
                cs.execute();
                return fetchList(conn, cs.getString(2), rowMapper());
            }
        });
    }

    public Optional<ProjectMember> findByProjectAndUser(long projectId, long userId) {
        return jdbc.execute((ConnectionCallback<Optional<ProjectMember>>) conn -> {
            try (CallableStatement cs = conn.prepareCall(
                    "CALL public.GetMemberByProjectAndUser(?, ?, ?)")) {
                cs.setLong(1, projectId);
                cs.setLong(2, userId);
                cs.registerOutParameter(3, Types.OTHER);
                cs.execute();
                return fetchOptional(conn, cs.getString(3), rowMapper());
            }
        });
    }

    public Optional<ProjectMember> updateRole(long projectId, long userId, MemberRole role) {
        return jdbc.execute((ConnectionCallback<Optional<ProjectMember>>) conn -> {
            try (CallableStatement cs = conn.prepareCall(
                    "CALL public.UpdateMemberRole(?, ?, ?, ?)")) {
                cs.setLong(1, projectId);
                cs.setLong(2, userId);
                cs.setString(3, role.name().toLowerCase());
                cs.registerOutParameter(4, Types.OTHER);
                cs.execute();
                return fetchOptional(conn, cs.getString(4), rowMapper());
            }
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
        try (Statement fetch = conn.createStatement();
             ResultSet rs = fetch.executeQuery("FETCH ALL FROM \"" + cursorName + "\"")) {
            List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapper.mapRow(rs, results.size()));
            }
            return results;
        }
    }

    private <T> Optional<T> fetchOptional(java.sql.Connection conn, String cursorName,
                                           RowMapper<T> mapper) throws java.sql.SQLException {
        List<T> rows = fetchList(conn, cursorName, mapper);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
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
