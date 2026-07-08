export type Criticality = 'NORMAL' | 'ALERT';

export type ItemAction = 'READ_LATER' | 'ARCHIVE' | 'IGNORE';

export interface ItemDto {
  id: number;
  source: string;
  title: string;
  url: string;
  summary: string | null;
  tags: string[];
  score: number;
  criticality: Criticality;
  cvssScore: number | null;
  inKev: boolean;
  publishedAt: string | null;
}

export interface DigestStats {
  newToday: number;
  criticalAlerts: number;
  sources: number;
  toRead: number;
}

export interface DigestResponse {
  stats: DigestStats;
  alerts: ItemDto[];
  items: ItemDto[];
}
