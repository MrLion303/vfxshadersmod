# VFX Shaders Mod

Motor independiente de efectos visuales en tiempo real para Minecraft Java Edition, orientado a Forge 1.20.1.

El proyecto combina shaders GLSL, geometría personalizada, partículas, animaciones, efectos ambientales, objetos colocados en coordenadas del mundo y posprocesamiento de cámara. No se limita a modificar el cielo: un agujero negro puede estar en cualquier punto del mapa, y la imagen de los bloques y del cielo se deforma a su alrededor.

## Comandos

- /vfx list: muestra efectos, categorías y estados.
- /vfx enable <efecto>: activa un efecto.
- /vfx disable <efecto>: desactiva un efecto.
- /vfx toggle <efecto>: alterna su estado.
- /vfx status <efecto>: consulta el estado.
- /vfx disable all: limpia los VFX locales.
- /vfx enable blackhole <tamaño> <x> <y> <z>: coloca un agujero negro de 1 a 100 bloques de diámetro.
- /vfx disable blackhole: elimina todos los agujeros negros colocados.
- /vfx enable blackhole_sky: activa el agujero negro celeste.
- /vfx disable blackhole_sky: desactiva el agujero negro celeste.
- /vfxmove empujon <jugadores>: lanza a los jugadores seleccionados hacia atrás y hacia arriba. Acepta nombres y selectores como @p, @a o @a[distance=..10].

El comando /vfxmove empujon modifica la velocidad desde el servidor, así que afecta al movimiento real de los jugadores y requiere permisos de operador (nivel 2). El impulso se calcula en dirección opuesta a la mirada horizontal de cada jugador, con una elevación moderada.

El tamaño de referencia del agujero negro situado en el mundo es de 10 bloques de diámetro. Las coordenadas admiten números absolutos o relativos con ~; por ejemplo: /vfx enable blackhole 10 100 64 -200.

## VFX incluidos

- **Órbitas de demostración (demo_orbit):** anillos de color alrededor del jugador.
- **Barras musicales del horizonte (music_horizon_bars):** barras animadas con una vuelta continua, sin una junta de ondas disparejas. El movimiento es procedural, no un analizador de audio.
- **Aurora boreal arcoíris (rainbow_aurora):** cortinas luminosas con ondas que cierran de manera continua.
- **Cielo arcoíris cambiante (rainbow_sky):** una cúpula translúcida de color que cambia lentamente a lo largo del espectro.
- **Agujero negro (blackhole):** horizonte negro anclado a coordenadas exactas, disco de acreción y lente gravitacional sobre la imagen del mundo. Puedes caminar hacia él y atravesar su centro; al estar dentro del horizonte, se oculta la esfera opaca y aumenta la deformación.
- **Agujero negro celeste (blackhole_sky):** un agujero negro situado visualmente en el cielo, con disco de acreción y lente gravitacional.

La lente gravitacional deforma la imagen renderizada, incluidos los bloques y el cielo que queden en su zona de influencia. No modifica la geometría real, no destruye bloques ni aplica fuerzas; es un efecto visual local a tu cliente.

## Arquitectura

Las definiciones de efectos son independientes de las apariciones colocadas. Una definición puede tener varias instancias en distintas coordenadas y dimensiones. El renderizado espacial utiliza un registro de renderers; los pases de pantalla operan sobre la textura de la escena.

El catálogo central alimenta /vfx list y las sugerencias de comandos. El comando /vfxmove empujon se registra en el servidor y aplica la velocidad a los jugadores seleccionados.

## Plataforma

- Minecraft Java Edition 1.20.1
- Minecraft Forge 47.4.18
- Java 17
- Gradle 8.1.1

GitHub Actions compila en cada push a main y guarda el JAR como artefacto cuando termina correctamente.

## Documentación

- [Arquitectura del motor](docs/ARQUITECTURA.md)
- [Hoja de ruta](docs/HOJA_DE_RUTA.md)
- [Cómo crear un efecto nuevo](docs/CREAR_EFECTOS.md)
- [Comandos VFX](docs/COMANDOS.md)

## Estado y limitaciones

La compilación valida Java y los JSON, pero la apariencia final debe comprobarse dentro del cliente de Minecraft y puede necesitar ajustes por diferencias de drivers o configuración gráfica. La sincronización visual multijugador de los VFX sigue pendiente; el comando de empujón sí aplica movimiento real desde el servidor.

El proyecto es independiente y no modifica ni depende del repositorio shadermod. Todavía no se ha elegido una licencia, así que no debe asumirse que el código puede redistribuirse o reutilizarse libremente.
