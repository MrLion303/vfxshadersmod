# VFX Shaders Mod

Motor independiente de efectos visuales en tiempo real para Minecraft Java Edition, orientado a **Forge 1.20.1**.

El objetivo no es añadir solamente una lista de efectos prefabricados. Queremos una base extensible para combinar shaders GLSL, geometría personalizada, partículas, animaciones, posprocesamiento y comportamiento del mundo. La meta es crear escenas cinematográficas —meteoritos con humo, portales, anomalías gravitacionales, supernovas y cielos dinámicos— sin rehacer el sistema para cada efecto nuevo.

> **Estado:** infraestructura base y contratos ampliados para efectos ambientales, efectos colocados en el mundo y futuros pases de pantalla. Los efectos cinematográficos complejos, el posprocesamiento activo y el editor visual todavía están pendientes.

## Comandos dentro del juego

- **/vfx list**: lista todos los efectos registrados, sus categorías y estados.
- **/vfx enable <efecto>**: activa uno.
- **/vfx disable <efecto>**: desactiva uno.
- **/vfx toggle <efecto>**: alterna su estado.
- **/vfx status <efecto>**: consulta su estado.
- **/vfx disable all**: desactiva todos.
- **/vfx help**: muestra la ayuda.

Los argumentos tienen autocompletado con Tab. Puedes escribir demo_orbit o el identificador completo vfxshadersmod:demo_orbit.

Los comandos son del cliente y solo afectan los efectos visuales de tu instancia; funcionan también al jugar en un servidor sin requerir permisos de operador. Las mecánicas que alteren jugadores, criaturas o bloques deberán ejecutarse y validarse en el servidor.

### VFX incluido como prueba

- **Órbitas de demostración** (demo_orbit): tres anillos de color alrededor del jugador local, usados como prueba del motor.
- **Barras musicales del horizonte** (music_horizon_bars): barras de ecualizador animadas en un anillo alrededor del horizonte.
- **Aurora boreal arcoíris** (rainbow_aurora): cortinas de luz ondulantes con colores que recorren el arcoíris.

Las barras usan una animación procedural; todavía no reciben el audio real del juego o del sistema.

Consulta la [guía de comandos](docs/COMANDOS.md).

## Arquitectura

El catálogo central guarda identificador, nombre visible, descripción y categoría. Las definiciones de los efectos y sus instancias son conceptos separados: una definición puede tener muchas apariciones situadas en coordenadas diferentes.

- **Núcleo:** ciclo de vida, categorías e identificadores.
- **Instancias del mundo:** dimensión, posición, rotación, escala, duración y parámetros por aparición.
- **Renderizado espacial:** contexto compartido de cámara y registro de renderers por etapa.
- **Shaders y materiales:** VfxShaderLibrary y VfxRenderTypes mantienen la base GLSL existente.
- **Posprocesamiento:** contratos y orden de pases preparados, pero el enlace al framebuffer todavía está pendiente.
- **Simulación y red:** separación entre visuales locales y comportamiento autoritativo del mundo; la sincronización multijugador aún está pendiente.

Una deformación visual de la imagen y un cambio real de bloques son mecanismos diferentes. La primera pertenece al renderizado o al posprocesamiento; la segunda requiere una implementación de simulación y, en multijugador, autoridad del servidor.

## Plataforma y compilación

- Minecraft Java Edition 1.20.1
- Minecraft Forge 47.4.18
- Java 17
- Gradle 8.1.1

Se necesita un JDK 17 y Gradle 8.1.1 instalado.

El JAR se genera en build/libs/. GitHub Actions compila en cada push a main y guarda el JAR como artefacto al terminar correctamente.

## Documentación

- [Arquitectura del motor](docs/ARQUITECTURA.md)
- [Hoja de ruta](docs/HOJA_DE_RUTA.md)
- [Cómo crear un efecto nuevo](docs/CREAR_EFECTOS.md)
- [Comandos VFX](docs/COMANDOS.md)

## Qué falta

Todavía se deben probar visualmente los efectos dentro de Minecraft. También faltan el pipeline activo de posprocesamiento, materiales reutilizables, partículas avanzadas, estelas, sincronización de eventos multijugador y el editor visual. El modelo de instancias y los contratos de renderizado son infraestructura; no significan que un agujero negro o una deformación de bloques ya estén implementados.

## Independencia y licencia

Este proyecto es independiente y no modifica ni depende del repositorio shadermod.

Todavía no se ha elegido una licencia. Hasta que se añada una, no se debe asumir que el código puede redistribuirse o reutilizarse libremente.
