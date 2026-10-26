export function DiffView({ diff }: { diff: string }) {
  if (!diff.trim()) {
    return <p className="muted">No source edit. This suggestion explains the failure and does not change a test.</p>;
  }
  return (
    <div className="diff" role="region" aria-label="Suggested diff">
      {diff.split('\n').map((line, index) => {
        const kind = line.startsWith('+') && !line.startsWith('+++')
          ? 'add'
          : line.startsWith('-') && !line.startsWith('---')
            ? 'del'
            : line.startsWith('@@')
              ? 'hunk'
              : 'ctx';
        return <div className={`diff-line ${kind}`} key={`${index}-${line}`}>{line || ' '}</div>;
      })}
    </div>
  );
}
