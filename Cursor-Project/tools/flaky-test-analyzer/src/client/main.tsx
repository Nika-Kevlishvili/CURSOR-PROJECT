import { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { Dashboard } from './pages/Dashboard';
import { ImportPage } from './pages/ImportPage';
import { Explorer } from './pages/Explorer';
import { Compare } from './pages/Compare';
import { Fixes } from './pages/Fixes';
import './styles.css';

const links = [
  ['#/', 'Overview'],
  ['#/tests', 'Analyze'],
  ['#/fixes', 'Approve'],
  ['#/import', 'Import'],
  ['#/compare', 'Compare'],
];

function routeOf(hash: string): string {
  const value = hash.replace(/^#/, '') || '/';
  if (value.startsWith('/import')) return 'import';
  if (value.startsWith('/tests')) return 'tests';
  if (value.startsWith('/compare')) return 'compare';
  if (value.startsWith('/fixes')) return 'fixes';
  return 'dashboard';
}

function App() {
  const [route, setRoute] = useState(routeOf(window.location.hash));
  useEffect(() => {
    const onHash = () => setRoute(routeOf(window.location.hash));
    window.addEventListener('hashchange', onHash);
    return () => window.removeEventListener('hashchange', onHash);
  }, []);
  return (
    <div className="app">
      <nav>
        <strong>Test fixer</strong>
        <p>Analyze a failed Playwright run, review the suggestion, then approve the exact change.</p>
        {links.map(([href, label]) => (
          <a key={href} href={href} className={routeOf(href) === route ? 'active' : ''}>{label}</a>
        ))}
      </nav>
      <main>
        {route === 'dashboard' && <Dashboard />}
        {route === 'import' && <ImportPage />}
        {route === 'tests' && <Explorer />}
        {route === 'compare' && <Compare />}
        {route === 'fixes' && <Fixes />}
      </main>
    </div>
  );
}

createRoot(document.getElementById('root')!).render(<App />);
