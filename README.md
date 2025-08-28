# Cartography

A Minecraft server plugin for map image processing that allows you to convert online images into Minecraft maps through commands. This plugin is built for Paper 1.21+ servers.

## Features

- Fetch online images via URL and convert them to Minecraft maps
- Support for various image processing parameters to customize output
- Multiple packing modes for map distribution (normal, shulker box, compressed)
- Built-in image caching system to avoid reprocessing identical images
- Configurable map size limits and cooldown times for performance control
- SQLite-based storage for map metadata persistence

## Supported Minecraft Versions

This plugin is designed for Minecraft 1.21+ versions running on Paper server implementations.
Based on the plugin configuration, the minimum supported version is 1.21, but it should work on subsequent 1.21.x versions as well.

The plugin uses Paper API features and may not be compatible with vanilla Minecraft servers or other server implementations like Spigot.

## Commands

### /getimage
Fetch an image from a URL and convert it to a Minecraft map or map collection.

#### Syntax
```
/getimage -url <image URL> [-l <length>] [-w <width>] [-keepscale] [-dither] [-nocache] [-pack <pack mode>] 
```

#### Parameters

| Parameter | Description | Type | Default |
|-----------|-------------|------|---------|
| `-url` | The URL of the image to be converted (required) | String | None |
| `-l` | Length of the map (number of map items horizontally) | Integer | 1 |
| `-w` | Width of the map (number of map items vertically) | Integer | 1 |
| `-keepscale` | Maintain the original aspect ratio of the image | Flag | Disabled |
| `-dither` | Enable dithering to improve image quality by reducing banding | Flag | Disabled |
| `-nocache` | Skip cache and force reprocessing of the image | Flag | Disabled |
| `-pack` | Packing mode for distributing the maps | Enum: normal, shulker, compress | normal |

#### Examples

Convert a simple image to a single map:
```
/getimage -url https://example.com/image.png
```

Create a 3x3 map wall with dithering enabled:
```
/getimage -url https://example.com/landscape.jpg -dither -l 3 -w 3
```

Create a map with original aspect ratio preserved in a shulker box:
```
/getimage -url https://example.com/portrait.jpg -keepscale -pack shulker
```

Force reprocessing of an image without using cache (this will force create new image map no matter if the server did that before):
```
/getimage -url https://example.com/image.png -nocache
```

### /getmap
Retrieve a map item with a specific ID that already exists on the server.

#### Syntax
```
/getmap <map ID>
```

#### Parameters

| Parameter | Description | Type | Required |
|-----------|-------------|------|----------|
| `map ID` | The numerical ID of the map to retrieve | Integer | Yes |

#### Example
```
/getmap 12345
```

## Packing Modes

The plugin supports three different packing modes for distributing your generated maps:

### Normal
Maps are given directly to the player's inventory.

### Shulker
Maps are packed into shulker boxes, with each box holding up to 27 maps. Multiple shulker boxes are used if needed. This is especially useful for large map collections.

### Compress
Maps are compressed into a paint item. Right-click the block with that item will automatically put the maps on the click location.

## Permissions

| Permission Node         | Description                           | Default |
|-------------------------|---------------------------------------|---------|
| `cartography.getimage`  | Allows usage of the /getimage command | true    |
| `cartography.getmap`    | Allows usage of the /getmap command   | op      |
| `cartography.bypassmax` | Bypasses the maximum map size limit   | op      |
| `cartography.bypasscd`  | Bypasses the map generation cooldown  | op      |

## Configuration

The configuration file is located at `plugins/Cartography/config.yml`:

```yaml
cool-down: 30000
max-size: 9
```

### Configuration Options

| Option | Description | Default Value | Unit |
|--------|-------------|---------------|------|
| `cool-down` | Minimum time between map generation requests per player | 30000 | milliseconds |
| `max-size` | Maximum total number of maps allowed in a single request (length × width) | 9 | maps |

## Installation

1. Download the latest release JAR file or build from source
2. Place the plugin JAR file in your server's `plugins` folder
3. Restart the server to generate the configuration file
4. Modify `plugins/Cartography/config.yml` as needed
5. Restart the server again or use a plugin reload command

## Building from Source

### Requirements
- Java 21 or higher
- Gradle 8.14+
- Paperweight plugin 2.0.0-beta.18 or higher

### Build Process

1. Clone or download the repository:
   ```bash
   git clone https://github.com/lumine1909/cartography.git
   ```

2. Navigate to the project directory:
   ```bash
   cd cartography
   ```

3. Build the plugin:
   ```bash
   ./gradlew build
   ```

4. The built JAR file will be located in `build/libs/`

### Common Issues 
**Image not loading**
    - Ensure the image URL is directly accessible
    - Check that the server has internet access
    - Verify the URL points to an actual image file

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a pull request

## License

This project is licensed under the [GPL 3.0 License](LICENSE).
