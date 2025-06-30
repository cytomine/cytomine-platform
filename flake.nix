{
  inputs = {
    nixpkgs.url = "nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let pkgs = nixpkgs.legacyPackages.${system};
      in {
        devShell = pkgs.mkShell {
          buildInputs = with pkgs;[
            openssl
            fluxcd
            kubectl
            postgresql
            kustomize
            kubernetes-helm
          ];
          shellHook = ''
            export KUBECONFIG=./.kube/config
          '';
        };
      });
}
