const fs = require('fs');
const path = require('path');
const dir = path.join(__dirname, 'pages');

function load(id) {
  for (const name of [`${id}-fresh.json`, `${id}.json`]) {
    const p = path.join(dir, name);
    if (!fs.existsSync(p)) continue;
    let t = fs.readFileSync(p, 'utf8');
    if (t.charCodeAt(0) === 0xfeff) t = t.slice(1);
    return JSON.parse(t);
  }
  return null;
}

function textOf(j) {
  const body = j.body?.storage?.value || j.body?.view?.value || '';
  return String(body)
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;/g, ' ')
    .replace(/&rsquo;/g, "'")
    .replace(/&ldquo;|&rdquo;|&quot;/g, '"')
    .replace(/&amp;/g, '&')
    .replace(/\s+/g, ' ');
}

const ids = [
  '5144712',
  '585695688',
  '64389206',
  '259162123',
  '282591305',
  '209977345',
  '45842450',
  '262897764',
  '8650789',
  '537690116',
  '287178781',
  '863830982',
  '880249042',
];

const needle =
  /proforma|due date|payment deadline|VALIDTO|after payment|paid proforma|delete|month|30 day|liability due/i;

for (const id of ids) {
  const j = load(id);
  if (!j) {
    console.log('missing', id);
    continue;
  }
  const text = textOf(j);
  console.log('\n========', id, '|', j.title, '| len', text.length);
  const re = /(.{0,100}(?:proforma|due date|payment deadline|VALIDTO|paid|delete|month|30\s*day|liability).{0,160})/gi;
  let m;
  let c = 0;
  const seen = new Set();
  while ((m = re.exec(text)) && c < 40) {
    const s = m[1].replace(/\s+/g, ' ').trim();
    if (!needle.test(s)) continue;
    if (seen.has(s)) continue;
    seen.add(s);
    console.log('-', s);
    c++;
  }
}
