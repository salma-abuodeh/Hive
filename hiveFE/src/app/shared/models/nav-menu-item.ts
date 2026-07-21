export type NavIcon = 'dashboard' | 'feed' | 'community' | 'notifications' | 'people' | 'profile' | 'saved' | 'signout';

export interface NavMenuItem {
  name: string;
  url: string;
  icon: NavIcon;
  requiresManageUsers?: boolean;
}
