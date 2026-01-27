

configure:
    @just install-flutter-dependencies

install-flutter-dependencies:
    brew tap leoafarias/fvm
    brew install fvm

install-rust-dependencies: 
    cargo install flutter_rust_bridge_codegen
    cargo install cargo-expand
    cargo install cargo-sort
    cargo install cargo-group-imports


[working-directory:'warbeats']
dev:
    flutter run        


# use concurrently to run the watch command too 
# flutter_rust_bridge_codegen generate --watch
