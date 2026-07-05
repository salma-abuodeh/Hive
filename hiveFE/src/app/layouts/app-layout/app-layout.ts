import { Component } from '@angular/core';
import { RouterOutlet , RouterLink} from '@angular/router';
@Component({
  selector: 'app-app-layout',
  imports: [RouterOutlet, RouterLink],
  templateUrl: './app-layout.html',
  styleUrl: './app-layout.css',
})
export class AppLayout {}
