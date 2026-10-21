package com.taskflow.project.application;

import com.taskflow.membership.domain.MemberRole;
import com.taskflow.membership.domain.ProjectMember;
import com.taskflow.membership.infrastructure.JdbcMemberRepository;
import com.taskflow.project.api.CreateProjectRequest;
import com.taskflow.project.domain.Project;
import com.taskflow.project.infrastructure.JdbcProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Verifies Spring Cache AOP on ProjectService using an in-memory cache.
 * No full Spring Boot context, no database, no Redis required.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ProjectServiceCacheTest.TestConfig.class)
class ProjectServiceCacheTest {

    @Configuration
    @EnableCaching
    @Import(ProjectService.class)
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("projects-by-user");
        }

        @Bean
        JdbcProjectRepository repository() {
            return mock(JdbcProjectRepository.class);
        }

        @Bean
        JdbcMemberRepository memberRepository() {
            return mock(JdbcMemberRepository.class);
        }
    }

    @Autowired ProjectService service;
    @Autowired CacheManager cacheManager;
    @Autowired JdbcProjectRepository repository;
    @Autowired JdbcMemberRepository memberRepository;

    private static final long USER_ID = 7L;

    private Project project(long id) {
        return new Project(id, USER_ID, "P" + id, null, Instant.EPOCH, Instant.EPOCH);
    }

    @BeforeEach
    void resetState() {
        reset(repository, memberRepository);
        // Clear the cache so each test starts with a cold cache
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });
        when(repository.countByMemberId(USER_ID)).thenReturn(1L);
        when(repository.findByMemberId(eq(USER_ID), anyInt(), anyInt()))
                .thenReturn(List.of(project(1L)));
    }

    @Test
    void listProjects_secondCall_doesNotHitRepository() {
        service.listProjects(0, 20, USER_ID);
        service.listProjects(0, 20, USER_ID);

        verify(repository, times(1)).findByMemberId(eq(USER_ID), eq(20), eq(0));
        verify(repository, times(1)).countByMemberId(USER_ID);
    }

    @Test
    void createProject_evictsCacheForCaller() {
        when(repository.save(eq(USER_ID), anyString(), any()))
                .thenReturn(project(2L));

        service.listProjects(0, 20, USER_ID);
        service.createProject(new CreateProjectRequest("New", null), USER_ID);
        service.listProjects(0, 20, USER_ID);

        verify(repository, times(2)).findByMemberId(eq(USER_ID), eq(20), eq(0));
    }

    @Test
    void deleteProject_evictsCacheForCaller() {
        when(repository.findById(1L)).thenReturn(Optional.of(project(1L)));
        when(memberRepository.findByProjectAndUser(1L, USER_ID))
                .thenReturn(Optional.of(
                        new ProjectMember(1L, USER_ID, MemberRole.OWNER, Instant.EPOCH)));
        when(repository.deleteById(1L)).thenReturn(true);

        service.listProjects(0, 20, USER_ID);
        service.deleteProject(1L, USER_ID);
        service.listProjects(0, 20, USER_ID);

        verify(repository, times(2)).findByMemberId(eq(USER_ID), eq(20), eq(0));
    }

    @Test
    void cacheIsIsolatedPerUser() {
        long otherUser = 99L;
        when(repository.countByMemberId(otherUser)).thenReturn(0L);
        when(repository.findByMemberId(eq(otherUser), anyInt(), anyInt()))
                .thenReturn(List.of());

        service.listProjects(0, 20, USER_ID);
        service.listProjects(0, 20, otherUser);
        service.listProjects(0, 20, USER_ID);
        service.listProjects(0, 20, otherUser);

        verify(repository, times(1)).findByMemberId(eq(USER_ID), eq(20), eq(0));
        verify(repository, times(1)).findByMemberId(eq(otherUser), eq(20), eq(0));
    }
}
