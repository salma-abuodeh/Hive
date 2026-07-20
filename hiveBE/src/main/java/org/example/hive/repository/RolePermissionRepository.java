package org.example.hive.repository;

import org.example.hive.model.Permission;
import org.example.hive.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    @Query("""
            select rp.permission from RolePermission rp
            where rp.role.id = :roleId and rp.permission.active = true
            """)
    List<Permission> findActivePermissionsByRoleId(@Param("roleId") Long roleId);
}
