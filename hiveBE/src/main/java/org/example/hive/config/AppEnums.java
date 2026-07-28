package org.example.hive.config;

public final class AppEnums {

    private AppEnums() {
    }

    public enum CompanyType {
        COMPANY,
        SCHOOL
    }

    public enum PermissionAction {
        VIEW,
        CREATE,
        UPDATE,
        DELETE,
        ARCHIVE,
        INVITE,
        DEACTIVATE,
        ASSIGN_ROLE,
        MANAGE_MEMBERS,
        MANAGE_PERMISSIONS,
        RESET_PASSWORD,
        UPDATE_BRANDING
    }

    public enum PermissionResource {
        COMPANY,
        TEAM,
        ROLE,
        USER,
        EVENT
    }

    public enum EventVisibility {
        COMPANY,
        TEAM,
        PRIVATE
    }

    public enum RsvpStatus {
        INVITED,
        ACCEPTED,
        DECLINED,
        MAYBE
    }
}