import { Routes } from '@angular/router';
import { Login } from './features/auth/pages/login/login';
import { DashboardHome } from './features/dashboard/pages/dashboard-home/dashboard-home';
import { CommunityFeed } from './features/community/pages/community-feed/community-feed';
import { NotificationsHome } from './features/notifications/pages/notifications-home/notifications-home';
import { AuthLayout } from './layouts/auth-layout/auth-layout';
import { AppLayout } from './layouts/app-layout/app-layout';

export const routes: Routes = [
  {
    path: '',
    component: AuthLayout,
    children: [
      { path: '', redirectTo: 'login', pathMatch: 'full' },
      { path: 'login', component: Login }
    ]
  },
  {
    path: '',
    component: AppLayout,
    children: [
      { path: 'dashboard', component: DashboardHome },
      { path: 'community', component: CommunityFeed },
      { path: 'notifications', component: NotificationsHome }
    ]
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
