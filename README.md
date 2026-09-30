# archsync

Simple configuration manager for Arch Linux with focus on user space.

## Features

- persist your valuable configuration files with git
- share configuration between different computers
- group related configuration files, packages and systemd service in one module
- support list, add, remove commands for module
- install/uninstall pacman packages
- install/uninstall aur packages
- create directory tree
- link/unlink files
- backup/overwrite links
- enable/disable systemd services
- enable/disable user systemd services
- custom shell script execution
- based on hostname set of modules
- support `--only-*` flags to restrict the entire pipeline (read → diff → sync → print) to only the specified feature.

## Commands

| command | args | description |
| --- | --- | --- |
| show | --diff --merge | show all modules info for current host |
| dump | | just print all available hosts and modules |
| sync | --cleanup  --merge| install packages, create dirs, link files, when cleanup remove packages |

All subcommands support the following options, environment and defaults in strict order: option overrides env, env overrides default.

## Common options

| option | env | default | description | type |
| --- | --- | --- | --- | --- |
| host | ARCHSYNC_HOST | `/etc/hostname` | hostname for automatic host declaration detection | string |
| repo | ARCHSYNC_REPO | `$HOME/.config/archsync` | repository with modules | path to dir |
| home | ARCHSYNC_USER_HOME | `$HOME` | all files under `$module/home` will be compared with this folder | path to dir |
| config | ARCHSYNC_USER_CONFIG | `$HOME/.config` | all files under `$module/config` will be compared with this folder | path to dir |

## list

Lists modules declared for the current host.

Options:

- `--details`: List all modules with optional details (description + pacman packages).

Algorithm:

1. Resolve into repository config object. It contains Home dir Location, Config Location, HostName, and Repository Directory.
2. Resolve all those fields using either option, env, or default values.
3. Use repository use case.
4. Inside repository service, read all hosts and resolve them using a YAML parser.
5. Resolve all modules using a YAML parser.
6. Output everything into a print line. First output host with modules, then all available modules with internal declaration.

## Schema

At `$HOME/.config/archsync` locates a git repo with following structure:

- `./modules/module-a` - root of a module
- `./modules/module-a/module.yaml` - module descriptor

```yaml
name: module-a
description: hello, this is a test module
pacman: ["fish", "fzf", "ripgrep", "tmux"]
```

- `./modules/module-a/config` - root for module's configs

- `./hosts/virtualhost.yaml` - for each host set of modules

```yaml
name: virtualhost
description: description for virtual
modules: ["module-a"]
```

## Example

Let's take a folder with the following structure:

```
example/hosts/virtualarch.yaml
example/modules/base/config/myanotherapp/another.json
example/modules/base/config/myapp/myapp.cfg
example/modules/base/module.yaml
example/modules/extended/config/somecli/mycli.yaml
example/modules/extended/module.yaml
```

Here are one virtual arch host description and two modules base and extended, each module contains its own configuration files and required packages.

`example/hosts/virtualarch.yaml`:

```yaml
modules:
- base
- extended
```

`example/modules/base/config/myanotherapp/another.json`:

```json
{
  "hello": "Bob"
}
```

`example/modules/base/config/myapp/myapp.cfg`:

```
HELLO
```

`example/modules/base/module.yaml`:

```yaml
pacman:
- linux-firmware
- linux-lts
- linux-lts-headers
```

`example/modules/extended/config/somecli/mycli.yaml`:

```yaml
hello: Bill
```

`example/modules/extended/module.yaml`:

```yaml
pacman:
- ripgrep
- fd
- jq
- fzf
- fish
- fisher
- tmux
```

## Data model

- host is a named set of modules, for automatic modules discovery based on hostname
- module is a self-contained group of coherent feature like: packages, systemd and config files

## TODO

- add group is a named set of modules, for grouping related set of modules for convenient
