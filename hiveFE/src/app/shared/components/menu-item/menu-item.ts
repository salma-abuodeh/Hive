import { Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { NavIcon } from '../../models/nav-menu-item';

@Component({
  selector: 'app-menu-item',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './menu-item.html',
  styleUrl: './menu-item.css',
})
export class MenuItem {
  readonly name = input.required<string>();
  readonly url = input.required<string>();
  readonly icon = input<NavIcon>('feed');
}
