import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DigestResponse, ItemAction } from '../models/digest.model';

@Injectable({ providedIn: 'root' })
export class DigestService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api';

  getDigest(): Observable<DigestResponse> {
    return this.http.get<DigestResponse>(`${this.baseUrl}/digest`);
  }

  applyAction(id: number, action: ItemAction): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/items/${id}/action`, { action });
  }
}
