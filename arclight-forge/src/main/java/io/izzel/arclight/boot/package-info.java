package io.izzel.arclight.boot;
/*
 * FML boot process (sketch)
 * Entrance: cpw.mods.bootstraplauncher.BootstrapLauncher
 *   Try to read system property legacyClassPath and determine classpath.
 *   If not present, use java.class.path instead
 *   Resolve and merge modules.
 *   Create ModuleClassLoader(MC-BOOTSTRAP)
 *   Create new layer(MC-BOOTSTRAP) from these modules
 *   Find and run launch Consumer in MC-BOOTSTRAP
 * Launch Consumer: BootstrapLaunchConsumer
 * Launcher: cpw.mods.modlauncher.Launcher
 *   Init ModuleLayerHandler
 *     Resolve MC-BOOTSTRAP as BOOT layer
 *   Init NameMappingServiceHandler
 *     Find INameMappingService in BOOT layer
 *       [MCPNamingService]
 *     Make metadata for services and save
 *   Init LaunchPluginHandler
 *     Find ILaunchPluginService in BOOT layer
 *     Save ILaunchPluginService (name -> service)
 *     Make modlist metadata for plugins and append to Environment (key is MODLIST)
 *   (String[] args)
 *   Store ARGS, parse gameDir and launchTarget
 *   Discover transformation services
 *     Find and run ITransformerDiscoveryService in BOOT layer
 *       [ClasspathTransformerDiscoverer] Find jars that provides ITransformationService and IModLocator from system ClassLoader classpath
 *       [ModDirTransformerDiscoverer] Find jars that provides ITransformationService, IModLocator and IDependencyLocator from mod dir
 *         Load FMLPaths and FMLConfig
 *     Build SERVICE layer from these jars, parent is BOOT
 *     Run early init for ITransformerDiscoveryService
 *       [ModDirTransformerDiscoverer] Construct early load process window
 *     Find and save ITransformationService in SERVICE layer
 *     Make modlist metadata for transformation services and append to Environment (key is MODLIST)
 *   Initialize transformation services
 *     Load transformation services
 *       Invoke onLoad for every transformation service, mark invalid if failed with exception
 *         [FMLServiceProvider] onLoad()
 *           FMLLoader: onInitialLoad()
 *             Verify requirement for
 *               - ModLauncher (compatible with 4.0)
 *               - AccessTransformer (compatible with 1.0)
 *               - EventBus (compatible with 1.0)
 *               - RuntimeDistCleaner
 *               - CoreMod
 *               - ForgeSPI (spec ver >= 2)
 *             Load NightConfig
 *         [MixinTransformationService] do nothing
 *       Check to ensure no service is invalid
 *       Collect argument parsers, parse args and offer results to service
 *       Invoke initialize for every transformation service
 *         [FMLServiceProvider] initialize() (set naming to mcp)
 *         [MixinTransformationService] initialize()
 *       Check for mapping naming (default mojang, now mcp) and find corresponding naming service
 *       Run transformation services (begin scanning)
 *         FMLServiceProvider: FMLLoader.beginModScan()
 *           Find, load and configure IModLocator
 *           For every locator, search for mod files and corrupted mod files (to be inserted into PLUGIN layer)
 *             [ArclightLocator_Forge] ModBootstrap.run(); adds arclight common
 *             [ClasspathLocator] When launch target is dev, adds from legacyClasspath (identical to java.class.path if not present)
 *             [MavenDirectoryLocator]
 *             [MinecraftLocator] adds server.jar, language providers, fmlcore
 *             [ExplodedDirectoryLocator]
 *             [ModsFolderLocator] adds from /mods
 *             *IModFile.Type: first from mods.toml, then from manifest.mf, finally as broken mods
 *           Find, load and configure IDependencyLocator
 *           For every locator, search for mod files
 *             [JarInJarDependencyLocator] resolve as GAMELIBRARY if inside MOD, else LIBRARY
 *           Find and return LANGPROVIDERs and LIBRARYs
 *         MixinTransformationService: MixinBootstrap.start()
 *       Collect service resources and add into PLUGIN layer
 *       Build PLUGIN layer
 *       Trigger scan completion for transformation services
 *         [FMLServiceProvider] FMLLoader.completeScan()
 *           Add coremod, AT for every mod
 *           Asynchronously scan for classes and annotations in mod files
 *           Find and return valid MODs and GAMELIBRARYs
 *         [MixinTransformationService] do nothing
 *       Collect service resources and add into GAME layer
 *       Gather and verify transformers from transformation services
 *       Store transformers to TransformerStore
 *       Offer service resources to launch plugins
 *       Verify launch target
 *       Collect target jars to transform (currently unused)
 *       Build TransformingClassLoader from TransformerStore, building GAME layer (layer is GAME)
 *       Use cl as fallback loader of PLUGIN
 *       Set context cl and launch mc using cl (layer is GAME)
 */