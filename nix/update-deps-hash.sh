#!/usr/bin/env bash
# Puts the hash of the Maven dependency download into nix/deps.sha256, after a pom.xml or nixpkgs' Maven changed.
set -euo pipefail
cd "$(dirname "$0")/.."

if log=$(nix build --no-link -L .#jars.fetchedMavenDeps 2>&1); then
  echo "nix/deps.sha256 is up to date"
  exit 0
fi
hash=$(sed -n 's/^ *got: *\(sha256-[A-Za-z0-9+/=]*\)$/\1/p' <<<"$log")
if [[ -z $hash ]]; then
  echo "$log" >&2
  exit 1
fi
echo "$hash" > nix/deps.sha256
echo "nix/deps.sha256: $hash"
