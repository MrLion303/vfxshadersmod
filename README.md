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

Los comandos VFX están disponibles en el cliente y en el servidor. Desde bloques de comandos, el servidor envía las órdenes visuales a los clientes conectados en la misma dimensión. Instala el mod tanto en el servidor como en los clientes. Los comandos de servidor requieren permiso de operador de nivel 2.

Ejemplos de bloque de comandos:

```text
/vfx enable blackhole 80 120 170 -300
/vfx disable blackhole
/vfx enable blackhole_sky
/vfx disable blackhole_sky
/vfx enable rainbow_sky
/vfx disable rainbow_sky
/vfxmove empujon @a
```

El agujero negro celeste tiene posición fija respecto al mundo: apunta hacia el azimut -Z y está a 50° de elevación, donde 0° es el horizonte y 90° el cenit. No depende de hacia dónde mire el jugador al activarlo. Las coordenadas del agujero negro del mundo aceptan valores absolutos o relativos con `~`.

## Efectos incluidos

- **demo_orbit:** tres anillos de color alrededor del jugador local.
- **music_horizon_bars:** barras musicales de neón alrededor del horizonte. La animación es procedural y aún no analiza audio real.
- **rainbow_aurora:** cortinas luminosas ondulantes con ciclos continuos.
- **rainbow_sky:** pinta uniformemente el cielo y va cambiando de azul a rojo y a los demás colores del arcoíris. Cubre el cenit y tiñe las nubes sin recolorear los bloques cercanos.
- **blackhole:** agujero negro en coordenadas del mundo, con sombra oscura, disco de acreción blanco-dorado casi horizontal, halos curvados y lente gravitacional de pantalla.
- **blackhole_sky:** versión gigantesca colocada en el cielo a 50° sobre el horizonte, con disco luminoso cálido y distorsión amplia de la escena, inspirada visualmente en Gargantúa de *Interstellar*.

La lente es una aproximación cinematográfica en pantalla: deforma el fondo, dibuja la sombra y el disco luminoso. No simula relatividad general ni altera bloques o colisiones. La apariencia final debe comprobarse dentro de Minecraft; una compilación correcta no garantiza la salida visual en todos los drivers.

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
