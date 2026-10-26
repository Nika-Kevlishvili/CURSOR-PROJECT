import { useEffect, useState } from 'react';
import { api } from '../api';

type Cell = { reportId: string; environment: string; label: string | null; executedAt: string | null; status: string };
type Matrix = {
  reports: { id: string; environment: string; label: string | null; executedAt: string | null }[];
  tests: { testId: string; fullTitle: string; filePath: string | null; classification: string; behavior: string; cells: Cell[] }[];
};

function tone(status: string): string {
  if (status === 'missing') return 'missing';
  if (status === 'passed') return 'pass';
  if (status === 'failed' || status === 'timedOut' || status === 'interrupted') return 'fail';
  return '';
}

export function Compare() {
  const [testId, setTestId] = useState('');
  const [data, setData] = useState<Matrix | null>(null);
  const [error, setError] = useState<string | null>(null);

  function load(id = testId) {
    api<Matrix>(`/api/compare${id ? `?testId=${encodeURIComponent(id)}` : ''}`)
      .then((payload) => { setData(payload); setError(null); })
      .catch((reason: Error) => setError(reason.message));
  }

  useEffect(() => { load(''); }, []);

  return (
    <section>
      <h1>Compare environments</h1>
      <p className="banner">“No result” means that execution did not contain the test. It is not a pass.</p>
      <form className="filters" onSubmit={(event) => { event.preventDefault(); load(); }}>
        <label>Test id <input value={testId} onChange={(event) => setTestId(event.target.value)} placeholder="leave empty for recent non-stable tests" /></label>
        <button type="submit">Compare</button>
      </form>
      {error && <p className="error">{error}</p>}
      {data && (
        <table>
          <thead>
            <tr>
              <th>Test</th>
              <th>Behavior</th>
              {data.reports.map((report) => (
                <th key={report.id}>{report.environment}{report.label ? ` · ${report.label}` : ''}<br />{report.executedAt ?? 'time unavailable'}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.tests.map((test) => (
              <tr key={test.testId}>
                <td>{test.fullTitle}<br /><span className="muted">{test.filePath ?? 'file unavailable'} · {test.classification}</span></td>
                <td>{test.behavior}</td>
                {test.cells.map((cell) => (
                  <td key={cell.reportId} className={tone(cell.status)}>{cell.status === 'missing' ? 'No result' : cell.status}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {data && data.tests.length === 0 && <p className="muted">No comparable tests are stored yet.</p>}
    </section>
  );
}
