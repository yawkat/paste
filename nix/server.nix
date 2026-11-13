# The server reads config.yml and key.pub from its working directory.
{
  lib,
  runCommand,
  makeWrapper,
  jre,
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
    makeWrapper ${lib.getExe jre} "$out/bin/paste-server" \
      --add-flags "-jar ${jars}/share/paste/server.jar"
  ''
