# VFX Shaders Mod

Motor de efectos en tiempo real para Minecraft Java Edition, orientado a Forge 1.20.1. Incluye geometría personalizada, VFX ambientales, objetos visuales colocados en coordenadas del mundo y pases GLSL de posprocesamiento.

## Comandos

- `/vfx list`: muestra los efectos disponibles.
- `/vfx enable <efecto>` y `/vfx disable <efecto>`: activan o desactivan un efecto.
- `/vfx toggle <efecto>`: alterna su estado.
- `/vfx disable all`: limpia todos los VFX.
- `/vfx enable blackhole <tamaño> <x> <y> <z>`: coloca un agujero negro de entre 1 y 100 bloques de diámetro.
- `/vfx disable blackhole`: elimina los agujeros negros colocados.
- `/vfx enable blackhole_sky`: muestra un agujero negro celeste gigante fijado a 50° sobre el horizonte.
- `/vfx disable blackhole_sky`: lo desactiva.
- `/vfxmove empujon <jugadores>`: impulsa a los jugadores seleccionados hacia atrás y hacia arriba.
- `/vfxmove moonjump enable|disable <jugadores>`: activa o desactiva gravedad lunar por jugador.
- `/vfxmove waterwalk enable|disable <jugadores>` (también `caminar_agua`): permite caminar por la superficie del agua.
- `/vfx enable cloud_vortex`: activa un vórtice de nubes rojo-anaranjadas en el cielo.
- `/vfx enable rainbow_aurora_photon`: activa la variante independiente de la aurora hecha con partículas Photon.
- `/vfx enable starsspeed <color>`: activa estelas estelares rápidas sobre un cielo tintado; el color es opcional (por defecto, azul).
- `/vfx enable ground_aurora <x> <y> <z> <color>`: extiende ondas luminosas sobre el suelo desde coordenadas dadas.
- `/vfx enable falling_stars`: activa una lluvia continua de estrellas diagonales arcoíris.
- `/vfx enable sky_portal`: abre un portal circular cinematográfico en el cielo.
- `/vfxonce scan <x> <y> <z> <color>`: lanza una única onda de escaneo que se adapta al relieve.
- `/vfxonce trampolin <x> <y> <z> [potencia] [radio] [velocidad]`: onda circular sobre el suelo; deforma visualmente la superficie y lanza a los jugadores cuando los alcanza.

Los comandos VFX están disponibles en el cliente y en el servidor. Desde bloques de comandos, el servidor envía las órdenes visuales a los clientes conectados en la misma dimensión. Instala el mod tanto en el servidor como en los clientes. Los comandos de servidor requieren permiso de operador de nivel 2. Los comandos de movimiento modifican únicamente a los jugadores seleccionados y se limpian al desconectarse.

Ejemplos de bloque de comandos:

```text
/vfx enable blackhole 80 120 170 -300
/vfx enable cloud_vortex
/vfx enable starsspeed celeste
/vfx enable ground_aurora 120 70 -300 naranja
/vfx enable falling_stars
/vfx enable sky_portal
/vfxonce scan 120 70 -300 azul
/vfxmove moonjump enable @a
/vfxmove waterwalk enable @a
/vfx disable blackhole
/vfx enable blackhole_sky
/vfx disable blackhole_sky
/vfx enable rainbow_sky
/vfx disable rainbow_sky
/vfxmove empujon @a
```

El agujero negro celeste tiene posición fija respecto al mundo: apunta hacia el azimut -Z y está a 50° de elevación, donde 0° es el horizonte y 90° el cenit. No depende de hacia dónde mire el jugador al activarlo. Las coordenadas del agujero negro del mundo aceptan valores absolutos o relativos con `~`.

## Efectos incluidos

- **demo_orbit:** anillos orbitales nativos de Photon, con movimiento de partículas, bloom y estelas.
- **music_horizon_bars:** barras geométricas originales alrededor del horizonte, con alturas y colores animados. Se conserva el renderizador previo a la migración a Photon; no analiza audio real.
- **music_sync_bars (Barras Sync):** copia visual independiente que reacciona a los sonidos que empiezan a reproducirse en el cliente local. Se activa con `/vfx enable music_sync_bars` y se desactiva con `/vfx disable music_sync_bars`; no sincroniza los impulsos entre jugadores.
- **rainbow_aurora:** aurora arcoíris clásica con cortinas geométricas ondulantes y gradientes de color, restaurada a su renderizador original.
- **rainbow_sky:** pinta uniformemente el cielo y va cambiando de azul a rojo y a los demás colores del arcoíris. Cubre el cenit y tiñe las nubes sin recolorear los bloques cercanos.
- **blackhole:** agujero negro en coordenadas del mundo con horizonte negro compacto, disco de acreción blanco-dorado/naranja y lente gravitacional de pantalla. Si la cámara queda dentro del horizonte, se oculta temporalmente el objeto para evitar envolver la vista en negro.
- **blackhole_sky:** versión gigantesca colocada en el cielo con el disco de partículas de Photon alineado con la acreción GLSL; los agujeros negros colocados usan un preset Photon separado con el disco casi horizontal.
- **cloud_vortex:** vórtice de nubes rojo-anaranjadas con raymarching volumétrico 3D. La imagen guía aporta el mapa de densidad y color en cada capa de profundidad, mientras Photon añade partículas y estelas.
- **rainbow_aurora_photon:** variante independiente de la aurora arcoíris, con bandas orbitales, bloom y estelas largas de Photon.
- **starsspeed:** campo original de estrellas veloces y estelas curvas sobre una cúpula celeste tintada. Permite seleccionar el color y conserva el renderer previo a Photon.
- **ground_aurora:** ondas Photon de radio creciente acompañadas por bandas propias que se adaptan al relieve; permite personalizar el color.
- **falling_stars:** lluvia de partículas arcoíris de Photon con estelas y colisiones que retiran las estrellas al tocar el terreno.
- **sky_portal:** anillos Photon que crecen durante la apertura y fragmentos orbitales con estelas alrededor de una ventana galáctica con shader propio. Usa `panorama_overlay.png` si está presente; si falta, el shader crea un interior procedural.
- **scan:** onda Photon con radio animado por curva, complementada por un aro propio que sigue la superficie de los bloques cargados.
- **trampolin:** reconstruye la superficie visible desde el búfer de profundidad y desplaza las texturas y los bordes de los bloques según una cresta radial, sin modificar permanentemente el mapa.

La lente de pantalla del agujero negro colocado usa una zona de influencia limitada alrededor del horizonte para conservar una distorsión gravitacional sutil sin crear un contorno exterior enorme. El horizonte 3D y el disco de acreción siguen activos. El efecto visual no simula relatividad general ni altera bloques o colisiones; el aspecto final debe comprobarse dentro de Minecraft.

## Probar la migración

Consulta [la guía de instalación y pruebas de Photon](docs/PROBAR_PHOTON.md) para descargar el JAR de GitHub Actions, preparar la instancia y probar cada efecto con comandos concretos.

## Motor Photon real

El mod usa **Photon para Forge 1.20.1, versión 1.1.17**, como motor real de partículas. Los emisores, formas, curvas de escala, rotación orbital, bloom y estelas se construyen con la API de Photon. Las órbitas, la lluvia de estrellas y `rainbow_aurora_photon` usan Photon como renderizador principal. Las barras musicales, la aurora clásica y `starsspeed` conservan sus renderizadores originales; el vórtice usa volumen 3D propio junto con partículas Photon, y otros efectos combinan Photon con shaders o geometría especializada.

Photon debe estar instalado junto con este mod. [Proyecto oficial de Photon](https://github.com/Low-Drag-MC/Photon) · [Versión Forge 1.20.1](https://modrinth.com/mod/photon-editor/version/mc1.20.1-1.1.17-forge).

El código fuente de Photon para esta versión se distribuye bajo GPL-3.0. Este repositorio depende del artefacto oficial y no copia ni modifica su código fuente.

## Plataforma

- Minecraft Java Edition 1.20.1
- Minecraft Forge 47.4.18
- Java 17
- Gradle 8.1.1

GitHub Actions compila en cada push a `main` y guarda el JAR como artefacto.

## Documentación

- [Arquitectura](docs/ARQUITECTURA.md)
- [Hoja de ruta](docs/HOJA_DE_RUTA.md)
- [Cómo crear efectos](docs/CREAR_EFECTOS.md)
- [Comandos](docs/COMANDOS.md)

Este proyecto es independiente y no modifica ni depende del repositorio `shadermod`. Aún no se ha elegido una licencia.
