import { Component, inject } from '@angular/core';
import { RouterOutlet , RouterLink, Router} from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
@Component({
  selector: 'app-app-layout',
  imports: [RouterOutlet, RouterLink],
  templateUrl: './app-layout.html',
  styleUrl: './app-layout.css',
})
export class AppLayout {
   readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
