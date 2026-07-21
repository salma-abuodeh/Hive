package org.example.hive.security;

import org.example.hive.config.AppEnums.PermissionAction;
import org.example.hive.config.AppEnums.PermissionResource;

public final class Permissions {

    private Permissions() {
    }

    //Company
    public static final String COMPANY_VIEW = of(PermissionResource.COMPANY, PermissionAction.VIEW);
    public static final String COMPANY_UPDATE = of(PermissionResource.COMPANY, PermissionAction.UPDATE);
    public static final String COMPANY_UPDATE_BRANDING = of(PermissionResource.COMPANY, PermissionAction.UPDATE_BRANDING);
    public static final String COMPANY_ARCHIVE = of(PermissionResource.COMPANY, PermissionAction.ARCHIVE);

    //Team
    public static final String TEAM_VIEW = of(PermissionResource.TEAM, PermissionAction.VIEW);
    public static final String TEAM_CREATE = of(PermissionResource.TEAM, PermissionAction.CREATE);
    public static final String TEAM_UPDATE = of(PermissionResource.TEAM, PermissionAction.UPDATE);
    public static final String TEAM_DELETE = of(PermissionResource.TEAM, PermissionAction.DELETE);
    public static final String TEAM_MANAGE_MEMBERS = of(PermissionResource.TEAM, PermissionAction.MANAGE_MEMBERS);

    //Role
    public static final String ROLE_VIEW = of(PermissionResource.ROLE, PermissionAction.VIEW);
    public static final String ROLE_CREATE = of(PermissionResource.ROLE, PermissionAction.CREATE);
    public static final String ROLE_UPDATE = of(PermissionResource.ROLE, PermissionAction.UPDATE);
    public static final String ROLE_DELETE = of(PermissionResource.ROLE, PermissionAction.DELETE);
    public static final String ROLE_MANAGE_PERMISSIONS = of(PermissionResource.ROLE, PermissionAction.MANAGE_PERMISSIONS);

    //User
    public static final String USER_VIEW = of(PermissionResource.USER, PermissionAction.VIEW);
    public static final String USER_INVITE = of(PermissionResource.USER, PermissionAction.INVITE);
    public static final String USER_UPDATE = of(PermissionResource.USER, PermissionAction.UPDATE);
    public static final String USER_DEACTIVATE = of(PermissionResource.USER, PermissionAction.DEACTIVATE);
    public static final String USER_ASSIGN_ROLE = of(PermissionResource.USER, PermissionAction.ASSIGN_ROLE);
    public static final String USER_RESET_PASSWORD = of(PermissionResource.USER, PermissionAction.RESET_PASSWORD);

    /** Platform-only operations (e.g. manage users across all companies). */
    public static final String PLATFORM_MANAGE = "PLATFORM_MANAGE";

    public static String of(PermissionResource resource, PermissionAction action) {
        return resource.name() + "_" + action.name();
    }
}