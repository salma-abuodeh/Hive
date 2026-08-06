import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  {
    path: 'chats',
    renderMode: RenderMode.Client,
  },
  {
    path: 'feed',
    renderMode: RenderMode.Client,
  },
  {
    path: 'feed/:id',
    renderMode: RenderMode.Client,
  },
  {
    path: 'saved',
    renderMode: RenderMode.Client,
  },
  {
    path: 'users',
    renderMode: RenderMode.Client,
  },
  {
    path: 'profile',
    renderMode: RenderMode.Client,
  },
  {
    path: 'company',
    renderMode: RenderMode.Client,
  },
  {
    path: '**',
    renderMode: RenderMode.Prerender,
  },
];
