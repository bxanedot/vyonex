# Agent notes

## XERJ reference coding

The local XERJ node listens on `http://127.0.0.1:19200` and stores its index outside this checkout at `%LOCALAPPDATA%\XERJ\vyonex-node`. If it is stopped, restart it with `xerj --data-dir "$env:LOCALAPPDATA\XERJ\vyonex-node" --port 19200 --insecure`.

VyonEx is indexed under `vyonex-current`. Search before changing unfamiliar extraction behavior with `xerj search --url http://127.0.0.1:19200 --prefix vyonex-current "<question>"`. Use `xerj code <corpus> "<question>"` for the external reference corpus, and cite any approach relied on with repository, file, and line.

Treat GPL/AGPL references as approach-only. Do not copy code; check each repository's recorded license before reusing any material.
