import { Component, input } from '@angular/core';
import { AppIcon } from '../../models/nav-menu-item';

@Component({
  selector: 'app-icon',
  templateUrl: './icon.html',
  styleUrl: './icon.css',
})
export class Icon {
  readonly name = input.required<AppIcon>();
}
