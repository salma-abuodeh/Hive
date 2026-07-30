export type NavIcon = 'feed' | 'community' | 'notifications' | 'people' | 'profile' | 'events' | 'signout';
export interface NavMenuItem {
  name: string;
  url: string;
  icon: NavIcon;
  requiresManageUsers?: boolean;
}