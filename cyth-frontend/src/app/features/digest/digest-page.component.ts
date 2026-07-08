import { Component, inject, signal, OnInit } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { DigestService } from '../../core/services/digest.service';
import { DigestResponse, ItemDto, ItemAction } from '../../core/models/digest.model';

@Component({
  selector: 'app-digest-page',
  imports: [DecimalPipe],
  template: `
    <div class="page">
      <header class="head">
        <div>
          <h1>Digest du jour</h1>
          @if (digest(); as d) {
            <p class="sub">
              {{ d.items.length + d.alerts.length }} items retenus
            </p>
          }
        </div>
        <button class="btn" (click)="load()">Régénérer</button>
      </header>

      @if (loading()) {
        <p class="state">Chargement…</p>
      } @else if (error()) {
        <p class="state err">{{ error() }}</p>
      } @else if (digest(); as d) {

        <div class="stats">
          <div class="stat">
            <span class="stat-label">Nouveautés</span>
            <span class="stat-value">{{ d.stats.newToday }}</span>
          </div>
          <div class="stat danger">
            <span class="stat-label">Alertes critiques</span>
            <span class="stat-value">{{ d.stats.criticalAlerts }}</span>
          </div>
          <div class="stat">
            <span class="stat-label">Sources</span>
            <span class="stat-value">{{ d.stats.sources }}</span>
          </div>
          <div class="stat">
            <span class="stat-label">À lire</span>
            <span class="stat-value">{{ d.stats.toRead }}</span>
          </div>
        </div>

        @if (d.alerts.length) {
          <h2 class="section danger-text">Exploité activement — priorité absolue</h2>
          @for (item of d.alerts; track item.id) {
            <article class="card alert">
              <div class="badges">
                <span class="badge badge-danger">{{ item.source }}</span>
                @if (item.cvssScore != null) {
                  <span class="badge badge-cvss">CVSS {{ item.cvssScore }}</span>
                }
              </div>
              <a class="title" [href]="item.url" target="_blank" rel="noopener">{{ item.title }}</a>
              @if (item.summary) {
                <p class="summary">{{ item.summary }}</p>
              }
              <div class="card-foot">
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
            </article>
          }
        }

        <h2 class="section muted">Reste du digest</h2>
        @for (item of d.items; track item.id) {
          <article class="card">
            <div class="badges">
              <span class="badge">{{ item.source }}</span>
              <span class="score">score {{ item.score | number: '1.0-1' }}</span>
            </div>
            <a class="title" [href]="item.url" target="_blank" rel="noopener">{{ item.title }}</a>
            @if (item.summary) {
              <p class="summary">{{ item.summary }}</p>
            }
            <div class="card-foot">
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
          </article>
        } @empty {
          <p class="state">Rien pour l'instant — les collecteurs n'ont peut-être pas encore tourné.</p>
        }
      }
    </div>
  `,
  styles: `
    .page { max-width: 760px; margin: 0 auto; padding: 24px 16px; font-family: system-ui, sans-serif; color: #1a1a1a; }
    .head { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 20px; }
    h1 { font-size: 22px; font-weight: 600; margin: 0; }
    .sub { color: #888; font-size: 13px; margin: 2px 0 0; }
    .btn { border: 1px solid #ccc; background: #fff; border-radius: 8px; padding: 8px 14px; cursor: pointer; font-size: 13px; }
    .btn:hover { background: #f5f5f5; }
    .state { color: #888; padding: 24px 0; }
    .state.err { color: #a32d2d; }
    .stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 12px; margin-bottom: 24px; }
    .stat { background: #f4f2ec; border-radius: 8px; padding: 12px 16px; display: flex; flex-direction: column; }
    .stat.danger { background: #fcebeb; }
    .stat-label { font-size: 13px; color: #888; }
    .stat.danger .stat-label { color: #a32d2d; }
    .stat-value { font-size: 24px; font-weight: 600; }
    .stat.danger .stat-value { color: #a32d2d; }
    .section { font-size: 14px; font-weight: 600; margin: 20px 0 10px; }
    .section.muted { color: #888; }
    .danger-text { color: #a32d2d; }
    .card { background: #fff; border: 1px solid #e5e5e5; border-radius: 12px; padding: 14px 18px; margin-bottom: 10px; }
    .card.alert { border-color: #e79a9a; }
    .badges { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; flex-wrap: wrap; }
    .badge { font-size: 12px; padding: 2px 9px; border-radius: 20px; background: #f1efe8; color: #5f5e5a; }
    .badge-danger { background: #fcebeb; color: #a32d2d; }
    .badge-cvss { background: #fcebeb; color: #791f1f; }
    .score { font-size: 12px; color: #999; }
    .title { display: block; font-size: 15px; font-weight: 600; color: #1a1a1a; text-decoration: none; margin-bottom: 4px; }
    .title:hover { text-decoration: underline; }
    .summary { font-size: 14px; color: #555; line-height: 1.5; margin: 0; }
    .card-foot { display: flex; align-items: center; justify-content: space-between; margin-top: 10px; }
    .tags { display: flex; gap: 6px; flex-wrap: wrap; }
    .tag { font-family: monospace; font-size: 11px; background: #f4f2ec; color: #666; padding: 2px 8px; border-radius: 6px; }
    .actions { display: flex; gap: 4px; }
    .icon { width: 30px; height: 30px; border: 1px solid #ddd; background: #fff; border-radius: 6px; cursor: pointer; font-size: 14px; }
    .icon:hover { background: #f5f5f5; }
  `
})
export class DigestPageComponent implements OnInit {
  private readonly service = inject(DigestService);

  readonly digest = signal<DigestResponse | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

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

  act(item: ItemDto, action: ItemAction): void {
    this.service.applyAction(item.id, action).subscribe({
      next: () => this.load(),
      error: () => this.error.set("L'action n'a pas pu être enregistrée."),
    });
  }
}
