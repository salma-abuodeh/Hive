import { Routes } from '@angular/router';
import { Login } from './features/auth/pages/login/login';
import { DashboardHome } from './features/dashboard/pages/dashboard-home/dashboard-home';
import { CommunityFeed } from './features/community/pages/community-feed/community-feed';
import { NotificationsHome } from './features/notifications/pages/notifications-home/notifications-home';
import { AuthLayout } from './layouts/auth-layout/auth-layout';
import { AppLayout } from './layouts/app-layout/app-layout';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { Signup } from './features/auth/pages/signup/signup';



export const routes: Routes = [
  {
    path: '',
    component: AuthLayout,
    children: [
      { path: '', redirectTo: 'login', pathMatch: 'full' },
      { path: 'login', component: Login, canActivate: [guestGuard]},
      { path: 'signup', component: Signup, canActivate: [guestGuard]}
    ]
  },
  {
    path: '',
    component: AppLayout,
    children: [
      { path: 'dashboard', component: DashboardHome, canActivate: [authGuard] },
      { path: 'community', component: CommunityFeed, canActivate: [authGuard] },
      { path: 'notifications', component: NotificationsHome, canActivate: [authGuard] }
    ]
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
