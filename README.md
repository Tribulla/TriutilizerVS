# Triutilizer VS

**Parallel physics solver integration for Valkyrien Skies**

Triutilizer VS adds multithreaded physics processing to Valkyrien Skies ships by hooking into the physics tick and distributing constraint solving across multiple CPU cores using a Parallel Red-Black SOR algorithm.

## Features

- **Parallel Physics Solving** - Distributes ship physics calculations across worker threads
- **Automatic Detection** - Only engages parallel solver when beneficial
- **Configurable** - Tune solver parameters for your use case
- **Debug Tools** - Commands and visualization for monitoring
- **Thread-Safe** - Proper synchronization between physics threads and main thread
- **Safe Fallback** - Reverts to vanilla VS physics if issues occur

## Requirements

- **Minecraft**: 1.19.2 or 1.20.1
- **Forge**: 43.x or 47.x
- **Triutilizer**: 2.1-a or higher (required dependency)
- **Valkyrien Skies**: 2.x

## Installation

1. Download and install [Triutilizer](https://github.com/Tribulla/Triutilizer) (core library)
2. Download and install Valkyrien Skies 2.x
3. Download and install Triutilizer VS (this mod)
4. Launch the game

Configuration file will be generated at `config/triutilizer/triutilizervs.toml` on first launch.

## How It Works

The mod uses Mixin to hook into Valkyrien Skies' `ShipObjectServerWorld.postTick()` method. On each physics tick:

1. Ship physics data is extracted (position, velocity, mass, inertia)
2. Physics constraints are identified and partitioned using graph coloring
3. Constraints are solved in parallel using Red-Black SOR algorithm
4. Results are applied back to the ship on the main thread

The solver uses Triutilizer's task manager to distribute work across worker threads while keeping all Minecraft world access on the main thread.

## Configuration

Edit `config/triutilizer/triutilizervs.toml`:

```toml
[common]
    # Enable or disable parallel physics solving
    enableParallelPhysics = true
    
    # Attempt to override VS native physics (cooperative mode if false)
    forceOverrideVSPhysics = false
    
    # Minimum number of constraints before using parallel solver
    minConstraintsForParallel = 10
    
    # Red-Black SOR solver parameters
    sorOmega = 1.8              # Relaxation parameter (1.0-2.0)
    sorMaxIterations = 10       # Maximum solver iterations
    sorTolerance = 0.0001       # Convergence tolerance
    
    # Debug logging
    enableDebugLogging = false
```

## Commands

All commands require operator permissions:

- `/triutilizervs solver` - Display solver status and integration info
- `/triutilizervs stats` - Show physics statistics and thread utilization
- `/triutilizervs overlay` - Toggle ship debug overlay (client-side)
- `/triutilizervs logging <enable|disable>` - Toggle debug logging

## Debug Overlay

When enabled, the debug overlay shows which ships are using the parallel solver:
- **Green boxes** - Ships using parallel physics
- **Red boxes** - Ships using regular VS physics

## Compatibility

**Known Compatible:**
- Eureka Ships
- VS Clockwork
- Create mod
- Other Valkyrien Skies addons

**Server/Client:**
- Server-side mod (processes physics on server)
- Optional on client (only needed for debug overlay feature)

## Technical Details

**Algorithm**: Parallel Red-Black SOR (Successive Over-Relaxation)  
**Integration**: Mixin-based hooks + reflective VS API access  
**Threading**: Computation on worker threads, world access on main thread  
**Framework**: Built on Triutilizer's priority-based task manager

See [INTEGRATION_STATUS.md](INTEGRATION_STATUS.md) for detailed technical documentation.

## Development Status

**Current Version**: 0.1-ea (Enhanced Alpha)

**What's Working:**
- Core parallel solver implementation
- Mixin integration with VS physics tick
- Ship data extraction and result application
- Configuration system
- Debug tools and visualization
- Thread-safe operations

**In Development:**
- Advanced inter-ship constraint extraction
- Improved collision detection algorithms
- Additional constraint types

## Building from Source

```bash
# Clone the repository
git clone https://github.com/Tribulla/Triutilizer.git
cd "Triutilizer VS"

# Build the mod
./gradlew build

# Output will be in build/libs/
```

## Troubleshooting

**"Integration FAILED" message:**
- Verify Valkyrien Skies is installed and version 2.x
- Enable debug logging: `/triutilizervs logging enable`
- Check logs for detailed error messages

**Solver not engaging:**
- Ships may be below `minConstraintsForParallel` threshold
- Check with `/triutilizervs overlay` to see solver status
- Try lowering `minConstraintsForParallel` in config

**Issues or crashes:**
- Set `enableParallelPhysics = false` to disable as temporary fix
- Report bugs on GitHub with logs and VS version

## Contributing

Contributions welcome! Areas that need work:

- VS API integration improvements
- Additional constraint type implementations
- Solver parameter optimization
- Documentation and testing

## Links

- **GitHub**: https://github.com/Tribulla/Triutilizer
- **Discord**: https://discord.gg/P6VTrethrk (TDI Server)
- **Patreon**: https://www.patreon.com/tribulla

## Credits

**Developer**: Tribulla  
**Multithreading Framework**: Triutilizer  
**Physics Algorithm**: Red-Black SOR with constraint graph coloring  

Core algorithms and infrastructure written by human developers, with AI assistance for code documentation and optimization.

## Servers Using This Mod

- TDI (The Divided Isles) - https://discord.gg/P6VTrethrk
- CCW (Create Constant Warfare)

## License

See [LICENSE](LICENSE) file for details.

---

**Note**: This is an early access release. The core functionality is stable and has been tested on production servers, but some advanced features are still in development. Report any issues on GitHub.
