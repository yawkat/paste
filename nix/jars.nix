# Builds every module with Maven: the server's and the CLI's jars, each with its dependencies in lib/ next to it.
#
# The downloaded dependencies are pinned by the hash in ./deps.sha256. After changing a dependency or plugin in a
# pom.xml, update it by running this from the repository root:
#
#   nix/update-deps-hash.sh
{
  lib,
  maven,
  jdk25,
}:

let
  src = lib.fileset.toSource {
    root = ../.;
    fileset = lib.fileset.unions [
      ../pom.xml
      ../shared
      ../server
      ../client
      ../cli
    ];
  };

  # Everything that decides which files Maven downloads. It goes into the name of the download, so changing any of it
  # downloads again, and fails on the hash if the dependencies changed, instead of a store that already has the old
  # download silently building against it.
  depsDigest = builtins.substring 0 12 (
    builtins.hashString "sha256" (
      lib.concatStringsSep "\n" (
        map (module: builtins.hashFile "sha256" (src + "/${module}pom.xml")) [
          ""
          "shared/"
          "server/"
          "client/"
          "cli/"
        ]
        ++ [ maven.version ]
      )
    )
  );
in
maven.buildMavenPackage {
  pname = "paste";
  version = "1.0-SNAPSHOT";
  inherit src;

  mvnJdk = jdk25;
  mvnHash = lib.fileContents ./deps.sha256;
  mvnFetchExtraArgs.pname = "paste-maven-deps-${depsDigest}";

  installPhase = ''
    runHook preInstall

    # The jars' manifests refer to their dependencies as lib/<name>.jar, so keep the file names.
    for app in server cli; do
      install -Dm644 -t "$out/share/paste/$app" "$app/target/$app-1.0-SNAPSHOT.jar"
      install -Dm644 -t "$out/share/paste/$app/lib" "$app"/target/lib/*.jar
    done

    runHook postInstall
  '';
}
