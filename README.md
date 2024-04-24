# JOSM Sidewalks
## Installation
JOSM Preferences → `Plugins` → `sidewalks`

## Usage
### Automatic crossing ways
Enter the mode via `Mode` → `Sidewalk Mode`.

Once in the mode, you can start mapping sidewalks. It works in a similar manner
to the `Draw` mode with some special actions for sidewalks. Of specific note:
* When adding nodes to a way with `highway=footway`, if the `footway` crosses
  a road and is sufficiently short, a crossing way will be created. It is
  recommended to put a node where the sidewalk stops and starts on either side
  of the road.

### Synchronizing keys between crossing ways and nodes
When adding tags to a crossing way _or_ node, common crossing tags will be
copied from the edited way or node to the attached node or way. Current keys
that are synchronized:
* `bicycle`
* `crossing`
* `crossing:continuous`
* `crossing:markings`
* `crossing:signals`
* `crossing_ref`
* `cycleway`
* `flashing_lights`
* `horse`
* `traffic_signals:sound`
* `traffic_signals:vibration`
### Parallel way creation
`Data` → `Create parallel sidewalks`

This just creates parallel ways to a highway. It is recommended to check the
geometry of the highway before using this tool.

## Advanced preferences
| Preference                            | Default value | Description                                                                                           |
|:--------------------------------------|:-------------:|:------------------------------------------------------------------------------------------------------|
| `sidewalk.crossing.kerb.tags`         |    `[{}]`     | Any additional tags to add to the `kerb` node                                                         |
| `sidewalk.crossing.kerb`              |    `true`     | If `true`, add `kerb` tags to crossing nodes connected to sidewalks                                   |
| `sidewalk.crossing.maxlength`         |     `30`      | The maximum length for a crossing way                                                                 |
| `sidewalk.crossing.node.maxdistance`  |      `6`      | Attempt to merge nodes with crossing tags at most this distance (m) away when creating crossing nodes |
| `sidewalk.crossing.node.dupedistance` |      `1`      | Attempt to merge nodes at most this distance (m) away when creating crossing nodes                    |
| `sidewalk.crossing.sync`              |    `true`     | If `true`, synchronize tags between the crossing way and the crossing node                            |

## License
GPLv2 or any later version
