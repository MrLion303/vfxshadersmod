# VFX Shaders Mod

Motor independiente de efectos visuales en tiempo real para Minecraft Java Edition, orientado a Forge 1.20.1.

El proyecto está diseñado para combinar shaders GLSL, geometría, partículas, animaciones, efectos ambientales, objetos visuales colocados en el mundo y posprocesamiento de cámara. No se limita a modificar el cielo: admite efectos cuya posición es fija en el mapa, como un agujero negro, y filtros que deforman visualmente los bloques y el paisaje a su alrededor.

> Estado: las barras del horizonte y la aurora tienen ciclos geométricos continuos; se añadió un cielo arcoíris cambiante y un agujero negro con diámetro ajustable, disco de acreción y pase de lente gravitacional. La apariencia debe comprobarse en una sesión real de Minecraft; la compilación por sí sola no verifica el resultado visual ni la compatibilidad con todos los drivers.

## Comandos dentro del juego

- /vfx list: lista los efectos registrados, sus categorías y estados.
- /vfx enable <efecto>: activa uno.
- /vfx disable <efecto>: desactiva uno.
- /vfx toggle <efecto>: alterna su estado.
- /vfx status <efecto>: consulta su estado.
- /vfx disable all: desactiva todos los efectos locales.
- /vfx blackhole spawn: coloca un agujero negro de 10 bloques de diámetro.
- /vfx blackhole spawn <diámetro>: lo coloca con un diámetro de 1 a 100 bloques.
- /vfx blackhole clear: elimina los agujeros negros colocados.
- /vfx help: muestra la ayuda.

Los comandos son locales al cliente y funcionan al jugar en un servidor sin requerir permisos de operador. Las acciones visuales no cambian los bloques reales ni se sincronizan todavía con otros jugadores.

## VFX disponibles

- Órbitas de demostración (demo_orbit): tres anillos de color alrededor del jugador local.
- Barras musicales del horizonte (music_horizon_bars): barras de ecualizador animadas en un anillo alrededor del horizonte. Las frecuencias ahora se repiten exactamente al cerrar la vuelta; el movimiento es procedural y aún no analiza el audio real.
- Aurora boreal arcoíris (rainbow_aurora): cortinas luminosas ondulantes con el color recorriendo el arcoíris. Las ondas se cierran sin una junta desigual.
- Cielo arcoíris cambiante (rainbow_sky): una cúpula transparente que mezcla el cielo con colores que cambian gradualmente.
- Agujero negro (black_hole): horizonte oscuro de 10 bloques de diámetro por defecto, disco de acreción animado y una lente gravitacional que distorsiona la imagen del paisaje cercano.

El agujero negro se coloca en el punto al que se apunta; si no se apunta a nada, aparece a 16 bloques delante del jugador. El parámetro personalizado es el diámetro del horizonte, y el disco visual se extiende más allá de este.

## Arquitectura

El catálogo central guarda identificador, nombre visible, descripción y categoría. Las definiciones de efectos y las instancias colocadas son conceptos separados: puede haber varios agujeros negros en coordenadas distintas sin anclarlos automáticamente al jugador.

- Núcleo: ciclo de vida, categorías e identificadores.
- Instancias del mundo: dimensión, posición, rotación, escala, duración y parámetros por aparición.
- Renderizado espacial: contexto de cámara y registro de renderers por etapa.
- Shaders y materiales: VfxShaderLibrary y VfxRenderTypes.
- Posprocesamiento: la lente gravitacional de agujeros negros proyecta las instancias a pantalla y deforma la textura de la escena.
- Simulación y red: separación entre visuales locales y comportamiento autoritativo del mundo; la sincronización multijugador aún está pendiente.

La lente gravitacional es una distorsión de imagen. No cambia la geometría real, no destruye bloques y no ejerce fuerzas sobre entidades.

## Plataforma y compilación

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

## Qué falta

La implementación visual todavía debe verificarse dentro de Minecraft y ajustarse si aparecen artefactos. También quedan materiales reutilizables más amplios, partículas avanzadas, estelas, sincronización de eventos multijugador, simulación física autoritativa y editor visual.

Este proyecto es independiente y no modifica ni depende del repositorio shadermod. Todavía no se ha elegido una licencia; no debe asumirse que el código puede redistribuirse o reutilizarse libremente.
