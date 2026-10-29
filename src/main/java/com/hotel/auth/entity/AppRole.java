package com.hotel.auth.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "app_role")
public class AppRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "role_code", nullable = false, unique = true, length = 30)
    private String roleCode;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    public AppRole() {
    }

    public AppRole(String roleCode, String roleName) {
        this.roleCode = roleCode;
        this.roleName = roleName;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
