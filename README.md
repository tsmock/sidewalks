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

## Useful overpass queries
### Count and length of sidewalks touched by a user after a set date in an area
```
// Count and length of all sidewalks touched in Mesa County by vorpalblade after April 1 2024
[out:json][timeout:25];
{{geocodeArea:"Mesa County, Colorado, United States"}}->.searchArea;
way["highway"="footway"](newer:"2024-04-01T00:00:00Z")(user_touched:vorpalblade)(area.searchArea);
make stat number=count(ways),length=sum(length());
out;
```
Explanation:
* `[out:json][timeout:25]`: Set the output to `json`, timeout after 25 seconds
* `{{geocodeArea:"Mesa County, Colorado, United States"}}->.searchArea;`: Set the search area to [Mesa County, Colorado](https://www.openstreetmap.org/relation/1411341)
* `way["highway"="footway"]`: Find footways
  * `(newer:"2024-04-01T00:00:00Z")`: Filter footways that were touched after April 01, 2024
  * `(user_touched:vorpalblade)`: Filter footways that were touched by the user `vorpalblade`
  * `(area.searchArea)`: Filter footways to only be inside the search area
* `make stat number=count(ways),length=sum(length());`: Output a count of the ways (`count(ways)`) and the sum of the length of those ways (`sum(length())`)
* `out`: Output the json

[Sample Query](https://overpass-turbo.eu/?q=Ly8gQ291bnQgYW5kIGxlbmd0aCBvZsSIbGwgc2lkZXdhbGtzIHTEhGNoZcSLaW4gTWVzYcSCxITEhnkgYsS0dm9ycMSfYmxhxJzEiGZ0ZXIgQXByacSYMSAyMDI0ClvEhHQ6anNvbl1bdGltZcWUOjI1XTsKe3tnxaBjb8ScQXJlYToixK3Er8SxxIV0eSzEsWzEucS_b8W7VW5pxYPEi1N0YcWDcyJ9fS0-LnPFsXLEpsWvxbHFpsSeeVsiaGlnaMaZIj0iZm9vdMahXShuxJ3FhMWzxY7FkC0wNMayMVQwMDrGuMa6MFoiKSh1xpJyX8SkdcSmxKg6xLjEusS8xL7EnMeAYcWwYcaRxpPGlceSKcWmbWFrZcSZxojEh251bWLFhD3FrMW4KMaZcyksxI3Ej8SRPXPHoijHrsSQaCgpx5gKxZQ7&c=AdVUgjuveK&R=)
(you'll have to hit `show data`)

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
