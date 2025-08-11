{
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
  inputs.flake-utils.url = "github:numtide/flake-utils";

  outputs =
    { flake-utils, nixpkgs, ... }:
    flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
        lib = pkgs.lib;
      in
      {
        devShells.default = pkgs.mkShellNoCC {
          packages = [
            pkgs.jdk_headless
            pkgs.jdt-language-server
            pkgs.lemminx
            pkgs.maven3
            pkgs.nixd
            pkgs.nixfmt
          ];
        };

        packages.default = pkgs.maven.buildMavenPackage rec {
          pname = "pomodoro";
          version = "0.1.0";

          src = lib.fileset.toSource {
            root = ./.;
            fileset = lib.fileset.unions [
              ./assembly.xml
              ./pom.xml
              ./src
            ];
          };

          mvnHash = "sha256-GJIriUGUoLytnSSb7AZfgXwj0+9d46TzCeQvywz2SD0=";
          mvnJdk = pkgs.jdk_headless;

          buildOffline = true;

          nativeBuildInputs = [ pkgs.makeWrapper ];

          installPhase = ''
            mkdir -p $out/bin $out/share/${pname}

            install -Dm644 target/${pname}-${version}-standalone.jar $out/share/${pname}

            makeWrapper ${lib.getExe pkgs.jre} $out/bin/${pname} \
              --add-flags "-jar $out/share/${pname}/${pname}-${version}-standalone.jar"
          '';
        };
      }
    );
}
