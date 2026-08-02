import { Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AppIcon } from '../../models/nav-menu-item';
import { Icon } from '../icon/icon';

@Component({
  selector: 'app-menu-item',
  imports: [RouterLink, RouterLinkActive, Icon],
  templateUrl: './menu-item.html',
  styleUrl: './menu-item.css',
})
export class MenuItem {
  readonly name = input.required<string>();
  readonly url = input.required<string>();
  readonly icon = input<AppIcon>('feed');
}
