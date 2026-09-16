# common/navigation

The screen registry and the back-stack saver. How they work is in `docs/architecture/FEATURES.md`
(Navigation, Factories, Extension points). This file holds what is specific to this module.

## The registry is keyed by `KClass<out Screen>`

Following the framework rather than working around it: `Circuit.Builder` keys its own
`animatedScreenTransforms` the same way.

## A miss is logged, never thrown

`CollectedScreenFactories` logs `No feature registered a factory for …` and returns null. A null
makes Circuit render `onUnavailableContent`, which looks like a blank screen with no error anywhere,
so the log line is the only trace. Throwing would turn a forgotten registration into a crash on the
first navigation.

## Testing the saver on the host

`SerializableCircuitSaver.save` writes into `android.os.Bundle`, which is a stub in JVM host tests.
`ScreenSaverRoundTripTest` therefore proves registration through kotlinx JSON with the same
polymorphic module the saver uses, and checks an unregistered screen is absent from that module. The
Bundle layer is androidx's; it is verified on the emulator by killing the process with a chat open.
