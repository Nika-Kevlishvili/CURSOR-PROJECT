"""Merge Word runs so [[DD.TablePCScales.ListPC.PC]] is one contiguous <w:t> value."""
from __future__ import annotations

import re
import shutil
import zipfile
from pathlib import Path

TARGET = "[[DD.TablePCScales.ListPC.PC]]"
WT = re.compile(r"(<w:t(?:\s[^>]*)?>)([^<]*)(</w:t>)")
ROOT = Path(__file__).resolve().parent


def fix_docx(src: Path, dest: Path) -> None:
    work = ROOT / f"_work_{dest.stem}"
    if work.exists():
        shutil.rmtree(work)
    work.mkdir(parents=True)

    with zipfile.ZipFile(src, "r") as z:
        z.extractall(work)

    doc_path = work / "word" / "document.xml"
    xml = doc_path.read_text(encoding="utf-8")
    matches = list(WT.finditer(xml))
    texts = [m.group(2) for m in matches]

    start_i = None
    for i in range(len(texts) - 2):
        a, b, c = texts[i], texts[i + 1], texts[i + 2]
        if (
            a.strip() == "[["
            and b.strip() == "DD.TablePCScales.ListPC.PC"
            and c.lstrip().startswith("]]")
        ):
            start_i = i
            break

    if start_i is None:
        for i, t in enumerate(texts):
            if TARGET in t:
                print(f"{src.name}: already contiguous in run[{i}] — copy → {dest.name}")
                shutil.copy2(src, dest)
                shutil.rmtree(work, ignore_errors=True)
                return
        raise SystemExit(f"Could not locate tag runs in {src.name}")

    print(f"{src.name}: merging split runs {start_i}..{start_i+2}")
    for j in range(start_i, start_i + 3):
        print(f"  {j}: {texts[j]!r}")

    t2 = texts[start_i + 2]
    idx = t2.find("]]")
    rest = t2[idx + 2 :]  # keep trailing '[[' if present

    pieces: list[str] = []
    last = 0
    for i, m in enumerate(matches):
        pieces.append(xml[last : m.start()])
        if i == start_i:
            pieces.append(m.group(1) + TARGET + m.group(3))
        elif i == start_i + 1:
            pieces.append(m.group(1) + "" + m.group(3))
        elif i == start_i + 2:
            pieces.append(m.group(1) + rest + m.group(3))
        else:
            pieces.append(m.group(0))
        last = m.end()
    pieces.append(xml[last:])
    doc_path.write_text("".join(pieces), encoding="utf-8")

    if dest.exists():
        dest.unlink()
    with zipfile.ZipFile(dest, "w", zipfile.ZIP_DEFLATED) as z:
        for p in work.rglob("*"):
            if p.is_file():
                z.write(p, p.relative_to(work).as_posix())

    with zipfile.ZipFile(dest) as z:
        out_xml = z.read("word/document.xml").decode("utf-8")
    opens = len(re.findall(r"<w:t[\s>]", out_xml))
    closes = len(re.findall(r"</w:t>", out_xml))
    out_texts = re.findall(r"<w:t(?:\s[^>]*)?>([^<]*)</w:t>", out_xml)
    joined = "".join(out_texts)
    assert opens == closes, f"w:t imbalance {opens}/{closes}"
    assert TARGET in joined, "target missing"
    for i in range(len(out_texts) - 2):
        if (
            out_texts[i].strip() == "[["
            and out_texts[i + 1].strip() == "DD.TablePCScales.ListPC.PC"
            and out_texts[i + 2].lstrip().startswith("]]")
        ):
            raise AssertionError("split form still present")
    print(f"  Wrote {dest.name} ({dest.stat().st_size} bytes); OK contiguous tag")
    shutil.rmtree(work, ignore_errors=True)


def main() -> None:
    for bad in [
        ROOT / "PDT-3223-invoice-ListPC-PC.docx",
        ROOT / "PDT-3223-invoice-ListPC-PC-from-bug.docx",
    ]:
        if bad.exists():
            bad.unlink()

    fix_docx(ROOT / "volume-sum-template.docx", ROOT / "PDT-3223-invoice-ListPC-PC.docx")
    fix_docx(ROOT / "bug-template.docx", ROOT / "PDT-3223-invoice-ListPC-PC-from-bug.docx")


if __name__ == "__main__":
    main()
