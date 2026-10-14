





install-rust-dependencies: 
    cargo install cargo-expand
    cargo install cargo-sort
    cargo install cargo-group-imports


[working-directory:'warbeats']
dev:
    flutter run        


# use concurrently to run the watch command too 
# flutter_rust_bridge_codegen generate --watch
#Run SeaORM migrations against dev DB
@run-migrations:
    sea-orm-cli migrate up --database-url {{ DEV_DB_URL }}

#Regenerate SeaORM entities from dev DB
@generate-entities:
    RUST_BACKTRACE=full sea-orm-cli generate entity \
        --database-url {{ DEV_DB_URL }} \
        --with-serde both \
        --enum-extra-derives 'ts_rs::TS' \
        --enum-extra-attributes 'serde(rename_all = "snake_case")' \
        --model-extra-attributes 'serde(rename_all="camelCase")' \
        --model-extra-attributes 'asterisk_macros::ts_rs_export_sea_orm_entity_name' \
        --ignore-tables asterisk_server_migrations \
        -o src/models

#Regenerate frontend TypeScript bindings in typegen/
@export-bindings:
    node scripts/ts-export.js

#Pull DB schema, regenerate entities, and export bindings
db-pull:
    just run-migrations
    just generate-entities
    cargo test
    just export-bindings
