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
  | 'poll'
  | 'like'
  | 'love'
  | 'laugh'
  | 'sad'
  | 'angry'
  | 'comment'
  | 'share'
  | 'attach'
  | 'document'
  | 'close'
  | 'more'
  | 'send'
  | 'pencil'
  | 'link';
export type NavIcon = AppIcon;

export interface NavMenuItem {
  name: string;
  url: string;
  icon: AppIcon;
  requiresManageUsers?: boolean;
  requiresManageTeams?: boolean;
  requiresManagement?: boolean;
}