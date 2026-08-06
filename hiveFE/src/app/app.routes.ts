import { Routes } from '@angular/router';
import { Login } from './features/auth/pages/login/login';
import { DashboardHome } from './features/dashboard/pages/dashboard-home/dashboard-home';
import { NotificationsHome } from './features/notifications/pages/notifications-home/notifications-home';
import { FeedHome } from './features/feed/pages/feed-home/feed-home';
import { PostDetail } from './features/feed/pages/post-detail/post-detail';
import { SavedPosts } from './features/feed/pages/saved-posts/saved-posts';
import { CompanyHome } from './features/company/pages/company-home/company-home';
import { AuthLayout } from './layouts/auth-layout/auth-layout';
import { AppLayout } from './layouts/app-layout/app-layout';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { Signup } from './features/auth/pages/signup/signup';
import { Profile } from './features/users/pages/profile/profile';
import { UsersList } from './features/users/pages/users-list/users-list';
import { adminGuard } from './core/guards/admin.guard';
import { EventsList } from './features/events/pages/events-list/events-list';
import { ManagementHome } from './features/management/pages/management-home/management-home';
import { managementGuard } from './core/guards/management.guard';
import { ChatsHome } from './features/chats/pages/chats-home/chats-home';

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
      { path: 'feed', component: FeedHome, canActivate: [authGuard] },
      { path: 'chats', component: ChatsHome, canActivate: [authGuard] },
      { path: 'feed/:id', component: PostDetail, canActivate: [authGuard] },
      { path: 'saved', component: SavedPosts, canActivate: [authGuard] },
      { path: 'company', component: CompanyHome, canActivate: [authGuard] },
      { path: 'notifications', component: NotificationsHome, canActivate: [authGuard] },
      { path: 'profile', component: Profile, canActivate: [authGuard] },
      { path: 'users', component: UsersList, canActivate: [authGuard, adminGuard] },
      { path: 'management', component: ManagementHome, canActivate: [authGuard, managementGuard] },
      { path: 'events', component: EventsList, canActivate: [authGuard] },
    ]
  },
  {
    path: '**',
    redirectTo: 'login'
  }
];
