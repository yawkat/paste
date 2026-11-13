{
  description = "paste: paste server (s.yawk.at) and its upload CLI";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-26.05";

  outputs =
    { self, nixpkgs }:
    let
      inherit (nixpkgs) lib;
      systems = [
        "x86_64-linux"
        "aarch64-linux"
      ];
      forAllSystems = f: lib.genAttrs systems (system: f nixpkgs.legacyPackages.${system});
    in
    {
      packages = forAllSystems (pkgs: rec {
        # Both jars come out of one Maven build, so there is one dependency hash for the whole project.
        jars = pkgs.callPackage ./nix/jars.nix { };
        server = pkgs.callPackage ./nix/server.nix { inherit jars; };
        cli = pkgs.callPackage ./nix/cli.nix { inherit jars; };
        default = server;
      });

      checks = forAllSystems (pkgs: {
        inherit (self.packages.${pkgs.stdenv.hostPlatform.system}) server cli;
        nixos = pkgs.callPackage ./nix/nixos-test.nix { inherit self; };
      });

      devShells = forAllSystems (pkgs: {
        default = pkgs.mkShell {
          packages = [
            pkgs.jdk21
            pkgs.maven
          ];
        };
      });

      formatter = forAllSystems (pkgs: pkgs.nixfmt-tree);
    };
}
