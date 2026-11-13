# The CLI, with desktop files that offer it for uploads from Spectacle and Dolphin.
{
  lib,
  runCommand,
  makeWrapper,
  writeText,
  jdk25,
  wl-clipboard,
  libnotify,
  xdg-utils,
  jars,
}:

let
  # Appears in Spectacle's Export -> "Open With" (and any image "Open With"),
  # so screenshots annotated in Spectacle can be uploaded straight from there.
  spectacleDesktop = writeText "at.yawk.paste.desktop" ''
    [Desktop Entry]
    Type=Application
    Name=s.yawk.at
    Comment=Upload to the paste server
    Icon=document-share
    Exec=paste-cli file %f
    MimeType=image/png;image/jpeg;
    Terminal=false
    NoDisplay=true
  '';

  # Dolphin right-click "Share via s.yawk.at" for any file.
  serviceMenu = writeText "at.yawk.paste-servicemenu.desktop" ''
    [Desktop Entry]
    Type=Service
    X-KDE-ServiceTypes=KonqPopupMenu/Plugin
    X-KDE-Priority=TopLevel
    MimeType=all/allfiles;
    Actions=pasteUpload;

    [Desktop Action pasteUpload]
    Name=Share via s.yawk.at
    Icon=document-share
    Exec=paste-cli file %f
  '';
in
runCommand "paste-cli-${jars.version}"
  {
    nativeBuildInputs = [ makeWrapper ];
    meta = {
      description = "CLI that uploads stdin, files and screenshots to a paste server";
      homepage = "https://github.com/yawkat/paste";
      mainProgram = "paste-cli";
      platforms = lib.platforms.linux;
    };
  }
  ''
    # Installed as paste-cli, not paste, to avoid shadowing coreutils' paste.
    makeWrapper ${jdk25}/bin/java "$out/bin/paste-cli" \
      --add-flags "-Djava.awt.headless=true -jar ${jars}/share/paste/cli/cli-${jars.version}.jar" \
      --prefix PATH : ${
        lib.makeBinPath [
          wl-clipboard
          libnotify
          xdg-utils
        ]
      }

    install -Dm644 ${spectacleDesktop} "$out/share/applications/at.yawk.paste.desktop"
    install -Dm644 ${serviceMenu} "$out/share/kio/servicemenus/at.yawk.paste.desktop"
  ''
