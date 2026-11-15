#!/usr/bin/env python3
"""Read-only post-run comparison against the shared complete15.2 source archive."""
import hashlib,json,pathlib,sys,tarfile
repo=pathlib.Path(sys.argv[1]);out=pathlib.Path(sys.argv[2]);archive=repo/'documentation/raid-redesign-step-15-2-evidence/sources.tar.gz'
rows=[]
with tarfile.open(archive) as tar:
    for member in tar:
        assert member.isfile() and not pathlib.PurePosixPath(member.name).is_absolute() and '..' not in pathlib.PurePosixPath(member.name).parts
        expected=tar.extractfile(member).read();actual=(repo/member.name).read_bytes()
        assert expected==actual,member.name
        rows.append(dict(path=member.name,sha256=hashlib.sha256(actual).hexdigest()))
assert len(rows)==203
receipt=dict(status='PASS',files=203,archiveSha256=hashlib.sha256(archive.read_bytes()).hexdigest(),
             timing='Full203source archive comparison after final run; before/after pivot checks separately cover5source files and8compiled classes',entries=rows)
out.write_text(json.dumps(receipt,indent=2)+'\n');print('PASS all203 archived source files byte-exact')
