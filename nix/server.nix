# The server reads config.yml and key.pub from its working directory.
{
  lib,
  runCommand,
  makeWrapper,
  jdk25_headless,
  jars,
}:

runCommand "paste-server-${jars.version}"
  {
    nativeBuildInputs = [ makeWrapper ];
    meta = {
      description = "Paste server";
      homepage = "https://github.com/yawkat/paste";
      mainProgram = "paste-server";
      platforms = lib.platforms.linux;
    };
  }
  ''
    makeWrapper ${jdk25_headless}/bin/java "$out/bin/paste-server" \
      --add-flags "-jar ${jars}/share/paste/server/server-${jars.version}.jar"
  ''
