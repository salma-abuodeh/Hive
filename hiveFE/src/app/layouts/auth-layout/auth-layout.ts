import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, Subscription } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';

interface PanelCopy {
  eyebrow: string;
  title: string;
  body: string;
  stats: { value: string; label: string }[];
}

@Component({
  selector: 'app-auth-layout',
  imports: [RouterOutlet, RouterLink],
  templateUrl: './auth-layout.html',
  styleUrl: './auth-layout.css',
})
export class AuthLayout implements AfterViewInit, OnDestroy {
  @ViewChild('honeycomb') honeycombRef?: ElementRef<HTMLDivElement>;

  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);
  private sub?: Subscription;
  private resizeObserver?: ResizeObserver;

  readonly panel = signal<PanelCopy>(this.copyFor(false));

  ngAfterViewInit(): void {
    this.syncRoute(this.router.url);
    this.sub = this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e) => this.syncRoute(e.urlAfterRedirects));

    if (isPlatformBrowser(this.platformId)) {
      queueMicrotask(() => this.renderHoneycomb());
      this.resizeObserver = new ResizeObserver(() => this.renderHoneycomb());
      if (this.honeycombRef?.nativeElement) {
        this.resizeObserver.observe(this.honeycombRef.nativeElement);
      }
    }
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.resizeObserver?.disconnect();
  }

  private syncRoute(url: string): void {
    this.panel.set(this.copyFor(url.includes('/signup')));
  }

  private copyFor(signup: boolean): PanelCopy {
    if (signup) {
      return {
        eyebrow: 'Get started',
        title: 'Bring your team into one hive.',
        body: 'Create your account, then join or start a company workspace in the next step.',
        stats: [
          // { value: '2 min', label: 'To set up your account' },
          // { value: 'Free', label: 'For individual members' },
        ],
      };
    }
    return {
      eyebrow: 'Welcome back',
      title: 'Pick up right where your team left off.',
      body: 'Your feed, chats, and tasks are exactly where you left them.',
      stats: [
        // { value: '12k+', label: 'Companies on Hive' },
        // { value: '4.8/5', label: 'Average team rating' },
      ],
    };
  }

  private renderHoneycomb(): void {
    const el = this.honeycombRef?.nativeElement;
    if (!el) return;

    const cell = 74;
    const w = el.clientWidth || 640;
    const h = el.clientHeight || 800;
    const hexH = cell * 0.87;
    const cols = Math.ceil(w / (cell * 0.75)) + 2;
    const rows = Math.ceil(h / hexH) + 2;
    const r = cell / 2;

    const points = (cx: number, cy: number, rad: number) => {
      const pts: string[] = [];
      for (let i = 0; i < 6; i++) {
        const a = (Math.PI / 180) * (60 * i - 30);
        pts.push(`${(cx + rad * Math.cos(a)).toFixed(1)},${(cy + rad * Math.sin(a)).toFixed(1)}`);
      }
      return pts.join(' ');
    };

    let svg = `<svg viewBox="0 0 ${w} ${h}" preserveAspectRatio="xMidYMid slice" aria-hidden="true">`;
    for (let row = -1; row < rows; row++) {
      for (let col = -1; col < cols; col++) {
        const x = col * cell * 0.75;
        const y = row * hexH + (col % 2 ? hexH / 2 : 0);
        const lit = ((row * 17 + col * 31) % 17) === 0;
        const stroke = lit ? 'rgba(245,166,35,0.55)' : 'rgba(253,252,246,0.08)';
        const fill = lit ? 'rgba(245,166,35,0.10)' : 'none';
        const dur = 8 + ((row + col) % 6);
        const delay = ((row * 3 + col) % 6);
        svg += `<polygon points="${points(x, y, r - 3)}" fill="${fill}" stroke="${stroke}" stroke-width="1.2" style="animation: drift ${dur}s ease-in-out ${delay}s infinite alternate;"/>`;
      }
    }
    svg += '</svg>';
    el.innerHTML = svg;
  }
}
