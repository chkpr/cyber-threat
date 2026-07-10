import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { DigestService } from '../../core/services/digest.service';
import { DigestResponse, ItemDto, ItemAction } from '../../core/models/digest.model';

interface Brick {
  name: string;
  critical: boolean;
  items: ItemDto[];
}

@Component({
  selector: 'app-digest-page',
  imports: [],
  template: `
    <div class="page">
      <header class="head">
        <div>
          <h1>Digest du jour</h1>
          @if (digest(); as d) {
            <p class="sub">{{ d.items.length + d.alerts.length }} items · {{ bricks().length }} sources</p>
          }
        </div>
        <button class="btn" (click)="load()">Régénérer</button>
      </header>

      @if (loading()) {
        <p class="state">Chargement…</p>
      } @else if (error()) {
        <p class="state err">{{ error() }}</p>
      } @else if (digest()) {
        <div class="bento">
          @for (brick of bricks(); track brick.name) {
            <section class="brick" [class.critical]="brick.critical">
              <div class="brick-head">
                <span class="brick-name">{{ brick.name }}</span>
                <span class="brick-count" [class.count-danger]="brick.critical">{{ brick.items.length }}</span>
              </div>

              @for (item of visibleItems(brick); track item.id) {
                <div class="entry">
                  <a class="entry-title" [href]="item.url" target="_blank" rel="noopener">{{ item.title }}</a>
                  @if (isExpanded(brick.name)) {
                    @if (item.summary) {
                      <p class="entry-summary">{{ item.summary }}</p>
                    }
                    <div class="entry-foot">
                      <div class="tags">
                        @for (tag of item.tags; track tag) {
                          <span class="tag">{{ tag }}</span>
                        }
                      </div>
                      <div class="actions">
                        <button class="icon" title="Lire plus tard" (click)="act(item, 'READ_LATER')">★</button>
                        <button class="icon" title="Archiver" (click)="act(item, 'ARCHIVE')">✓</button>
                        <button class="icon" title="Ignorer" (click)="act(item, 'IGNORE')">✕</button>
                      </div>
                    </div>
                  }
                </div>
              }

              @if (brick.items.length > 3) {
                <button class="more" (click)="toggle(brick.name)">
                  {{ isExpanded(brick.name) ? 'Réduire' : 'Voir tout (' + brick.items.length + ')' }}
                </button>
              } @else if (brick.items.length === 0) {
                <p class="empty">—</p>
              }
            </section>
          }
        </div>
      }
    </div>
  `,
  styles: `
    .page { max-width: 1200px; margin: 0 auto; padding: 24px 16px; font-family: system-ui, sans-serif; color: #1a1a1a; }
    .head { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 20px; }
    h1 { font-size: 22px; font-weight: 600; margin: 0; }
    .sub { color: #888; font-size: 13px; margin: 2px 0 0; }
    .btn { border: 1px solid #ccc; background: #fff; border-radius: 8px; padding: 8px 14px; cursor: pointer; font-size: 13px; }
    .btn:hover { background: #f5f5f5; }
    .state { color: #888; padding: 24px 0; }
    .state.err { color: #a32d2d; }

    .bento { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; align-items: start;}
    .brick { background: #fff; border: 1px solid #e5e5e5; border-radius: 12px; padding: 14px; }
    .brick.critical { border-color: #e79a9a; background: #fffafa; }

    .brick-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
    .brick-name { font-size: 13px; font-weight: 600; }
    .brick.critical .brick-name { color: #a32d2d; }
    .brick-count { background: #f4f2ec; color: #666; font-size: 12px; padding: 1px 8px; border-radius: 20px; }
    .count-danger { background: #fcebeb; color: #a32d2d; }

    .entry { padding: 6px 0; border-top: 1px solid #f0efe9; }
    .entry:first-of-type { border-top: none; }
    .entry-title { display: block; font-size: 14px; color: #1a1a1a; text-decoration: none; line-height: 1.4; }
    .entry-title:hover { text-decoration: underline; }
    .entry-summary { font-size: 13px; color: #666; line-height: 1.5; margin: 4px 0 0; }
    .entry-foot { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; }
    .tags { display: flex; gap: 5px; flex-wrap: wrap; }
    .tag { font-family: monospace; font-size: 11px; background: #f4f2ec; color: #666; padding: 1px 7px; border-radius: 6px; }
    .actions { display: flex; gap: 4px; }
    .icon { width: 26px; height: 26px; border: 1px solid #ddd; background: #fff; border-radius: 6px; cursor: pointer; font-size: 12px; }
    .icon:hover { background: #f5f5f5; }

    .more { margin-top: 10px; width: 100%; border: 1px solid #e5e5e5; background: #fafafa; border-radius: 8px;
      padding: 6px; cursor: pointer; font-size: 12px; color: #555; }
    .more:hover { background: #f0f0f0; }
    .empty { color: #bbb; font-size: 13px; margin: 4px 0 0; }
  `
})
export class DigestPageComponent implements OnInit {
  private readonly service = inject(DigestService);

  readonly digest = signal<DigestResponse | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly expanded = signal<Set<string>>(new Set());

  readonly bricks = computed<Brick[]>(() => {
    const d = this.digest();
    if (!d) return [];

    const result: Brick[] = [];
    if (d.alerts.length) {
      result.push({ name: 'Critique', critical: true, items: d.alerts });
    }

    const bySource = new Map<string, ItemDto[]>();
    for (const item of d.items) {
      const arr = bySource.get(item.source) ?? [];
      arr.push(item);
      bySource.set(item.source, arr);
    }

    const sourceBricks = [...bySource.entries()]
      .map(([name, items]) => ({ name, critical: false, items }))
      .sort((a, b) => b.items.length - a.items.length);

    return [...result, ...sourceBricks];
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.getDigest().subscribe({
      next: (data) => {
        this.digest.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger le digest. Le backend est-il démarré sur le port 8080 ?');
        this.loading.set(false);
      },
    });
  }

  isExpanded(name: string): boolean {
    return this.expanded().has(name);
  }

  toggle(name: string): void {
    const next = new Set(this.expanded());
    if (next.has(name)) {
      next.delete(name);
    } else {
      next.add(name);
    }
    this.expanded.set(next);
  }

  visibleItems(brick: Brick): ItemDto[] {
    return this.isExpanded(brick.name) ? brick.items : brick.items.slice(0, 3);
  }

  act(item: ItemDto, action: ItemAction): void {
    this.service.applyAction(item.id, action).subscribe({
      next: () => this.load(),
      error: () => this.error.set("L'action n'a pas pu être enregistrée."),
    });
  }
}
