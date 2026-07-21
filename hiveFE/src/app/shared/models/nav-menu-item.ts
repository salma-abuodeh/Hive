export type NavIcon = 'dashboard' | 'feed' | 'community' | 'notifications' | 'people' | 'company' | 'profile' | 'saved' | 'signout';

export interface NavMenuItem {
  name: string;
  url: string;
  icon: NavIcon;
  requiresManageUsers?: boolean;
  requiresManageTeams?: boolean;
}
