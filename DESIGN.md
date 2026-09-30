# design draft v01

This is a design draft for my CLI tool for management and Linux distribution.

# subcommand

All subcommands support following options, environment and defaults in strict order: option overrides env, env overrides default.


| command | args | description | 
| --- | --- | --- |
| list | --details  | list all modules with optional details |
| report | --details | report all modules for given host merged |
| sync | --cleanup | install packages, create dirs, link files, when cleanup remove packages |

## options and defaults

| option | env | default | description | type |
| --- | --- | --- | --- | --- |
| host | ARCHSYNC_HOST | `/etc/hostname` | hostname for automatic host declaration detection | string |
| repo | ARCHSYNC_REPO | `$HOME/.config/archsync` | repository with modules | path to dir |
| home | ARCHSYNC_USER_HOME | `$HOME` | all files under `$module/home` will be compared with this folder | path to dir |
| config | ARCHSYNC_USER_CONFIG | `$HOME/.config` | all files under `$module/config` will be compared with this folder | path to dir |

# Feature flags

The tool supports `--only-*` flags (e.g., `--only-pacman`, `--only-config`) to restrict the scope of operations. 

When at least one `--only-*` flag is set to `true`, the tool enters "only mode". In this mode:
- Only the features that are explicitly enabled by an `--only-*` flag will be processed in the entire pipeline (reading, diffing, syncing, and printing).
- All other features are disabled.

If no `--only-*` flags are set, all features are enabled by default.

# example

Let's take a folder with the following structure:
```
example/hosts/virtualarch.yaml
example/modules/base/config/myanotherapp/another.json
example/modules/base/config/myapp/myapp.cfg
example/modules/base/module.yaml
example/modules/extended/config/somecli/mycli.yaml
example/modules/extended/module.yaml
```

Here are one virtual arch host description and two modules base and extended each module contains its own configuration files and required packages. 

