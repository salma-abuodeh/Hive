export type AppIcon =
  | 'dashboard'
  | 'feed'
  | 'community'
  | 'notifications'
  | 'people'
  | 'company'
  | 'profile'
  | 'saved'
  | 'signout'
  | 'search'
  | 'plus'
  | 'chevronDown'
  | 'events'
  | 'chat'
  | 'message'
  | 'send';
export type NavIcon = AppIcon;

export interface NavMenuItem {
  name: string;
  url: string;
  icon: AppIcon;
  requiresManageUsers?: boolean;
  requiresManageTeams?: boolean;
  requiresManagement?: boolean;
}
