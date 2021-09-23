# JOSM MapWithAI StreetLevel AI Detections
## Installation
At time of writing, this plugin is not widely available.

## Usage
At this time, coverage is very limited.

Current known coverage is currently limited to an area in Seattle.

* [Seattle](https://www.openstreetmap.org/#map=17/47.61192/-122.34495)

[JOSM Remote Control](http://127.0.0.1:8111/load_and_zoom?left=-122.3492431640625&bottom=47.60986653003798&right=-122.34374999999999&top=47.61356975397398)
```shell
$ curl http://127.0.0.1:8111/load_and_zoom\?left=-122.3492431640625\&bottom\=47.60986653003798\&right\=-122.34374999999999\&top\=47.61356975397398
```

Please note that it currently appears that the service only returns data in a tile. The above example area uses tile
`16/10495/22886`. The plugin currently does not attempt to make requests on the tile level, so any data layer _must_
be downloaded tile-by-tile.

Useful website: https://oms.wff.ch/calc.htm (converts lat/lon to x/y). Use zoom `16`.
### Adding the layer
`Data` -> `MapWithAI StreetLevel: Add Layer` (note: this should eventually move into the `MapWithAI` menu)

Again, current known coverage is limited, and the service is currently being updated/modified. Bugs will occur.

### Adding data
There is a special `Apply suggestion` action. It (by default) has no shortcut, and will be merged with the `Add selected data` from the MapWithAI plugin in the future.

### Images
At this time, the service returns Mapillary v3 images. _This will break sometime in the future_.

Furthermore, 360 images, while viewable, are not seen as 360 images.

See [JOSM #16472](https://josm.openstreetmap.de/ticket/16472) for work towards that.
