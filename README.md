# COMMAND FRAMEWORK

API de comandos para **Paper 1.21.11**, desenvolvida com **Java 21**. O objetivo é simplificar o registro e a organização de comandos, com suporte a permissões, plataformas e subcomandos.

## Registro

Na classe principal do plugin, registre o pacote que contém suas classes de comandos:

```java
@Override
public void onEnable() {
    new CommandRegister(this).create("seu.pacote.de.comandos");
}
```

A API procura classes nesse pacote e registra os métodos anotados com `@Command`.

## Criando um comando

Utilize a anotação `@Command` no método que será executado:

```java
@Command(
    name = "teste",
    platform = Platform.PLAYER,
    description = "Exemplo de comando",
    permission = "plugin.teste"
)
public void handleTeste(CommandContext context) {
    context.info("§cTeste");
}
```

### Parâmetros

| Parâmetro | Descrição |
|---|---|
| `name` | Nome do comando. |
| `platform` | Define quem pode executar o comando. |
| `description` | Descrição do comando. |
| `permission` | Permissão necessária para executá-lo. |
| `usage` | Texto de uso do comando. |
| `aliases` | Nomes alternativos para o comando raiz. |

## Plataformas

O atributo `platform` define quem pode executar o comando.

| Plataforma | Descrição |
|---|---|
| `PLAYER` | Somente jogadores. |
| `CONSOLE` | Somente o console. |
| `BOTH` | Jogadores e console. |

Se não informar uma plataforma, o padrão é `Platform.BOTH`.

## Subcomandos

Os subcomandos são definidos usando espaços no atributo `name`. Por exemplo, `name = "admin ban"` registra `ban` como subcomando de `admin`.

```java
@Command(name = "admin", description = "Comandos administrativos")
public void handleAdmin(CommandContext context) {
    context.info("Utilize /admin ban <jogador>");
}

@Command(
    name = "admin ban",
    platform = Platform.BOTH,
    description = "Bane um jogador",
    permission = "plugin.admin.ban"
)
public void handleBan(CommandContext context) {
    String player = context.arg(0);

    if (player == null) {
        context.info("Utilize /admin ban <jogador>");
        return;
    }

    context.info("Jogador informado: " + player);
}
```

Nesse exemplo, `/admin` executa `handleAdmin`, enquanto `/admin ban Gabriel` executa `handleBan`, com `Gabriel` disponível no argumento de índice `0`.

## Argumentos

O `CommandContext` oferece métodos para consultar os argumentos e enviar mensagens.

```java
String name = context.arg(0);
int amount = context.argInt(1, 1);

context.info("Nome: " + name);
context.info("Quantidade: " + amount);
```

- `arg(index)`: retorna o argumento no índice informado ou `null` se ele não existir.
- `argInt(index, fallback)`: tenta converter o argumento para inteiro; se estiver ausente ou inválido, retorna o valor padrão.
- `player()`: retorna um `Optional<Player>`.
- `isPlayer()`: verifica se o executor é um jogador.
- `info(message)`: envia uma mensagem ao executor.

Os índices começam em `0`.

## Registro na `plugin.yml`

Não é necessário declarar individualmente os comandos na `plugin.yml`. A API registra os comandos dinamicamente por meio do `CommandMap`.

A `plugin.yml` do plugin continua sendo necessária para as informações básicas exigidas pelo servidor, como nome e versão.

## Instalação

Adicione o repositório do JitPack ao seu `build.gradle.kts`:

```kotlin
repositories {
    maven("https://jitpack.io")
}
```

Em seguida, adicione a dependência:

```kotlin
dependencies {
    implementation("com.github.GabriellMartins:Command-Framework:1.0.0")
}
```

Substitua `1.0.0` pela versão ou tag publicada no repositório.



## Sobre

O Command Framework foi criado para facilitar o desenvolvimento de plugins Minecraft, oferecendo uma forma direta de declarar comandos, permissões e subcomandos por meio de anotações.
