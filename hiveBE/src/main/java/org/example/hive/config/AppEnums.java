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
        EVENT,
        POLL
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

    public enum PostType {
        ANNOUNCEMENT, TEXT
    }

    public enum VisibilityType {
        COMPANY,
        TEAM
    }

    public enum ReactionType {
        LIKE,
        LOVE,
        LAUGHING,
        SAD,
        ANGRY
    }
    public enum AttachmentContext {
        AVATAR,
        COMPANY_LOGO,
        TEAM_LOGO,
        EVENT_COVER,
        POST,
        COMMENT,
        CHAT_MESSAGE,
        COMPANY_APPLICATION_DOCUMENT,
        MEMBERSHIP_REQUEST_DOCUMENT
    }

    public enum AttachmentType {
        IMAGE,
        DOCUMENT
    }
}