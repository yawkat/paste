# Boots a VM with PostgreSQL and the server, uploads a text paste with the CLI and reads it back.
{
  self,
  stdenv,
  testers,
  formats,
}:

let
  inherit (self.packages.${stdenv.hostPlatform.system}) server cli;

  configYml = (formats.yaml { }).generate "config.yml" {
    sqlConfig = {
      jdbcUrl = "jdbc:postgresql://127.0.0.1/paste";
      username = "paste";
      password = "";
    };
    host = "127.0.0.1";
    port = 8080;
  };
in
testers.runNixOSTest {
  name = "paste";

  nodes.machine =
    { pkgs, ... }:
    {
      services.postgresql = {
        enable = true;
        ensureDatabases = [ "paste" ];
        ensureUsers = [
          {
            name = "paste";
            ensureDBOwnership = true;
          }
        ];
        authentication = "host paste paste 127.0.0.1/32 trust";
      };

      systemd.services.paste = {
        after = [ "postgresql.service" ];
        requires = [ "postgresql.service" ];
        wantedBy = [ "multi-user.target" ];
        serviceConfig = {
          ExecStart = "${server}/bin/paste-server";
          # config.yml, and key.pub, which the server writes on the first upload
          StateDirectory = "paste";
          WorkingDirectory = "/var/lib/paste";
          DynamicUser = true;
        };
        preStart = "ln -sf ${configYml} config.yml";
      };

      environment.systemPackages = [
        cli
        pkgs.openssl
        pkgs.jq
      ];
      environment.etc."paste-client.json".text = builtins.toJSON { remote = "http://127.0.0.1:8080"; };
    };

  testScript = ''
    machine.wait_for_unit("paste.service")
    machine.wait_for_open_port(8080)

    # The client's key: base64 DER, PKCS#8 and X.509 like Java's getEncoded()
    machine.succeed(
      "mkdir -p /root/.config/paste",
      "cp /etc/paste-client.json /root/.config/paste/config.json",
      "openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out /tmp/key.pem",
      "openssl pkcs8 -topk8 -nocrypt -in /tmp/key.pem -outform DER -out /tmp/key.der",
      "openssl pkey -inform DER -in /tmp/key.der -pubout -outform DER -out /tmp/pub.der",
      "jq -n --arg private $(base64 -w0 /tmp/key.der) --arg public $(base64 -w0 /tmp/pub.der) "
      + "'{private: $private, public: $public}' > /root/.config/paste/key",
    )

    url = machine.succeed("echo 'hello from the nixos test' | paste-cli").strip()
    print(url)
    assert url.startswith("http://127.0.0.1:8080/"), url
    machine.succeed(f"curl -sf {url} | grep -F 'hello from the nixos test'")

    # The server trusts the first key it sees, and keeps it
    machine.succeed("test -s /var/lib/paste/key.pub")
  '';
}
