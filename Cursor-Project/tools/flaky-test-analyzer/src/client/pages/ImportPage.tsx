import { useState, type FormEvent } from 'react';

type ImportResponse = {
  duplicate: boolean;
  report: {
    id: string;
    environment: string;
    label: string | null;
    executedAt: string | null;
    parser: string;
    playwrightVersion: string | null;
    testCount: number;
    failedCount: number;
    warnings: string[];
    sourceFilename: string;
    reportPath: string | null;
  };
};

export function ImportPage() {
  const [environment, setEnvironment] = useState('dev');
  const [label, setLabel] = useState('');
  const [executedAt, setExecutedAt] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [progress, setProgress] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<ImportResponse | null>(null);

  function submit(event: FormEvent) {
    event.preventDefault();
    if (!file) {
      setError('Choose a ZIP file.');
      return;
    }
    const body = new FormData();
    body.set('report', file);
    body.set('environment', environment);
    if (label.trim()) body.set('label', label.trim());
    if (executedAt) body.set('executedAt', new Date(executedAt).toISOString());
    setError(null);
    setResult(null);
    const request = new XMLHttpRequest();
    request.open('POST', '/api/reports/import');
    request.setRequestHeader('X-Flaky-Analyzer', '1');
    request.upload.onprogress = (progressEvent) => {
      if (progressEvent.lengthComputable) {
        setProgress(`Uploading ${Math.round((progressEvent.loaded / progressEvent.total) * 100)}%`);
      }
    };
    request.onload = () => {
      setProgress(null);
      const payload = JSON.parse(request.responseText) as ImportResponse & { error?: string };
      if (request.status >= 400) {
        setError(payload.error ?? 'Import failed.');
        return;
      }
      setResult(payload);
    };
    request.onerror = () => {
      setProgress(null);
      setError('The upload did not reach the local analyzer.');
    };
    setProgress('Uploading…');
    request.send(body);
  }

  return (
    <section>
      <h1>Import a Playwright report</h1>
      <p className="muted">Upload a ZIP of a Playwright HTML report from Dev, Dev2, or Test. The same archive is not stored twice.</p>
      <form className="form" onSubmit={submit}>
        <label>ZIP file <input type="file" accept=".zip,application/zip" onChange={(event) => setFile(event.target.files?.[0] ?? null)} /></label>
        <label>Environment
          <select value={environment} onChange={(event) => setEnvironment(event.target.value)}>
            <option value="dev">Dev</option>
            <option value="dev2">Dev2</option>
            <option value="test">Test</option>
          </select>
        </label>
        <label>Execution label <input value={label} onChange={(event) => setLabel(event.target.value)} placeholder="optional build id" /></label>
        <label>Executed at <input type="datetime-local" value={executedAt} onChange={(event) => setExecutedAt(event.target.value)} /></label>
        <button className="primary" type="submit">Import</button>
      </form>
      {progress && <p>{progress}</p>}
      {error && <p className="error">{error}</p>}
      {result && (
        <div className={result.duplicate ? 'banner' : 'ok'}>
          <p>{result.duplicate ? 'This archive was already imported. No duplicate execution was created.' : 'Import finished.'}</p>
          <p>{result.report.sourceFilename} · {result.report.environment} · parser {result.report.parser}</p>
          <p>{result.report.testCount} tests, {result.report.failedCount} failed. Playwright version: {result.report.playwrightVersion ?? 'unavailable'}.</p>
          <p>Executed at: {result.report.executedAt ?? 'unavailable'}. Report file: {result.report.reportPath ?? 'unavailable'}.</p>
          {result.report.label && <p>Label: {result.report.label}</p>}
          {result.report.warnings.map((warning) => <p key={warning}>{warning}</p>)}
        </div>
      )}
    </section>
  );
}
